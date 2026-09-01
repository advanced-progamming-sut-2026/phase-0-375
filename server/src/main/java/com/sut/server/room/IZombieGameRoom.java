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

        // 1. Instantiate level & settings
        IZombieLevel createdLevel = null;
        try {
            if (MiniGameRegistry.getInstance() != null) {
                createdLevel = (IZombieLevel) MiniGameRegistry.getInstance().createMiniGame(MiniGameType.I_ZOMBIE, 1);
            }
        } catch (Exception e) {
            System.err.println("[IZombieGameRoom] Could not create level from MiniGameRegistry: " + e.getMessage());
        }
        if (createdLevel == null) {
            // Fallback manual level configuration
            createdLevel = createDefaultIZombieLevel();
        }
        this.level = createdLevel;
        this.redLineColumn = level.redLineColumn();

        // 2. Initialize GameModel and setup board
        this.gameModel = new GameModel(level);
        this.level.onStart(gameModel);

        // 3. Initialize Headless PvZGameLoop
        this.gameLoop = new PvZGameLoop(gameModel);

        // 4. Initial resource setup
        this.plantSun = DEFAULT_INITIAL_SUN;
        this.zombieSun = gameModel.getSunAmount() > 0 ? gameModel.getSunAmount() : DEFAULT_INITIAL_SUN;
        if (gameModel.getSunAmount() != zombieSun) {
            gameModel.addSun(zombieSun - gameModel.getSunAmount());
        }

        // 5. Attach room ID context to client connection handlers
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
        // Game may have ended outside this tick (surrender / external endGame).
        // Always unregister so players are no longer treated as in-match/busy.
        if (gameOver) {
            stop();
            return;
        }

        try {
            tickCounter++;

            // 1. Drain and execute pending action runnables
            Runnable action;
            while ((action = pendingActions.poll()) != null) {
                try {
                    action.run();
                } catch (Exception e) {
                    System.err.println("[IZombieGameRoom] Error executing player action runnable: " + e.getMessage());
                }
            }

            // 2. Decrement active plant card cooldowns
            for (Map.Entry<String, Float> entry : plantCardCooldowns.entrySet()) {
                float remaining = entry.getValue() - TICK_INTERVAL_SECONDS;
                if (remaining <= 0f) {
                    plantCardCooldowns.remove(entry.getKey());
                } else {
                    entry.setValue(remaining);
                }
            }

            // 3. Advance headless physics and combat simulation
            gameLoop.update(TICK_INTERVAL_SECONDS);
            matchTime += TICK_INTERVAL_SECONDS;
            zombieSun = gameModel.getSunAmount();

            // 5. Evaluate win / loss triggers
            checkWinLossConditions();

            // 6. Broadcast authoritative state snapshot
            broadcastSnapshot();

            // 7. Cleanup on game over
            if (gameOver) {
                stop();
            }

        } catch (Exception e) {
            System.err.println("[IZombieGameRoom] Exception during room tick in " + roomId + ": " + e.getMessage());
            e.printStackTrace();
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

        String sunZombieName = level != null && level.getSettings() != null ? level.getSettings().getSunZombie() : "ZombieIZombieSun";

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
        if (packet == null) return;

        if (sender != plantPlayer) {
            sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_PLANT", "UNAUTHORIZED_ROLE", packet.getRow(), packet.getCol()));
            return;
        }

        pendingActions.offer(() -> {
            int row = packet.getRow();
            int col = packet.getCol();

            if (gameOver) {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_PLANT", "GAME_OVER", row, col));
                return;
            }

            int maxRows = gameModel.getGameMap() != null ? gameModel.getGameMap().getRows() : 5;
            if (row < 0 || row >= maxRows || col < 0 || col >= redLineColumn) {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_PLANT", "BEYOND_RED_LINE", row, col));
                return;
            }

            String plantName = packet.getPlantName();
            Plant def = PlantFactory.getDefinition(plantName);
            if (def == null) {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_PLANT", "INVALID_PLANT_NAME", row, col));
                return;
            }

            if (plantSun < def.getCost()) {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_PLANT", "INSUFFICIENT_SUN", row, col));
                return;
            }

            if (plantCardCooldowns.containsKey(def.getName())) {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_PLANT", "COOLDOWN_ACTIVE", row, col));
                return;
            }

            Cell cell = gameModel.getGameMap().getCell(col, row);
            if (cell == null || cell.getPlaceable(PlacableLayer.MAIN) != null) {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_PLANT", "CELL_OCCUPIED", row, col));
                return;
            }

            PlantInstance instance = PlantFactory.createInstance(def);
            if (instance == null) {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_PLANT", "CREATION_FAILED", row, col));
                return;
            }

            boolean placed = gameModel.placePlant(instance, row, col);
            if (placed) {
                plantSun -= def.getCost();
                float cd = def.getRechargeTime() > 0 ? def.getRechargeTime() : 5.0f;
                plantCardCooldowns.put(def.getName(), cd);
                sender.sendPacket(new PlayerActionResponsePacket(true, "PLACE_PLANT", "OK", row, col));
            } else {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_PLANT", "PLACEMENT_REJECTED", row, col));
            }
        });
    }

    /**
     * Handles zombie spawn requests from the zombie attacker.
     */
    public void handleZombieAction(ClientConnectionHandler sender, PlaceZombieRequestPacket packet) {
        if (packet == null) return;

        if (sender != zombiePlayer) {
            sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_ZOMBIE", "UNAUTHORIZED_ROLE", packet.getRow(), packet.getCol()));
            return;
        }

        pendingActions.offer(() -> {
            int row = packet.getRow();
            int col = packet.getCol();

            if (gameOver) {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_ZOMBIE", "GAME_OVER", row, col));
                return;
            }

            int maxRows = gameModel.getGameMap() != null ? gameModel.getGameMap().getRows() : 5;
            int maxCols = gameModel.getGameMap() != null ? gameModel.getGameMap().getCols() : 9;

            if (row < 0 || row >= maxRows || col < redLineColumn || col >= maxCols) {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_ZOMBIE", "BEHIND_RED_LINE", row, col));
                return;
            }

            String zombieName = packet.getZombieName();
            Zombie def = ZombieFactory.getDefinition(zombieName);
            if (def == null) {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_ZOMBIE", "INVALID_ZOMBIE_NAME", row, col));
                return;
            }

            int cost = getZombieCost(def.getName());
            if (gameModel.getSunAmount() < cost) {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_ZOMBIE", "INSUFFICIENT_SUN", row, col));
                return;
            }

            boolean spent = gameModel.spendSun(cost);
            if (!spent) {
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_ZOMBIE", "INSUFFICIENT_SUN", row, col));
                return;
            }
            zombieSun = gameModel.getSunAmount();

            ZombieInstance spawned = gameModel.spawnZombieAt(def.getName(), row, col);
            if (spawned != null) {
                sender.sendPacket(new PlayerActionResponsePacket(true, "PLACE_ZOMBIE", "OK", row, col));
            } else {
                gameModel.addSun(cost); // Refund
                zombieSun = gameModel.getSunAmount();
                sender.sendPacket(new PlayerActionResponsePacket(false, "PLACE_ZOMBIE", "SPAWN_FAILED", row, col));
            }
        });
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
        List<PlantSnapshotDto> plantDtos = new ArrayList<>();
        List<ZombieSnapshotDto> zombieDtos = new ArrayList<>();
        List<ProjectileSnapshotDto> projectileDtos = new ArrayList<>();

        // Map plants
        for (PlantInstance plant : gameModel.getAllPlants()) {
            if (plant == null) continue;
            String id = getEntityId(plant);
            int row = plant.getPosition() != null ? plant.getPosition().getY() : 0;
            int col = plant.getPosition() != null ? plant.getPosition().getX() : 0;
            int maxHp = plant.getDefinition() != null ? plant.getDefinition().getBaseHP() : plant.getCurrentHP();
            String state = plant.getState() != null ? plant.getState().name() : "IDLE";
            plantDtos.add(new PlantSnapshotDto(
                    id,
                    plant.getDefinition() != null ? plant.getDefinition().getName() : "Unknown",
                    row,
                    col,
                    plant.getCurrentHP(),
                    maxHp,
                    state,
                    plant.isPlantFoodActive(),
                    plant.isFrozen(),
                    plant.getStackCount()
            ));
        }

        // Map zombies
        for (ZombieInstance zombie : gameModel.getActiveZombies()) {
            if (zombie == null) continue;
            String id = getEntityId(zombie);
            int row = zombie.getGridY();
            float x = zombie.getContinuousPosition() != null ? zombie.getContinuousPosition().getX() : (float) zombie.getGridPosition().getX();
            float y = zombie.getContinuousPosition() != null ? zombie.getContinuousPosition().getY() : (float) zombie.getGridPosition().getY();
            int maxHp = zombie.getDefinition() != null ? zombie.getDefinition().getBaseHP() : zombie.getCurrentHP();
            int armorHp = 0;
            if (zombie.getArmors() != null) {
                for (Armor a : zombie.getArmors()) {
                    if (a != null) armorHp += a.getCurrentHealth();
                }
            }
            String state = zombie.getState() != null ? zombie.getState().name() : "WALKING";
            zombieDtos.add(new ZombieSnapshotDto(
                    id,
                    zombie.getDefinition() != null ? zombie.getDefinition().getName() : "Unknown",
                    row,
                    x,
                    y,
                    zombie.getCurrentHP(),
                    maxHp,
                    armorHp,
                    state,
                    zombie.getCurrentSpeed(),
                    zombie.isChilled(),
                    zombie.isFrozen(),
                    zombie.isButtered(),
                    zombie.isHypnotized()
            ));
        }

        // Map projectiles
        for (Projectile projectile : gameModel.getActiveProjectiles()) {
            if (projectile == null) continue;
            String id = getEntityId(projectile);
            projectileDtos.add(new ProjectileSnapshotDto(
                    id,
                    projectile.getClass().getSimpleName(),
                    projectile.getRow(),
                    projectile.getX(),
                    projectile.getY(),
                    projectile.getVelocity() * projectile.getDirection(),
                    projectile.getElement() != null ? projectile.getElement().name() : "NONE"
            ));
        }

        List<Integer> breached = new ArrayList<>(gameModel.getBreachedRows());
        float timeRemaining = Math.max(0f, matchDuration - matchTime);

        List<SunSnapshotDto> sunDtos = new ArrayList<>();
        for (Sun sun : gameModel.getActiveSuns()) {
            if (sun == null) {
                continue;
            }
            sunDtos.add(new SunSnapshotDto(
                    sun.getX(),
                    sun.getY(),
                    sun.getValue(),
                    sun.getType() != null ? sun.getType().name() : "NORMAL",
                    sun.getOffsetX(),
                    sun.getOffsetY(),
                    sun.getFallRemaining(),
                    sun.getFallDuration(),
                    sun.hasOrigin(),
                    sun.getOriginX(),
                    sun.getOriginY()
            ));
        }

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

    /**
     * Stops the room execution and unregisters it from RoomManager.
     */
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
