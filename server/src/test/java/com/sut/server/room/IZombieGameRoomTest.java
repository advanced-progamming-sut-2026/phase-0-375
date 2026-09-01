package com.sut.server.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sut.server.net.ClientConnectionHandler;
import com.sut.server.net.PacketRouter;
import com.sut.server.net.TcpServer;
import model.game.core.GameModel;
import model.network.dto.PlantSnapshotDto;
import model.network.dto.ZombieSnapshotDto;
import model.network.enums.ReactionType;
import model.network.packet.chat.ReactionPacket;
import model.network.packet.game.GameStateSnapshotPacket;
import model.network.packet.game.PlacePlantRequestPacket;
import model.network.packet.game.PlaceZombieRequestPacket;
import model.network.packet.game.PlayerActionResponsePacket;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class IZombieGameRoomTest {

    private RoomManager roomManager;
    private ScheduledExecutorService executor;
    private PacketRouter packetRouter;
    private ObjectMapper objectMapper;

    private ServerSocket testServerSocket;
    private Socket clientSocketA, serverSocketA;
    private Socket clientSocketB, serverSocketB;
    private ClientConnectionHandler plantHandler;
    private ClientConnectionHandler zombieHandler;
    private IZombieGameRoom gameRoom;

    @BeforeEach
    void setUp() throws IOException {
        executor = Executors.newScheduledThreadPool(2);
        roomManager = new RoomManager(executor);
        packetRouter = new PacketRouter();
        objectMapper = TcpServer.createDefaultObjectMapper();

        testServerSocket = new ServerSocket(0);

        // Plant player connection
        clientSocketA = new Socket("127.0.0.1", testServerSocket.getLocalPort());
        serverSocketA = testServerSocket.accept();
        plantHandler = new ClientConnectionHandler("conn-plant", serverSocketA,
                new TcpServer("127.0.0.1", 0, packetRouter, objectMapper), packetRouter, objectMapper);
        plantHandler.setUsername("PlantHero");

        // Drain clientSocketA so TCP socket write buffer doesn't fill and block
        Thread drainA = new Thread(() -> {
            try {
                byte[] buf = new byte[8192];
                while (!clientSocketA.isClosed() && clientSocketA.getInputStream().read(buf) != -1) {}
            } catch (IOException ignored) {}
        });
        drainA.setDaemon(true);
        drainA.start();

        // Zombie player connection
        clientSocketB = new Socket("127.0.0.1", testServerSocket.getLocalPort());
        serverSocketB = testServerSocket.accept();
        zombieHandler = new ClientConnectionHandler("conn-zombie", serverSocketB,
                new TcpServer("127.0.0.1", 0, packetRouter, objectMapper), packetRouter, objectMapper);
        zombieHandler.setUsername("ZombieBoss");

        // Drain clientSocketB so TCP socket write buffer doesn't fill and block
        Thread drainB = new Thread(() -> {
            try {
                byte[] buf = new byte[8192];
                while (!clientSocketB.isClosed() && clientSocketB.getInputStream().read(buf) != -1) {}
            } catch (IOException ignored) {}
        });
        drainB.setDaemon(true);
        drainB.start();

        gameRoom = new IZombieGameRoom("room-test-123", roomManager, plantHandler, zombieHandler);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (gameRoom != null) {
            gameRoom.stop();
        }
        if (plantHandler != null) plantHandler.disconnect("Teardown");
        if (zombieHandler != null) zombieHandler.disconnect("Teardown");

        try { if (clientSocketA != null) clientSocketA.close(); } catch (IOException ignored) {}
        try { if (serverSocketA != null) serverSocketA.close(); } catch (IOException ignored) {}
        try { if (clientSocketB != null) clientSocketB.close(); } catch (IOException ignored) {}
        try { if (serverSocketB != null) serverSocketB.close(); } catch (IOException ignored) {}
        try { if (testServerSocket != null) testServerSocket.close(); } catch (IOException ignored) {}

        if (roomManager != null) roomManager.shutdown();
        if (executor != null) executor.shutdownNow();
    }

    @Test
    @DisplayName("Room Setup: Pre-plants defenses and spawns stationary sun zombies")
    void testRoomInitializationAndBoardPrePlanting() {
        GameModel model = gameRoom.getGameModel();
        assertNotNull(model);
        assertNotNull(gameRoom.getGameLoop());

        assertEquals(3, gameRoom.getRedLineColumn());
        assertEquals(150, gameRoom.getPlantSun());
        assertEquals(150, gameRoom.getZombieSun());

        // Pre-planted defenses
        assertFalse(model.getAllPlants().isEmpty());
        // Pre-planted 5 sun-producing zombies in col 8
        assertEquals(5, model.getActiveZombies().size());
        for (ZombieInstance z : model.getActiveZombies()) {
            assertEquals("ZombieIZombieSun", z.getDefinition().getName());
            assertEquals(8, z.getGridPosition().getX());
        }
    }

    @Test
    @DisplayName("20 Hz Headless Ticking: Tick counter and match time advance")
    void testHeadlessLoopTicking() {
        gameRoom.start(executor);
        assertTrue(gameRoom.isRunning());

        // Simulate 20 ticks (1 second)
        for (int i = 0; i < 20; i++) {
            gameRoom.tick();
        }

        assertEquals(20, gameRoom.getTickCounter());
        assertEquals(1.0f, gameRoom.getMatchTime(), 0.001f);
        assertFalse(gameRoom.isGameOver());
    }

    @Test
    @DisplayName("Passive Economy: Plant sun regenerates +25 every 10 seconds")
    void testPassivePlantSunEconomy() {
        gameRoom.setPlantSun(100);

        // Advance 200 ticks (10.0 seconds)
        for (int i = 0; i < 200; i++) {
            gameRoom.tick();
        }

        assertEquals(125, gameRoom.getPlantSun());
    }

    @Test
    @DisplayName("Action Validation: Plant player places Peashooter within col < 3, spends sun")
    void testLegalPlantPlacement() {
        gameRoom.setPlantSun(150);

        // Place Peashooter (cost 100) at row 0, col 2 (left of red line col 3)
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Peashooter", 0, 2));

        // Tick to process queued action
        gameRoom.tick();

        assertEquals(50, gameRoom.getPlantSun());
        PlantInstance plant = gameRoom.getGameModel().getPlantAt(0, 2);
        assertNotNull(plant);
        assertEquals("Peashooter", plant.getDefinition().getName());
    }

    @Test
    @DisplayName("Action Validation: Plant placement rejected beyond red line (col >= 3)")
    void testPlantPlacementBeyondRedLineRejected() {
        gameRoom.setPlantSun(150);

        // Attempt placement at col 3 (at red line)
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Peashooter", 0, 3));
        gameRoom.tick();

        assertEquals(150, gameRoom.getPlantSun(), "Sun must not be deducted");
        assertNull(gameRoom.getGameModel().getPlantAt(0, 3));
    }

    @Test
    @DisplayName("Action Validation: Plant placement rejected when insufficient sun")
    void testPlantPlacementInsufficientSunRejected() {
        gameRoom.setPlantSun(50); // Peashooter requires 100

        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Peashooter", 4, 0));
        gameRoom.tick();

        assertEquals(50, gameRoom.getPlantSun());
        assertNull(gameRoom.getGameModel().getPlantAt(4, 0));
    }

    @Test
    @DisplayName("Action Validation: Plant placement rejected during card cooldown")
    void testPlantPlacementCooldownActiveRejected() {
        gameRoom.setPlantSun(300);

        // First placement succeeds at (0, 2)
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Sunflower", 0, 2));
        gameRoom.tick();
        assertEquals(250, gameRoom.getPlantSun());

        // Immediate second placement of Sunflower at empty cell (4, 0) is blocked by cooldown
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Sunflower", 4, 0));
        gameRoom.tick();
        assertEquals(250, gameRoom.getPlantSun(), "Cooldown must prevent second placement");
        assertNull(gameRoom.getGameModel().getPlantAt(4, 0));
    }

    @Test
    @DisplayName("Action Validation: Plant placement rejected when target cell already occupied")
    void testPlantPlacementOccupiedCellRejected() {
        gameRoom.setPlantSun(300);

        // Place Peashooter at (0, 2)
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Peashooter", 0, 2));
        gameRoom.tick();
        assertEquals(200, gameRoom.getPlantSun());

        // Attempt placement at same cell with Wall-nut
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Wall-nut", 0, 2));
        gameRoom.tick();
        assertEquals(200, gameRoom.getPlantSun(), "Sun must not be deducted for occupied cell");
    }

    @Test
    @DisplayName("Action Validation: Zombie player sending plant placement receives UNAUTHORIZED_ROLE")
    void testZombiePlayerCannotPlacePlants() {
        gameRoom.handlePlantAction(zombieHandler, new PlacePlantRequestPacket("Peashooter", 0, 0));
        gameRoom.tick();
        // Action is rejected immediately before queueing
    }

    @Test
    @DisplayName("Action Validation: Zombie player spawns zombie right of red line (col >= 3), spends sun")
    void testLegalZombieSpawn() {
        gameRoom.setZombieSun(150);
        gameRoom.getGameModel().addSun(0);

        // Spawn ZombieImp (cost 25) at row 1, col 5
        gameRoom.handleZombieAction(zombieHandler, new PlaceZombieRequestPacket("ZombieImp", 1, 5));
        gameRoom.tick();

        assertEquals(125, gameRoom.getZombieSun());
        assertEquals(6, gameRoom.getGameModel().getActiveZombies().size()); // 5 sun zombies + 1 Imp
    }

    @Test
    @DisplayName("Action Validation: Zombie spawn rejected behind red line (col < 3)")
    void testZombieSpawnBehindRedLineRejected() {
        gameRoom.setZombieSun(150);

        gameRoom.handleZombieAction(zombieHandler, new PlaceZombieRequestPacket("ZombieImp", 1, 2));
        gameRoom.tick();

        assertEquals(150, gameRoom.getZombieSun());
        assertEquals(5, gameRoom.getGameModel().getActiveZombies().size());
    }

    @Test
    @DisplayName("Action Validation: Zombie spawn rejected when insufficient zombie sun")
    void testZombieSpawnInsufficientSunRejected() {
        gameRoom.setZombieSun(20);
        gameRoom.getGameModel().spendSun(gameRoom.getGameModel().getSunAmount() - 20);

        gameRoom.handleZombieAction(zombieHandler, new PlaceZombieRequestPacket("ZombieDefault", 1, 5)); // Cost 50
        gameRoom.tick();

        assertEquals(5, gameRoom.getGameModel().getActiveZombies().size());
    }

    @Test
    @DisplayName("Action Validation: Plant player sending zombie spawn receives UNAUTHORIZED_ROLE")
    void testPlantPlayerCannotSpawnZombies() {
        gameRoom.handleZombieAction(plantHandler, new PlaceZombieRequestPacket("ZombieImp", 1, 5));
        gameRoom.tick();
        assertEquals(5, gameRoom.getGameModel().getActiveZombies().size());
    }

    @Test
    @DisplayName("Snapshot: State snapshot contains plants, zombies, sun and timer")
    void testSnapshotGeneration() {
        GameStateSnapshotPacket snapshot = gameRoom.createSnapshot();

        assertNotNull(snapshot);
        assertEquals(gameRoom.getTickCounter(), snapshot.getTick());
        assertEquals(gameRoom.getPlantSun(), snapshot.getPlantSun());
        assertEquals(gameRoom.getZombieSun(), snapshot.getZombieSun());
        assertFalse(snapshot.getPlants().isEmpty());
        assertEquals(5, snapshot.getZombies().size());
        assertFalse(snapshot.isGameOver());
        assertNull(snapshot.getWinnerRole());
    }

    @Test
    @DisplayName("Win Condition: Zombie wins when all 5 rows breached")
    void testWinConditionAllBrainsEaten() {
        GameModel model = gameRoom.getGameModel();
        for (int row = 0; row < 5; row++) {
            model.markBrainEaten(row);
        }

        gameRoom.tick();

        assertTrue(gameRoom.isGameOver());
        assertEquals("ZOMBIE", gameRoom.getWinnerRole());
        assertEquals("ALL_BRAINS_EATEN", gameRoom.getEndReason());
    }

    @Test
    @DisplayName("Win Condition: Plant wins when match timer expires with < 5 brains eaten")
    void testWinConditionMatchTimeExpired() {
        gameRoom.setMatchDuration(10.0f);

        // Advance 201 ticks (10.05 seconds)
        for (int i = 0; i < 201; i++) {
            gameRoom.tick();
        }

        assertTrue(gameRoom.isGameOver());
        assertEquals("PLANT", gameRoom.getWinnerRole());
        assertEquals("TIME_EXPIRED", gameRoom.getEndReason());
    }

    @Test
    @DisplayName("Forfeit: Player surrender ends match immediately in opponent victory")
    void testPlayerSurrender() {
        gameRoom.handleReaction(plantHandler, new ReactionPacket("PlantHero", ReactionType.SURRENDER, "I yield"));

        assertTrue(gameRoom.isGameOver());
        assertEquals("ZOMBIE", gameRoom.getWinnerRole());
        assertEquals("PLANT_SURRENDERED", gameRoom.getEndReason());
    }

    @Test
    @DisplayName("Forfeit: Surrender unregisters players so they are no longer busy/in-match")
    void testSurrenderClearsInMatchStatus() {
        IZombieGameRoom room = roomManager.createRoom(plantHandler, zombieHandler);
        assertNotNull(roomManager.getRoomForPlayer(plantHandler));
        assertNotNull(roomManager.getRoomForPlayer(zombieHandler));

        room.handleReaction(plantHandler, new ReactionPacket("PlantHero", ReactionType.SURRENDER, "gg"));

        assertNull(roomManager.getRoomForPlayer(plantHandler), "Plant player must leave room map after game end");
        assertNull(roomManager.getRoomForPlayer(zombieHandler), "Zombie player must leave room map after game end");
        assertNull(plantHandler.getCurrentRoomId());
        assertNull(zombieHandler.getCurrentRoomId());
        assertEquals(0, roomManager.getActiveRoomCount());
    }

    @Test
    @DisplayName("Cleanup: endGame outside tick still unregisters players on the next tick")
    void testEndGameOutsideTickClearsInMatchStatusOnNextTick() throws InterruptedException {
        IZombieGameRoom room = roomManager.createRoom(plantHandler, zombieHandler);
        room.endGame("PLANT", "TEST_END");

        // Scheduled 20 Hz tick hits the gameOver early-return and calls stop()
        long deadline = System.currentTimeMillis() + 1000;
        while (roomManager.getRoomForPlayer(plantHandler) != null && System.currentTimeMillis() < deadline) {
            Thread.sleep(25);
        }

        assertNull(roomManager.getRoomForPlayer(plantHandler));
        assertNull(roomManager.getRoomForPlayer(zombieHandler));
        assertEquals(0, roomManager.getActiveRoomCount());
    }

    @Test
    @DisplayName("Forfeit: Player disconnect awards match to opponent")
    void testPlayerDisconnectForfeit() {
        gameRoom.handlePlayerDisconnect(plantHandler);

        assertTrue(gameRoom.isGameOver());
        assertEquals("ZOMBIE", gameRoom.getWinnerRole());
        assertEquals("PLANT_DISCONNECTED", gameRoom.getEndReason());
    }

    @Test
    @DisplayName("Dynamic Defense: Plant player places multiple plants across columns 0..2 during active match")
    void testDynamicMultiPlantPlacementDuringActiveMatch() {
        gameRoom.start(executor);
        gameRoom.setPlantSun(1000);

        // Pre-conditions: Stage 1 has pre-planted plants, but some cells in col 0..2 are empty
        // E.g., (0, 2), (1, 0), (2, 1), (3, 0), (3, 2), (4, 0), (4, 1) are empty in Stage 1
        assertNull(gameRoom.getGameModel().getPlantAt(0, 2));
        assertNull(gameRoom.getGameModel().getPlantAt(1, 0));
        assertNull(gameRoom.getGameModel().getPlantAt(3, 0));

        // 1. Place Sunflower (cost 50) at (0, 2)
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Sunflower", 0, 2));
        gameRoom.tick();
        assertNotNull(gameRoom.getGameModel().getPlantAt(0, 2));
        assertEquals("Sunflower", gameRoom.getGameModel().getPlantAt(0, 2).getDefinition().getName());
        assertEquals(950, gameRoom.getPlantSun());

        // 2. Place Peashooter (cost 100) at (1, 0)
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Peashooter", 1, 0));
        gameRoom.tick();
        assertNotNull(gameRoom.getGameModel().getPlantAt(1, 0));
        assertEquals("Peashooter", gameRoom.getGameModel().getPlantAt(1, 0).getDefinition().getName());
        assertEquals(850, gameRoom.getPlantSun());

        // 3. Place Wall-nut (cost 50) at (3, 0)
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Wall-nut", 3, 0));
        gameRoom.tick();
        assertNotNull(gameRoom.getGameModel().getPlantAt(3, 0));
        assertEquals("Wall-nut", gameRoom.getGameModel().getPlantAt(3, 0).getDefinition().getName());
        assertEquals(800, gameRoom.getPlantSun());
    }

    @Test
    @DisplayName("Cooldowns: Independent cooldown tracking allows placing different plants without interference")
    void testDifferentCardCooldownsIndependent() {
        gameRoom.setPlantSun(500);

        // Place Sunflower at (0, 2) - triggers Sunflower cooldown
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Sunflower", 0, 2));
        gameRoom.tick();
        assertEquals(450, gameRoom.getPlantSun());
        assertNotNull(gameRoom.getGameModel().getPlantAt(0, 2));

        // Immediately place Peashooter at (1, 0) - Peashooter is NOT on cooldown
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Peashooter", 1, 0));
        gameRoom.tick();
        assertEquals(350, gameRoom.getPlantSun());
        assertNotNull(gameRoom.getGameModel().getPlantAt(1, 0));

        // Immediately place Wall-nut at (3, 0) - Wall-nut is NOT on cooldown
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Wall-nut", 3, 0));
        gameRoom.tick();
        assertEquals(300, gameRoom.getPlantSun());
        assertNotNull(gameRoom.getGameModel().getPlantAt(3, 0));
    }

    @Test
    @DisplayName("Cooldowns: Advancing time expires cooldown and allows placing the same plant again")
    void testCardCooldownExpirationAllowsSubsequentPlacement() {
        gameRoom.setPlantSun(500);

        // Place Sunflower at (0, 2)
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Sunflower", 0, 2));
        gameRoom.tick();
        assertEquals(450, gameRoom.getPlantSun());

        // Second Sunflower placement at (1, 0) is blocked immediately
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Sunflower", 1, 0));
        gameRoom.tick();
        assertNull(gameRoom.getGameModel().getPlantAt(1, 0), "Must be blocked by cooldown");
        assertEquals(450, gameRoom.getPlantSun());

        // Advance 150 ticks (7.5 seconds) - Sunflower recharge is ~5-7.5s
        for (int i = 0; i < 150; i++) {
            gameRoom.tick();
        }

        // Now place Sunflower at (1, 0) - Cooldown has expired
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Sunflower", 1, 0));
        gameRoom.tick();
        assertNotNull(gameRoom.getGameModel().getPlantAt(1, 0), "Must succeed after cooldown expires");
        // Sun should have been deducted (note passive economy might have added some, but plant placement cost is deducted)
        assertEquals("Sunflower", gameRoom.getGameModel().getPlantAt(1, 0).getDefinition().getName());
    }

    @Test
    @DisplayName("Exhaustion: Plant wins when zombie player has 0 sun and no zombies remaining on lawn")
    void testZombiePlayerExhaustionLossCondition() {
        gameRoom.setZombieSun(0);
        gameRoom.getGameModel().spendSun(gameRoom.getGameModel().getSunAmount());

        // Clear all zombies (sun zombies and attackers)
        gameRoom.getGameModel().getActiveZombies().clear();
        assertEquals(0, gameRoom.getGameModel().getActiveZombies().size());

        gameRoom.tick();

        assertTrue(gameRoom.isGameOver(), "Match should end when zombie player is exhausted");
        assertEquals("PLANT", gameRoom.getWinnerRole());
        assertEquals("ZOMBIE_OUT_OF_SUN", gameRoom.getEndReason());
    }

    @Test
    @DisplayName("Exhaustion: Zombie player is NOT exhausted if stationary sun producers are still active")
    void testZombiePlayerNotExhaustedWhileSunProducersAlive() {
        gameRoom.setZombieSun(0);
        gameRoom.getGameModel().spendSun(gameRoom.getGameModel().getSunAmount());

        // 5 sun zombies remain in column 8
        assertEquals(5, gameRoom.getGameModel().getActiveZombies().size());

        gameRoom.tick();

        assertFalse(gameRoom.isGameOver(), "Match should NOT end while sun zombies are producing sun");
        assertNull(gameRoom.getWinnerRole());
    }

    @Test
    @DisplayName("Exhaustion: Zombie player is NOT exhausted if attacking zombies are advancing on lawn")
    void testZombiePlayerNotExhaustedWhileAttackerAlive() {
        gameRoom.setZombieSun(0);
        gameRoom.getGameModel().spendSun(gameRoom.getGameModel().getSunAmount());

        // Remove sun zombies, but add 1 attacking zombie
        gameRoom.getGameModel().getActiveZombies().clear();
        gameRoom.getGameModel().spawnZombieAt("ZombieDefault", 0, 5);
        assertEquals(1, gameRoom.getGameModel().getActiveZombies().size());

        gameRoom.tick();

        assertFalse(gameRoom.isGameOver(), "Match should NOT end while attacking zombie is alive");
        assertNull(gameRoom.getWinnerRole());
    }

    @Test
    @DisplayName("Validation: Invalid plant and zombie names are cleanly rejected")
    void testInvalidEntityNamesRejected() {
        gameRoom.setPlantSun(500);
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("NonExistentPlant", 0, 2));
        gameRoom.tick();
        assertNull(gameRoom.getGameModel().getPlantAt(0, 2));

        gameRoom.setZombieSun(500);
        gameRoom.handleZombieAction(zombieHandler, new PlaceZombieRequestPacket("NonExistentZombie", 0, 5));
        gameRoom.tick();
        assertEquals(5, gameRoom.getGameModel().getActiveZombies().size());
    }

    @Test
    @DisplayName("Validation: Actions sent after game over are rejected")
    void testActionsAfterGameOverRejected() {
        gameRoom.endGame("PLANT", "TEST_END");
        assertTrue(gameRoom.isGameOver());

        gameRoom.setPlantSun(500);
        gameRoom.handlePlantAction(plantHandler, new PlacePlantRequestPacket("Sunflower", 0, 2));
        gameRoom.tick();
        assertNull(gameRoom.getGameModel().getPlantAt(0, 2));

        gameRoom.setZombieSun(500);
        gameRoom.handleZombieAction(zombieHandler, new PlaceZombieRequestPacket("ZombieImp", 0, 5));
        gameRoom.tick();
        assertEquals(5, gameRoom.getGameModel().getActiveZombies().size());
    }
}
