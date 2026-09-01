package com.sut.server.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sut.server.repository.ServerUserRepository;
import com.sut.server.room.RoomManager;
import com.sut.server.service.AuthService;
import com.sut.server.service.LobbyService;
import model.data.minigame.MiniGameRegistry;
import model.network.dto.PlantSnapshotDto;
import model.network.dto.ZombieSnapshotDto;
import model.network.enums.MatchmakingMode;
import model.network.enums.MatchmakingStatus;
import model.network.enums.PlayerRole;
import model.network.enums.ReactionType;
import model.network.packet.Packet;
import model.network.packet.auth.LoginRequestPacket;
import model.network.packet.auth.LoginResponsePacket;
import model.network.packet.auth.RegisterRequestPacket;
import model.network.packet.auth.RegisterResponsePacket;
import model.network.packet.chat.ReactionPacket;
import model.network.packet.game.GameStateSnapshotPacket;
import model.network.packet.game.PlacePlantRequestPacket;
import model.network.packet.game.PlaceZombieRequestPacket;
import model.network.packet.game.PlayerActionResponsePacket;
import model.network.packet.matchmaking.MatchFoundPacket;
import model.network.packet.matchmaking.MatchmakingRequestPacket;
import model.network.packet.matchmaking.MatchmakingResponsePacket;
import model.plant.PlantFactory;
import model.zombie.ZombieFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiplayerGameIntegrationTest {

    @TempDir
    Path tempDir;

    private TcpServer tcpServer;
    private ServerUserRepository userRepository;
    private AuthService authService;
    private RoomManager roomManager;
    private LobbyService lobbyService;
    private PacketRouter packetRouter;
    private ObjectMapper objectMapper;
    private int boundPort;

    private static class NetworkTestClient {
        final Socket socket;
        final BufferedReader reader;
        final BufferedWriter writer;
        final ObjectMapper mapper;
        final List<Packet> receivedPackets = new ArrayList<>();

        NetworkTestClient(int port, ObjectMapper mapper) throws IOException {
            this.socket = new Socket("127.0.0.1", port);
            this.mapper = mapper;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        void sendPacket(Packet packet) throws IOException {
            String json = mapper.writeValueAsString(packet);
            writer.write(json);
            writer.newLine();
            writer.flush();
        }

        <T extends Packet> T receiveNextPacket(Class<T> expectedType, long timeoutMs) throws Exception {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                // Check buffered received packets
                for (int i = 0; i < receivedPackets.size(); i++) {
                    if (expectedType.isInstance(receivedPackets.get(i))) {
                        return expectedType.cast(receivedPackets.remove(i));
                    }
                }

                int remaining = (int) Math.max(10, deadline - System.currentTimeMillis());
                socket.setSoTimeout(remaining);
                try {
                    String line = reader.readLine();
                    if (line != null && !line.isBlank()) {
                        Packet p = mapper.readValue(line, Packet.class);
                        if (expectedType.isInstance(p)) {
                            return expectedType.cast(p);
                        }
                        receivedPackets.add(p);
                    }
                } catch (java.net.SocketTimeoutException ignored) {
                    // continue loop until deadline
                }
            }
            throw new AssertionError("Timed out waiting for packet of type " + expectedType.getSimpleName()
                    + ". Received: " + receivedPackets);
        }

        void close() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        try { PlantFactory.init("/assets/data/plants/plants.json"); } catch (Exception ignored) {}
        try { ZombieFactory.init("/assets/data/zombies/zombies.json", "/assets/data/armor/ArmorTypeData.json"); } catch (Exception ignored) {}
        try { MiniGameRegistry.init("/assets/data/minigames/minigames.json"); } catch (Exception ignored) {}

        Path storagePath = tempDir.resolve("integration-users.json");
        userRepository = new ServerUserRepository(storagePath);
        authService = new AuthService(userRepository);
        roomManager = new RoomManager();
        lobbyService = new LobbyService(roomManager);

        packetRouter = new PacketRouter();
        authService.registerRoutes(packetRouter);
        lobbyService.registerRoutes(packetRouter);
        roomManager.registerRoutes(packetRouter);

        objectMapper = TcpServer.createDefaultObjectMapper();
        tcpServer = new TcpServer("127.0.0.1", 0, packetRouter, objectMapper);
        tcpServer.start();
        boundPort = tcpServer.getBoundPort();
    }

    @AfterEach
    void tearDown() {
        if (roomManager != null) {
            roomManager.shutdown();
        }
        if (tcpServer != null) {
            tcpServer.stop();
        }
    }

    @Test
    @DisplayName("End-to-End Multiplayer Flow: Register -> Login -> Matchmaking -> Room Ticking -> Actions -> Snapshots -> Surrender")
    void testCompleteMultiplayerGameFlow() throws Exception {
        NetworkTestClient clientPlant = new NetworkTestClient(boundPort, objectMapper);
        NetworkTestClient clientZombie = new NetworkTestClient(boundPort, objectMapper);

        try {
            // 1. Register & Login Player 1
            clientPlant.sendPacket(new RegisterRequestPacket("player1", "Pass1234!", "PlayerOne", "p1@pvz.com", "male", 1, "Ans1"));
            RegisterResponsePacket reg1 = clientPlant.receiveNextPacket(RegisterResponsePacket.class, 2000);
            assertTrue(reg1.isSuccess());

            clientPlant.sendPacket(new LoginRequestPacket("player1", "Pass1234!", false));
            LoginResponsePacket log1 = clientPlant.receiveNextPacket(LoginResponsePacket.class, 2000);
            assertTrue(log1.isSuccess());

            // 2. Register & Login Player 2
            clientZombie.sendPacket(new RegisterRequestPacket("player2", "Pass1234!", "PlayerTwo", "p2@pvz.com", "female", 2, "Ans2"));
            RegisterResponsePacket reg2 = clientZombie.receiveNextPacket(RegisterResponsePacket.class, 2000);
            assertTrue(reg2.isSuccess());

            clientZombie.sendPacket(new LoginRequestPacket("player2", "Pass1234!", false));
            LoginResponsePacket log2 = clientZombie.receiveNextPacket(LoginResponsePacket.class, 2000);
            assertTrue(log2.isSuccess());

            // 3. Matchmaking Queue Enqueue
            clientPlant.sendPacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, PlayerRole.PLANT, "player1"));
            MatchmakingResponsePacket queueResp = clientPlant.receiveNextPacket(MatchmakingResponsePacket.class, 2000);
            assertEquals(MatchmakingStatus.QUEUED, queueResp.getStatus());

            // Player 2 queues as ZOMBIE
            clientZombie.sendPacket(new MatchmakingRequestPacket(MatchmakingMode.RANDOM, null, PlayerRole.ZOMBIE, "player2"));

            // 4. Both receive MatchFoundPacket
            MatchFoundPacket matchPlant = clientPlant.receiveNextPacket(MatchFoundPacket.class, 2000);
            MatchFoundPacket matchZombie = clientZombie.receiveNextPacket(MatchFoundPacket.class, 2000);

            assertEquals(PlayerRole.PLANT, matchPlant.getAssignedRole());
            assertEquals("player2", matchPlant.getOpponentUsername());

            assertEquals(PlayerRole.ZOMBIE, matchZombie.getAssignedRole());
            assertEquals("player1", matchZombie.getOpponentUsername());

            assertEquals(matchPlant.getRoomId(), matchZombie.getRoomId());
            assertEquals(1, roomManager.getActiveRoomCount());

            // 5. Receive 20 Hz periodic GameStateSnapshotPackets from server
            GameStateSnapshotPacket snapshot1 = clientPlant.receiveNextPacket(GameStateSnapshotPacket.class, 2000);
            assertNotNull(snapshot1);
            assertEquals(150, snapshot1.getPlantSun());
            assertEquals(150, snapshot1.getZombieSun());
            assertFalse(snapshot1.getPlants().isEmpty());
            assertEquals(5, snapshot1.getZombies().size()); // 5 sun zombies

            // 6. Plant Player places a Peashooter at unoccupied (row 1, col 0)
            clientPlant.sendPacket(new PlacePlantRequestPacket("Peashooter", 1, 0));
            PlayerActionResponsePacket plantActionResp = clientPlant.receiveNextPacket(PlayerActionResponsePacket.class, 2000);
            assertTrue(plantActionResp.isSuccess());
            assertEquals("PLACE_PLANT", plantActionResp.getActionType());
            assertEquals(1, plantActionResp.getRow());
            assertEquals(0, plantActionResp.getCol());

            // 7. Zombie Player spawns a ZombieImp at (row 1, col 6)
            clientZombie.sendPacket(new PlaceZombieRequestPacket("ZombieImp", 1, 6));
            PlayerActionResponsePacket zombieActionResp = clientZombie.receiveNextPacket(PlayerActionResponsePacket.class, 2000);
            assertTrue(zombieActionResp.isSuccess());
            assertEquals("PLACE_ZOMBIE", zombieActionResp.getActionType());
            assertEquals(1, zombieActionResp.getRow());
            assertEquals(6, zombieActionResp.getCol());

            // 8. Receive updated GameStateSnapshot reflecting actions (poll up to 2.5s for tick to process and broadcast)
            GameStateSnapshotPacket snapshotAfterActions = null;
            long deadline = System.currentTimeMillis() + 2500;
            boolean foundPlacedPlant = false;
            boolean foundSpawnedZombie = false;

            while (System.currentTimeMillis() < deadline && (!foundPlacedPlant || !foundSpawnedZombie)) {
                GameStateSnapshotPacket s = clientPlant.receiveNextPacket(GameStateSnapshotPacket.class, 500);
                if (s != null) {
                    snapshotAfterActions = s;
                    if (s.getPlants().stream().anyMatch(p -> "Peashooter".equalsIgnoreCase(p.getPlantName()) && p.getRow() == 1 && p.getCol() == 0)) {
                        foundPlacedPlant = true;
                    }
                    if (s.getZombies().stream().anyMatch(z -> "ZombieImp".equalsIgnoreCase(z.getZombieName()) && z.getRow() == 1)) {
                        foundSpawnedZombie = true;
                    }
                }
            }

            assertNotNull(snapshotAfterActions);
            assertTrue(foundPlacedPlant, "Snapshot must reflect placed Peashooter at (1, 0)");
            assertTrue(foundSpawnedZombie, "Snapshot must reflect spawned ZombieImp in row 1");

            // 9. Chat Reaction Exchange
            clientPlant.sendPacket(new ReactionPacket("player1", ReactionType.EMOJI, "SMILE"));
            ReactionPacket reactionReceived = clientZombie.receiveNextPacket(ReactionPacket.class, 2000);
            assertEquals("player1", reactionReceived.getSenderUsername());
            assertEquals(ReactionType.EMOJI, reactionReceived.getReactionType());
            assertEquals("SMILE", reactionReceived.getContent());

            // 10. Surrender / Game Over Flow
            clientPlant.sendPacket(new ReactionPacket("player1", ReactionType.SURRENDER, "gg"));
            GameStateSnapshotPacket gameOverSnapshot = null;
            long goDeadline = System.currentTimeMillis() + 2500;
            while (System.currentTimeMillis() < goDeadline) {
                GameStateSnapshotPacket s = clientZombie.receiveNextPacket(GameStateSnapshotPacket.class, 500);
                if (s != null && s.isGameOver()) {
                    gameOverSnapshot = s;
                    break;
                }
            }
            assertNotNull(gameOverSnapshot, "Must receive game over snapshot after surrender");
            assertTrue(gameOverSnapshot.isGameOver());
            assertEquals("ZOMBIE", gameOverSnapshot.getWinnerRole());
            assertEquals("PLANT_SURRENDERED", gameOverSnapshot.getEndReason());

        } finally {
            clientPlant.close();
            clientZombie.close();
        }
    }
}
