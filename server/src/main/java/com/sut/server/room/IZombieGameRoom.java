package com.sut.server.room;

import com.sut.server.net.ClientConnectionHandler;
import model.data.minigame.MiniGameRegistry;
import model.enums.MiniGameType;
import model.enums.PlacableLayer;
import model.enums.PlantState;
import model.enums.ZombieState;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.level.minigame.izombie.IZombieLevel;
import model.game.level.minigame.izombie.IZombieSettings;
import model.game.map.Cell;
import model.network.dto.PlantSnapshotDto;
import model.network.dto.ProjectileSnapshotDto;
import model.network.dto.SunSnapshotDto;
import model.network.dto.ZombieSnapshotDto;
import model.network.enums.PlayerRole;
import model.network.enums.ReactionType;
import model.network.packet.chat.ReactionPacket;
import model.network.packet.game.CollectSunRequestPacket;
import model.network.packet.game.GameStateSnapshotPacket;
import model.network.packet.game.PlacePlantRequestPacket;
import model.network.packet.game.PlaceZombieRequestPacket;
import model.network.packet.game.PlayerActionResponsePacket;
import model.network.packet.matchmaking.MatchFoundPacket;
import model.item.Sun;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.zombie.ZombieFactory;
import model.zombie.armor.Armor;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Authoritative server-side game room for 1v1 Multiplayer "I, Zombie".
 * Executes headless PvZGameLoop at 20 Hz (50ms dt), enforces asymmetric role actions,
 * manages plant/zombie economies, and broadcasts authoritative GameStateSnapshotPackets.
 */
public class IZombieGameRoom implements Runnable {

    public static final float TICK_INTERVAL_SECONDS = 0.05f; // 20 Hz
    public static final long TICK_INTERVAL_MS = 50L;
    public static final float DEFAULT_MATCH_DURATION = 180f; // 3 minutes
    public static final int DEFAULT_INITIAL_SUN = 150;

    private final String roomId;
    private final RoomManager roomManager;
    private final ClientConnectionHandler plantPlayer;
    private final ClientConnectionHandler zombiePlayer;

    private final GameModel gameModel;
    private final PvZGameLoop gameLoop;
    private final IZombieLevel level;
    private final int redLineColumn;

    // Asymmetric Economies & Match Progress
    private int plantSun = DEFAULT_INITIAL_SUN;
    private int zombieSun = DEFAULT_INITIAL_SUN;
    private float matchTime = 0f;
    private float matchDuration = DEFAULT_MATCH_DURATION;
    private long tickCounter = 0;

    private volatile boolean running = false;
    private volatile boolean gameOver = false;
    private String winnerRole = null;
    private String endReason = null;

    private final ConcurrentLinkedQueue<Runnable> pendingActions = new ConcurrentLinkedQueue<>();
    private final Map<String, Float> plantCardCooldowns = new ConcurrentHashMap<>();
    private final Map<Object, String> entityIdMap = new ConcurrentHashMap<>();
    private final AtomicLong entityIdSequence = new AtomicLong(1);

    private ScheduledFuture<?> scheduledTickFuture;

    public IZombieGameRoom(
            String roomId,
            RoomManager roomManager,
            ClientConnectionHandler plantPlayer,
            ClientConnectionHandler zombiePlayer
    ) {
        this.roomId = roomId;
        this.roomManager = roomManager;
        this.plantPlayer = plantPlayer;
        this.zombiePlayer = zombiePlayer;

        ensureCatalogsLoaded();
        this.level = createLevel();
        this.redLineColumn = level.redLineColumn();
        this.gameModel = new GameModel(level);
        this.level.onStart(gameModel);
        this.gameLoop = new PvZGameLoop(gameModel);
        this.plantSun = DEFAULT_INITIAL_SUN;
        this.zombieSun = gameModel.getSunAmount() > 0 ? gameModel.getSunAmount() : DEFAULT_INITIAL_SUN;
        if (gameModel.getSunAmount() != zombieSun) {
            gameModel.addSun(zombieSun - gameModel.getSunAmount());
        }
        if (plantPlayer != null) {
            plantPlayer.setCurrentRoomId(roomId);
        }
        if (zombiePlayer != null) {
            zombiePlayer.setCurrentRoomId(roomId);
        }
    }

    /**
     * Starts the room ticking loop on the scheduled executor service.
     */
    public synchronized void start(ScheduledExecutorService executor) {
        if (running || gameOver) {
            return;
        }
        this.running = true;

        // Broadcast initial match found packet with authoritative assigned roles
        if (plantPlayer != null) {
            String opp = zombiePlayer != null ? zombiePlayer.getUsername() : "Opponent";
            plantPlayer.sendPacket(new MatchFoundPacket(roomId, opp, PlayerRole.PLANT, 3));
        }
        if (zombiePlayer != null) {
            String opp = plantPlayer != null ? plantPlayer.getUsername() : "Opponent";
            zombiePlayer.sendPacket(new MatchFoundPacket(roomId, opp, PlayerRole.ZOMBIE, 3));
        }

        // Schedule 20 Hz loop
        this.scheduledTickFuture = executor.scheduleAtFixedRate(this, 100, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Periodic 20 Hz simulation tick.
     */
    @Override
    public void run() {
        tick();
    }

    public void tick() {
        if (gameOver) {
            stop();
            return;
        }
        try {
            tickCounter++;
            drainPendingActions();
            tickPlantCooldowns();
            gameLoop.update(TICK_INTERVAL_SECONDS);
            matchTime += TICK_INTERVAL_SECONDS;
            zombieSun = gameModel.getSunAmount();
            checkWinLossConditions();
            broadcastSnapshot();
            if (gameOver) {
                stop();
            }
        } catch (Exception e) {
            System.err.println("[IZombieGameRoom] Exception during room tick in "
                    + roomId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drainPendingActions() {
        Runnable action;
        while ((action = pendingActions.poll()) != null) {
            try {
                action.run();
            } catch (Exception e) {
                System.err.println("[IZombieGameRoom] Error executing player action runnable: "
                        + e.getMessage());
            }
        }
    }

    private void tickPlantCooldowns() {
        for (Map.Entry<String, Float> entry : plantCardCooldowns.entrySet()) {
            float remaining = entry.getValue() - TICK_INTERVAL_SECONDS;
            if (remaining <= 0f) {
                plantCardCooldowns.remove(entry.getKey());
            } else {
                entry.setValue(remaining);
            }
        }
    }

    /**
     * Evaluates authoritative win/loss conditions for 1v1 "I, Zombie".
     */
    private void checkWinLossConditions() {
        if (gameOver) return;

        int totalRows = gameModel.getGameMap() != null ? gameModel.getGameMap().getRows() : 5;

        // Condition 1: Zombie Victory — All lane brains eaten
        if (gameModel.getBreachedRows().size() >= totalRows) {
            endGame("ZOMBIE", "ALL_BRAINS_EATEN");
            return;
        }

        // Condition 2: Plant Victory — Match duration timer expired with intact brains
        if (matchTime >= matchDuration) {
            endGame("PLANT", "TIME_EXPIRED");
            return;
        }

        // Condition 3: Plant Victory — Zombie player out of sun and no attacking zombies active
        if (isZombiePlayerExhausted()) {
            endGame("PLANT", "ZOMBIE_OUT_OF_SUN");
        }
    }

    /**
     * Checks if the zombie player has no sun left to place even the cheapest zombie
     * and has no active attacking zombies advancing on the lawn.
     */
    private boolean isZombiePlayerExhausted() {
        int minCost = level != null && level.getSettings() != null ? level.getSettings().minZombieCost() : 25;
        if (minCost <= 0) minCost = 25;

        if (zombieSun >= minCost) {
            return false;
        }

        String sunZombieName = level != null && level.getSettings() != null
                ? level.getSettings().getSunZombie() : "ZombieIZombieSun";

        // Check if there are attacking zombies or stationary sun producers that could still produce sun
        boolean hasSunProducers = false;
        boolean hasAttackingZombies = false;

        for (ZombieInstance z : gameModel.getActiveZombies()) {
            if (z.getState() == ZombieState.DYING) continue;
            String name = z.getDefinition() != null ? z.getDefinition().getName() : "";
            if (name.equalsIgnoreCase(sunZombieName)) {
                hasSunProducers = true;
            } else {
                hasAttackingZombies = true;
            }
        }

        // If no attacking zombies and no sun producers to generate more sun, zombie player is exhausted
        return !hasAttackingZombies && !hasSunProducers;
    }

    /**
     * Ends the game with a specified winner and reason.
     */
    public synchronized void endGame(String winner, String reason) {
        if (this.gameOver) return;
        this.gameOver = true;
        this.winnerRole = winner;
        this.endReason = reason;
        System.out.println("[IZombieGameRoom] Game Over in " + roomId + ". Winner: " + winner + ", Reason: " + reason);
    }

    /**
     * Handles sun collection from the plant defender.
     */
    public void handleCollectSun(ClientConnectionHandler sender, CollectSunRequestPacket packet) {
        if (packet == null) {
            return;
        }
        if (sender != plantPlayer) {
            return;
        }
        pendingActions.offer(() -> {
            if (gameOver) {
                return;
            }
            Sun picked = null;
            for (Sun sun : gameModel.getActiveSuns()) {
                if (sun != null && sun.getX() == packet.getX() && sun.getY() == packet.getY()) {
                    picked = sun;
                    break;
                }
            }
            if (picked == null) {
                return;
            }
            gameModel.collectSun(picked);
            plantSun = Math.min(9990, plantSun + picked.getValue());
        });
    }

    /**
     * Handles plant placement requests from the plant defender.
     */
    public void handlePlantAction(ClientConnectionHandler sender, PlacePlantRequestPacket packet) {
        if (packet == null) {
            return;
        }
        if (sender != plantPlayer) {
            sendAction(sender, false, "PLACE_PLANT", "UNAUTHORIZED_ROLE", packet);
            return;
        }
        pendingActions.offer(() -> executePlantPlacement(sender, packet));
    }

    private void executePlantPlacement(ClientConnectionHandler sender, PlacePlantRequestPacket packet) {
        int row = packet.getRow();
        int col = packet.getCol();
        if (gameOver) {
            sendAction(sender, false, "PLACE_PLANT", "GAME_OVER", row, col);
            return;
        }
        int maxRows = gameModel.getGameMap() != null ? gameModel.getGameMap().getRows() : 5;
        if (row < 0 || row >= maxRows || col < 0 || col >= redLineColumn) {
            sendAction(sender, false, "PLACE_PLANT", "BEYOND_RED_LINE", row, col);
            return;
        }
        Plant def = PlantFactory.getDefinition(packet.getPlantName());
        String reject = plantPlacementRejectReason(def, row, col);
        if (reject != null) {
            sendAction(sender, false, "PLACE_PLANT", reject, row, col);
            return;
        }
        PlantInstance instance = PlantFactory.createInstance(def);
        if (instance == null) {
            sendAction(sender, false, "PLACE_PLANT", "CREATION_FAILED", row, col);
            return;
        }
        if (gameModel.placePlant(instance, row, col)) {
            plantSun -= def.getCost();
            float cd = def.getRechargeTime() > 0 ? def.getRechargeTime() : 5.0f;
            plantCardCooldowns.put(def.getName(), cd);
            sendAction(sender, true, "PLACE_PLANT", "OK", row, col);
        } else {
            sendAction(sender, false, "PLACE_PLANT", "PLACEMENT_REJECTED", row, col);
        }
    }

    private String plantPlacementRejectReason(Plant def, int row, int col) {
        if (def == null) {
            return "INVALID_PLANT_NAME";
        }
        if (plantSun < def.getCost()) {
            return "INSUFFICIENT_SUN";
        }
        if (plantCardCooldowns.containsKey(def.getName())) {
            return "COOLDOWN_ACTIVE";
        }
        Cell cell = gameModel.getGameMap().getCell(col, row);
        if (cell == null || cell.getPlaceable(PlacableLayer.MAIN) != null) {
            return "CELL_OCCUPIED";
        }
        return null;
    }

    /**
     * Handles zombie spawn requests from the zombie attacker.
     */
    public void handleZombieAction(ClientConnectionHandler sender, PlaceZombieRequestPacket packet) {
        if (packet == null) {
            return;
        }
        if (sender != zombiePlayer) {
            sendAction(sender, false, "PLACE_ZOMBIE", "UNAUTHORIZED_ROLE", packet);
            return;
        }
        pendingActions.offer(() -> executeZombieSpawn(sender, packet));
    }

    private void executeZombieSpawn(ClientConnectionHandler sender, PlaceZombieRequestPacket packet) {
        int row = packet.getRow();
        int col = packet.getCol();
        if (gameOver) {
            sendAction(sender, false, "PLACE_ZOMBIE", "GAME_OVER", row, col);
            return;
        }
        int maxRows = gameModel.getGameMap() != null ? gameModel.getGameMap().getRows() : 5;
        int maxCols = gameModel.getGameMap() != null ? gameModel.getGameMap().getCols() : 9;
        if (row < 0 || row >= maxRows || col < redLineColumn || col >= maxCols) {
            sendAction(sender, false, "PLACE_ZOMBIE", "BEHIND_RED_LINE", row, col);
            return;
        }
        Zombie def = ZombieFactory.getDefinition(packet.getZombieName());
        if (def == null) {
            sendAction(sender, false, "PLACE_ZOMBIE", "INVALID_ZOMBIE_NAME", row, col);
            return;
        }
        int cost = getZombieCost(def.getName());
        if (gameModel.getSunAmount() < cost || !gameModel.spendSun(cost)) {
            sendAction(sender, false, "PLACE_ZOMBIE", "INSUFFICIENT_SUN", row, col);
            return;
        }
        zombieSun = gameModel.getSunAmount();
        ZombieInstance spawned = gameModel.spawnZombieAt(def.getName(), row, col);
        if (spawned != null) {
            sendAction(sender, true, "PLACE_ZOMBIE", "OK", row, col);
        } else {
            gameModel.addSun(cost);
            zombieSun = gameModel.getSunAmount();
            sendAction(sender, false, "PLACE_ZOMBIE", "SPAWN_FAILED", row, col);
        }
    }

    /**
     * Resolves the sun cost for a given zombie name.
     */
    public int getZombieCost(String zombieName) {
        if (level != null && level.getSettings() != null) {
            Map<String, Integer> costs = level.getSettings().getZombieCosts();
            for (Map.Entry<String, Integer> e : costs.entrySet()) {
                if (e.getKey().equalsIgnoreCase(zombieName)) {
                    return e.getValue();
                }
            }
        }
        // Standard fallback costs
        return switch (zombieName) {
            case "ZombieImp" -> 25;
            case "ZombieDefault" -> 50;
            case "ZombieArmor1", "ConeheadZombie" -> 75;
            case "ZombieNewspaper" -> 100;
            case "ZombieArmor2", "BucketheadZombie" -> 125;
            case "ZombieExplorer" -> 100;
            case "ZombieProspector" -> 125;
            case "ZombieModernAllStar" -> 150;
            case "ZombieArmor4", "KnightZombie" -> 200;
            case "ZombieWizard" -> 150;
            case "ZombieGargantuar" -> 300;
            default -> 50;
        };
    }

    /**
     * Handles in-game chat and reaction emotes, forwarding them between opponents
     * and processing surrender forfeits.
     */
    public void handleReaction(ClientConnectionHandler sender, ReactionPacket packet) {
        if (packet == null) return;

        if (packet.getReactionType() == ReactionType.SURRENDER) {
            if (sender == plantPlayer) {
                endGame("ZOMBIE", "PLANT_SURRENDERED");
            } else if (sender == zombiePlayer) {
                endGame("PLANT", "ZOMBIE_SURRENDERED");
            }
            broadcastSnapshot();
            stop();
            return;
        }

        // Forward reaction to opponent
        if (sender == plantPlayer && zombiePlayer != null) {
            zombiePlayer.sendPacket(packet);
        } else if (sender == zombiePlayer && plantPlayer != null) {
            plantPlayer.sendPacket(packet);
        }
    }

    /**
     * Handles a player disconnection, awarding victory by forfeit to the opponent.
     */
    public synchronized void handlePlayerDisconnect(ClientConnectionHandler disconnected) {
        if (gameOver) return;

        if (disconnected == plantPlayer) {
            endGame("ZOMBIE", "PLANT_DISCONNECTED");
        } else if (disconnected == zombiePlayer) {
            endGame("PLANT", "ZOMBIE_DISCONNECTED");
        }

        broadcastSnapshot();
        stop();
    }

    /**
     * Serializes and broadcasts the authoritative state snapshot to both players.
     */
    public void broadcastSnapshot() {
        GameStateSnapshotPacket snapshot = createSnapshot();
        if (plantPlayer != null && !plantPlayer.isClosed()) {
            plantPlayer.sendPacket(snapshot);
        }
        if (zombiePlayer != null && !zombiePlayer.isClosed()) {
            zombiePlayer.sendPacket(snapshot);
        }
    }

    /**
     * Constructs a full GameStateSnapshotPacket from current game simulation state.
     */
    public GameStateSnapshotPacket createSnapshot() {
        List<PlantSnapshotDto> plantDtos = IZombieSnapshotMapper.mapPlants(gameModel, this::getEntityId);
        List<ZombieSnapshotDto> zombieDtos = IZombieSnapshotMapper.mapZombies(gameModel, this::getEntityId);
        List<ProjectileSnapshotDto> projectileDtos =
                IZombieSnapshotMapper.mapProjectiles(gameModel, this::getEntityId);
        List<SunSnapshotDto> sunDtos = IZombieSnapshotMapper.mapSuns(gameModel);
        List<Integer> breached = new ArrayList<>(gameModel.getBreachedRows());
        float timeRemaining = Math.max(0f, matchDuration - matchTime);
        GameStateSnapshotPacket snapshot = new GameStateSnapshotPacket(
                tickCounter,
                matchTime,
                timeRemaining,
                plantSun,
                zombieSun,
                plantDtos,
                zombieDtos,
                projectileDtos,
                breached,
                gameOver,
                winnerRole,
                endReason
        );
        snapshot.setSuns(sunDtos);
        snapshot.setPlantSeedCooldowns(new HashMap<>(plantCardCooldowns));
        snapshot.setMatchDuration(matchDuration);
        return snapshot;
    }

    /**
     * Assigns or retrieves a stable, unique identifier for an in-memory entity.
     */
    private String getEntityId(Object entity) {
        return entityIdMap.computeIfAbsent(entity, k -> "ent-" + entityIdSequence.getAndIncrement());
    }

    private static void sendAction(
            ClientConnectionHandler sender,
            boolean ok,
            String action,
            String code,
            PlacePlantRequestPacket packet) {
        sendAction(sender, ok, action, code, packet.getRow(), packet.getCol());
    }

    private static void sendAction(
            ClientConnectionHandler sender,
            boolean ok,
            String action,
            String code,
            PlaceZombieRequestPacket packet) {
        sendAction(sender, ok, action, code, packet.getRow(), packet.getCol());
    }

    private static void sendAction(
            ClientConnectionHandler sender, boolean ok, String action, String code, int row, int col) {
        sender.sendPacket(new PlayerActionResponsePacket(ok, action, code, row, col));
    }

    private static IZombieLevel createLevel() {
        try {
            if (MiniGameRegistry.getInstance() != null) {
                return (IZombieLevel) MiniGameRegistry.getInstance()
                        .createMiniGame(MiniGameType.I_ZOMBIE, 1);
            }
        } catch (Exception e) {
            System.err.println("[IZombieGameRoom] Could not create level from MiniGameRegistry: "
                    + e.getMessage());
        }
        return createDefaultIZombieLevel();
    }

    // --- Helpers & Catalogs ---
    public synchronized void stop() {
        this.running = false;
        if (scheduledTickFuture != null) {
            scheduledTickFuture.cancel(false);
            scheduledTickFuture = null;
        }
        if (roomManager != null) {
            roomManager.removeRoom(roomId);
        }
    }

    // --- Helpers & Catalogs ---

    private static synchronized void ensureCatalogsLoaded() {
        try {
            if (PlantFactory.getDefinition("Peashooter") == null) {
                PlantFactory.init("/assets/data/plants/plants.json");
            }
        } catch (Exception e) {
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
            } catch (IOException ignored) {}
        }

        try {
            if (ZombieFactory.getDefinition("ZombieDefault") == null) {
                ZombieFactory.init("/assets/data/zombies/zombies.json", "/assets/data/armor/ArmorTypeData.json");
            }
        } catch (Exception e) {
            try {
                ZombieFactory.init("/assets/data/zombies/zombies.json", "/assets/data/armor/ArmorTypeData.json");
            } catch (IOException ignored) {}
        }

        try {
            if (MiniGameRegistry.getInstance() == null) {
                MiniGameRegistry.init("/assets/data/minigames/minigames.json");
            }
        } catch (Exception e) {
            try {
                MiniGameRegistry.init("/assets/data/minigames/minigames.json");
            } catch (IOException ignored) {}
        }
    }

    private static IZombieLevel createDefaultIZombieLevel() {
        IZombieLevel fallback = new IZombieLevel(null, MiniGameType.I_ZOMBIE, 1);
        IZombieSettings settings = new IZombieSettings();
        settings.setRedLineColumn(3);
        settings.addPlaceableZombie("ZombieImp", 25);
        settings.addPlaceableZombie("ZombieDefault", 50);
        settings.addPlaceableZombie("ZombieArmor1", 75);
        settings.addPlaceableZombie("ZombieNewspaper", 100);
        settings.addPlaceableZombie("ZombieArmor2", 125);
        fallback.setSettings(settings);
        return fallback;
    }

    // --- Getters ---

    public String getRoomId() {
        return roomId;
    }

    public ClientConnectionHandler getPlantPlayer() {
        return plantPlayer;
    }

    public ClientConnectionHandler getZombiePlayer() {
        return zombiePlayer;
    }

    public GameModel getGameModel() {
        return gameModel;
    }

    public PvZGameLoop getGameLoop() {
        return gameLoop;
    }

    public int getPlantSun() {
        return plantSun;
    }

    public void setPlantSun(int plantSun) {
        this.plantSun = plantSun;
    }

    public int getZombieSun() {
        return zombieSun;
    }

    public void setZombieSun(int zombieSun) {
        this.zombieSun = zombieSun;
    }

    public float getMatchTime() {
        return matchTime;
    }

    public long getTickCounter() {
        return tickCounter;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public String getWinnerRole() {
        return winnerRole;
    }

    public String getEndReason() {
        return endReason;
    }

    public int getRedLineColumn() {
        return redLineColumn;
    }

    public float getMatchDuration() {
        return matchDuration;
    }

    public void setMatchDuration(float matchDuration) {
        this.matchDuration = matchDuration;
    }
}
