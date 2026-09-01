package model.game.core;

import model.app.App;
import model.enums.Chapter;
import model.enums.GameState;
import model.enums.GroundType;
import model.enums.PlacableLayer;
import model.enums.PlantCategory;
import model.enums.ZombieState;
import model.event.EventBus;
import model.event.GameEvent;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.map.Cell;
import model.game.map.FloatPoint;
import model.game.map.GameMap;
import model.game.map.Lane;
import model.game.map.TideState;
import model.game.map.WaterBand;
import model.game.map.terrain.CraterTerrainStrategy;
import model.game.map.terrain.IceTerrainStrategy;
import model.game.map.terrain.TerrainStrategy;
import model.game.systems.TerrainSystem;
import model.game.map.Point;
import model.game.rule.EndGameCondition;
import model.game.score.MyopointTracker;
import model.game.wave.Wave;
import model.game.wave.WaveManager;
import model.game.systems.ChapterEffectsSystem;
import model.item.Grave;
import model.item.Grave.GraveType;
import model.item.LootDrop;
import model.item.LootPickup;
import model.item.PlantFoodPickup;
import model.item.Sun;
import model.item.pushable.Pushable;
import model.item.placeable.Placeable;
import model.plant.ability.PlantAbility;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.news.NewsFactory;
import model.user.User;
import model.zombie.ZombieFactory;
import model.zombie.behavior.BehaviorContext;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameModel implements BehaviorContext {
    private long currentTick;
    private final ResourceBank resources; // sun / plant-food economy (composition)
    private int difficultyLevel;
    private GameState gameState;
    private Chapter chapter;

    /** Per-plant-type seed packet cooldowns (seconds remaining). Empty = all seeds ready. */
    private final Map<String, Float> seedCooldowns = new HashMap<>();
    /** Set by `cheat remove-cooldown` — disables seed cooldowns for the rest of the level. */
    private boolean seedCooldownsDisabled = false;

    private Level currentLevel;
    private WaveManager waveManager;

    private EndGameCondition endGameCondition;

    private GameMap gameMap;
    private List<ZombieInstance> activeZombies;
    private List<Projectile> activeProjectiles;
    private final List<Projectile> projectileHitCues;
    private List<Sun> activeSuns;
    private List<PlantFoodPickup> activePlantFood;
    private List<LootPickup> activeLootPickups;
    private List<LootDrop> pendingLootDrops;
    private List<Pushable> orphanedPushables;

    private EventBus eventBus;
    private List<String> selectedPlants;       // plant types chosen for this level
    private String imitaterCopyTarget; // Plant Imitater should morph into; last non-Imitater the player picked or planted

    /** Continuous X past the lawn left edge where a breacher stands and chews. */
    public static final float HOUSE_CHEW_X = -0.9f;

    // End-game bookkeeping (read by EndGameCondition implementations)
    private boolean houseBreached;
    private final Set<Integer> breachedRows = new HashSet<>();
    /** Zombie that walked into the house (lose spotlight); null for non-breach losses. */
    private ZombieInstance breachingZombie;
    /** Continuous X of the most recent zombie death. */
    private float lastZombieDeathX = Float.NaN;
    /** Row of the most recent zombie death. */
    private float lastZombieDeathY = Float.NaN;
    private int zombiesKilled;
    private int plantsLost;
    private float elapsedSeconds;

    // Optional scorer attached by the daily Myopoint score level
    private MyopointTracker myopointTracker;

    // Chapter-specific ambient effects (tornado, ice wind, tide)
    private ChapterEffectsSystem chapterEffects;

    /** Big Wave Beach tide / static sea; inactive on other chapters. */
    private final TideState tideState;

    // Per-level stats used for quest tracking (extracted component)
    private final LevelQuestStats questStats = new LevelQuestStats();

    /** Center-screen stings (wave / necromancy / low tide); GUI drains FIFO. */
    private final ArrayDeque<String> pendingAnnouncements = new ArrayDeque<>();

    /** Egypt sandstorms in flight; each lands its zombie at touchdown. */
    private final List<SandstormSpawn> pendingSandstorms = new ArrayList<>();
    /** Read-only view of {@link #pendingSandstorms} for the renderer. */
    private final List<SandstormSpawn> sandstormsView =
            Collections.unmodifiableList(pendingSandstorms);

    /** Frostbite Caves ice winds sweeping hit lanes; presentation-only. */
    private final List<IceWindGust> iceWinds = new ArrayList<>();
    /** Read-only view of {@link #iceWinds} for the renderer. */
    private final List<IceWindGust> iceWindsView =
            Collections.unmodifiableList(iceWinds);

    // Loot economy (diamonds / coins / flower pots dropped by zombie kills)
    private int diamondCount;
    private int coinCount;
    private int flowerPotCount;

    public GameModel(Level currentLevel) {
        this.currentTick = 0;
        User currentUser = App.getInstance().getCurrentUser();
        this.difficultyLevel = currentUser != null ? currentUser.getDifficultyLevel() : 3;
        this.gameState = GameState.RUNNING;

        this.currentLevel = currentLevel;
        LevelConfig levelConfig = this.currentLevel.getConfig();
        // load plant food bought from the shop (stored on the user profile)
        User pfOwner = App.getInstance().getCurrentUser();
        this.resources = new ResourceBank(levelConfig.getRules().getInitialSun(),
                pfOwner != null ? pfOwner.getPlantFoodCount() : 0);
        this.chapter = levelConfig.getChapter();
        this.endGameCondition = levelConfig.getEndGameCondition();

        this.activeZombies = new ArrayList<>();
        this.activeProjectiles = new ArrayList<>();
        this.projectileHitCues = new ArrayList<>();
        this.activeSuns = new ArrayList<>();
        this.activePlantFood = new ArrayList<>();
        this.activeLootPickups = new ArrayList<>();
        this.pendingLootDrops = new ArrayList<>();
        this.orphanedPushables = new ArrayList<>();

        this.gameMap = new GameMap(levelConfig.getRows(), levelConfig.getColumns());

        this.tideState = TideState.fromConfig(levelConfig);

        this.waveManager = new WaveManager(levelConfig.getWaves(), this);

        this.chapterEffects = new ChapterEffectsSystem(this);

        this.eventBus = new EventBus() {
            @Override
            public void dispatch(GameEvent event) {

            }
        };
    }

    public GameMap getMap() {
        return gameMap;
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    public long getTick() {
        return currentTick;
    }

    @Override
    public int getSunAmount() {
        return resources.getSunAmount();
    }

    public int getPlantFoodCount() {
        return resources.getPlantFoodCount();
    }

    public int getDifficulty() {
        return difficultyLevel;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public float difficultyBoost() {
        return Math.max(1, difficultyLevel) / 3.0f;
    }

    public float difficultyPenalty() {
        return 3.0f / difficultyLevel;
    }

    /** Whether the settings menu asked to draw the lawn grid during gameplay. */
    public boolean isShowLawnGrid() {
        User user = App.getInstance().getCurrentUser();
        return user != null && user.isShowLawnGrid();
    }

    /** Whether in-game debug cheat controls should be shown. */
    public boolean isDebugMode() {
        User user = App.getInstance().getCurrentUser();
        return user != null && user.isDebugMode();
    }

    // Seed packet cooldowns

    public boolean isSeedReady(String plantName) {
        if (seedCooldownsDisabled) return true;
        Float remaining = seedCooldowns.get(plantName);
        return remaining == null || remaining <= 0f;
    }

    public float getSeedCooldown(String plantName) {
        if (seedCooldownsDisabled) return 0f;
        Float remaining = seedCooldowns.get(plantName);
        return remaining == null ? 0f : remaining;
    }

    public void startSeedRecharge(String plantName, float seconds) {
        if (seedCooldownsDisabled || seconds <= 0f) return;
        seedCooldowns.put(plantName, seconds);
    }

    public void disableSeedCooldowns() {
        seedCooldownsDisabled = true;
        seedCooldowns.clear();
    }

    public Level getCurrentLevel() { return currentLevel; }

    /** Attaches the optional Myopoint scorer (set by the daily score level). */
    public void setMyopointTracker(MyopointTracker tracker) { this.myopointTracker = tracker; }

    public MyopointTracker getMyopointTracker() { return myopointTracker; }

    public GameState getState() {
        return gameState;
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public List<ZombieInstance> getZombies() {
        return activeZombies;
    }

    public List<ZombieInstance> getActiveZombies() {
        return activeZombies;
    }

    public List<Projectile> getProjectiles() {
        return activeProjectiles;
    }

    public List<Projectile> getActiveProjectiles() {
        return activeProjectiles;
    }

    /** Records a projectile impact for one-shot splat / hit PAM playback. */
    public void recordProjectileHit(Projectile projectile) {
        if (projectile != null) {
            projectileHitCues.add(projectile);
        }
    }

    /**
     * Projectile impacts since the last drain. The lawn renderer consumes this
     * each frame; empty when nothing hit.
     */
    public List<Projectile> drainProjectileHits() {
        if (projectileHitCues.isEmpty()) {
            return List.of();
        }
        List<Projectile> drained = new ArrayList<>(projectileHitCues);
        projectileHitCues.clear();
        return drained;
    }

    /** Drops cues the view never consumed (TUI, skipped frames). */
    public void discardUnreadProjectileHits() {
        projectileHitCues.clear();
    }

    private final List<Point> radioactiveSunExplosionCues = new ArrayList<>();

    public void recordRadioactiveSunExplosion(int col, int row) {
        radioactiveSunExplosionCues.add(new Point(col, row));
    }

    public List<Point> drainRadioactiveSunExplosions() {
        if (radioactiveSunExplosionCues.isEmpty()) {
            return List.of();
        }
        List<Point> drained = new ArrayList<>(radioactiveSunExplosionCues);
        radioactiveSunExplosionCues.clear();
        return drained;
    }

    /** Slide-tile activations since the last drain, as {@code (col, row)} points. */
    private final List<Point> slideStartCues = new ArrayList<>();

    /** A slide waiting for its zombie to reach the slide tile's middle. */
    public static final class ArmedSlide {
        private final int tileColumn;
        private final int fromRow;
        private final int toRow;

        ArmedSlide(int tileColumn, int fromRow, int toRow) {
            this.tileColumn = tileColumn;
            this.fromRow = fromRow;
            this.toRow = toRow;
        }

        /** Column of the slide tile; its middle sits at this continuous X. */
        public int getTileColumn() { return tileColumn; }
        /** Lane the zombie slides from. */
        public int getFromRow() { return fromRow; }
        /** Lane the zombie slides into. */
        public int getToRow() { return toRow; }
    }

    /** Armed slides keyed by their zombie. */
    private final Map<ZombieInstance, ArmedSlide> armedSlides = new HashMap<>();

    /** Slides still gliding between lanes; presentation-only. */
    private final List<LaneSlide> laneSlides = new ArrayList<>();
    /** Read-only view of {@link #laneSlides} for the renderer. */
    private final List<LaneSlide> laneSlidesView =
            Collections.unmodifiableList(laneSlides);

    /** Low-tide ambushes still surfacing; presentation-only. */
    private final List<WaterEmerge> waterEmerges = new ArrayList<>();
    /** Read-only view of {@link #waterEmerges} for the renderer. */
    private final List<WaterEmerge> waterEmergesView =
            Collections.unmodifiableList(waterEmerges);

    /**
     * Marks a freshly spawned ambush zombie as surfacing from the shallow
     * water; the view plays the Snorkel-style mask + ripple while it rises.
     */
    public void beginWaterEmerge(ZombieInstance zombie) {
        if (zombie != null) {
            waterEmerges.add(new WaterEmerge(zombie));
        }
    }

    /** In-flight water emergences (read-only) for the view layer. */
    public List<WaterEmerge> getWaterEmerges() {
        return waterEmergesView;
    }

    /** True while a low-tide ambush zombie is still surfacing. */
    public boolean isWaterEmerging(ZombieInstance zombie) {
        if (zombie == null || waterEmerges.isEmpty()) {
            return false;
        }
        for (WaterEmerge emerge : waterEmerges) {
            if (emerge.getZombie() == zombie) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void armLaneSlide(ZombieInstance zombie, Cell slideTile, int toRow) {
        if (zombie == null || slideTile == null) {
            return;
        }
        armedSlides.put(zombie,
                new ArmedSlide(slideTile.getColumn(), slideTile.getRow(), toRow));
    }

    /**
     * Fires {@code zombie}'s armed slide once it reaches the slide tile's
     * middle. Zombies walk left, and integer continuous X is a tile centre,
     * so the midpoint is crossed at {@code continuousX <= tileColumn}.
     *
     * @return true exactly when the slide fired this tick
     */
    public boolean tickArmedSlide(ZombieInstance zombie, float continuousX) {
        if (zombie == null || zombie.isMovingBackward()) {
            return false;
        }
        ArmedSlide armed = armedSlides.get(zombie);
        if (armed == null || continuousX > armed.getTileColumn()) {
            return false;
        }
        armedSlides.remove(zombie);
        // Logical relocation happens here; the LaneSlide record below only
        // drives the visual glide between the two lanes.
        moveZombieToLane(zombie, armed.getToRow());
        if (armed.getFromRow() != armed.getToRow()) {
            laneSlides.add(new LaneSlide(zombie, armed.getFromRow(), armed.getToRow()));
        }
        slideStartCues.add(new Point(armed.getTileColumn(), armed.getFromRow()));
        return true;
    }

    /**
     * Slide activations since the last drain. The lawn renderer consumes this
     * each frame to play the slider active_start / active_end clips; empty
     * when nothing slid.
     */
    public List<Point> drainSlideStarts() {
        if (slideStartCues.isEmpty()) {
            return List.of();
        }
        List<Point> drained = new ArrayList<>(slideStartCues);
        slideStartCues.clear();
        return drained;
    }

    /** Drops cues the view never consumed (TUI, skipped frames). */
    public void discardUnreadSlideStarts() {
        slideStartCues.clear();
    }

    /** In-flight slide glides (read-only) for the view layer. */
    public List<LaneSlide> getLaneSlides() {
        return laneSlidesView;
    }

    /**
     * Width of the dynamic tide band (columns from the right edge the water
     * may flood); {@code 0} outside Big Wave Beach levels.
     */
    public int getTideLimitColumns() {
        LevelConfig config = currentLevel != null ? currentLevel.getConfig() : null;
        return config != null ? Math.max(0, config.getTideLimitColumn()) : 0;
    }

    /**
     * Columns currently flooded by the dynamic tide, counting from the right
     * edge; {@code 0} when the tide is fully out. Static sea is not counted.
     */
    public int getTideColumns() {
        return tideState.getDynamicColumns();
    }

    /** Beach tide state; inactive ({@link TideState#isActive()} false) elsewhere. */
    public TideState getTideState() {
        return tideState;
    }

    /**
     * Rightmost flooded column count for the wave PAM: dynamic tide width with
     * a floor at permanent {@code waterTiles}.
     */
    public int getFloodedColumns() {
        if (gameMap == null) {
            return 0;
        }
        if (tideState.isActive()) {
            return tideState.floodedColumns(gameMap.getCols(), gameMap.getRows());
        }
        return WaterBand.columnsFromRight(gameMap);
    }

    @Override
    public List<Sun> getActiveSuns() {
        return activeSuns;
    }

    public List<PlantFoodPickup> getActivePlantFood() {
        return activePlantFood;
    }

    public List<LootPickup> getActiveLootPickups() {
        return activeLootPickups;
    }

    public boolean isNightLevel() {
        return chapter == Chapter.DARK_AGES;
    }

    public void addDiamonds(int amount) {
        if (amount > 0) {
            diamondCount += amount;
            model.user.persistance.UserSync.addGems(amount);
        }
    }

    public void addCoins(int amount) {
        if (amount > 0) {
            coinCount += amount;
            model.user.persistance.UserSync.addCoins(amount);
        }
    }

    public void addFlowerPots(int amount) {
        if (amount > 0) {
            flowerPotCount += amount;
            User user = App.getInstance().getCurrentUser();
            if (user != null && App.getInstance().getUserRepository() != null) {
                // Unlock next pots on the server one-by-one (x,y from greenhouse layout).
                for (int i = 0; i < amount; i++) {
                    int potIndex = user.getUnlockedPots() + i;
                    int x = potIndex % 4;
                    int y = potIndex / 4;
                    App.getInstance().getUserRepository().unlockGreenhousePot(user.getUsername(), x, y);
                }
            }
        }
    }

    public int getDiamondCount() { return diamondCount; }

    public int getCoinCount() { return coinCount; }

    public int getFlowerPotCount() { return flowerPotCount; }

    /**
     * @return true if the cell at {@code (row, col)} is a water tile.
     */
    public boolean isWaterTile(int row, int col) {
        Cell cell = getCellAt(row, col);
        return cell != null && (cell.getGroundType() == GroundType.WATER
                || cell.getGroundType() == GroundType.LOW_TIDE);
    }

    /**
     * Places an already-constructed plant instance onto the field at the
     * given grid cell.
     *
     * @return true if the plant was placed; false if the
     *         cell is out of bounds, the target layer is already
     *         occupied, or the cell's terrain strategy rejects the plant
     */
    public boolean placePlant(PlantInstance plant, int row, int col) {
        if (plant == null) return false;
        if (row < 0 || col < 0 || row >= gameMap.getRows() || col >= gameMap.getCols()) {
            return false;
        }
        Cell cell = gameMap.getCell(col, row);
        PlacableLayer targetLayer = plant.getLayer();
        if (cell.getPlaceable(targetLayer) != null) {
            return false;
        }
        if (targetLayer == PlacableLayer.MAIN) {
            TerrainStrategy terrain = cell.getTerrainStrategy();
            if (terrain != null && !terrain.canPlant(plant.getDefinition(), cell)) {
                return false;
            }
        }
        plant.setPosition(new Point(col, row));
        boolean added = cell.addPlaceable(plant);
        if (added) {
            questStats.onPlantPlaced(this, plant.getDefinition(), row, col);
        }
        if (added && eventBus != null) {
            eventBus.dispatch(new GameEvent(GameEvent.Type.PLANT_PLACED));
        }
        return added;
    }

    @Override
    public void addSun(int amount) {
        resources.addSun(amount);
    }

    /** Absolute sun balance (used by networked display sync). */
    public void setSunAmount(int amount) {
        resources.setSunAmount(amount);
    }

    @Override
    public boolean spendSun(int amount) {
        return resources.spendSun(amount);
    }

    public void addPlantFood() {
        resources.addPlantFood();
    }

    public boolean usePlantFood() {
        return resources.usePlantFood();
    }

    /** Records a zombie type as seen for the collection; saves only on first sighting. */
    public void recordZombieSeen(String zombieName) {
        questStats.onZombieSpawned(elapsedSeconds);
        User user = App.getInstance().getCurrentUser();
        if (user == null || zombieName == null) {
            return;
        }
        Set<String> seen = user.getUnlockedZombies();
        if (seen == null) {
            seen = new HashSet<>();
            user.setUnlockedZombies(seen);
        }
        if (seen.add(zombieName)) {
            user.rememberNewsPublishDate(NewsFactory.zombieNewsId(zombieName));
            if (App.getInstance().getUserRepository() != null) {
                App.getInstance().getUserRepository().unlockZombie(user.getUsername(), zombieName);
            }
        }
    }

    public void spawnZombie(Zombie zombie, int lane) {
        ZombieInstance instance = ZombieFactory.createInstance(zombie);
        instance.setCurrentHP(Math.max(1, (int) (instance.getCurrentHP() * difficultyBoost())));
        recordZombieSeen(zombie.getName());
        instance.setContinuousPosition(new FloatPoint(gameMap.getCols(), lane));
        instance.setGridPosition(new Point(gameMap.getCols(), lane));
        activeZombies.add(instance);
        gameMap.addZombie(instance, gameMap.getCols(), lane);
        if (myopointTracker != null) {
            myopointTracker.onZombieSpawned(instance, elapsedSeconds);
        }
        if (waveManager != null) {
            waveManager.onWaveZombieSpawned(instance);
        }
        eventBus.dispatch(new GameEvent(GameEvent.Type.ZOMBIE_SPAWNED));
    }

    /**
     * Ancient Egypt tornado entry (final wave): the zombie is carried in by a
     * sandstorm and touches down 1-4 columns ahead of the normal entry edge.
     *
     * @return the landed instance, so sandstorm records can hide it behind
     *         the outro fade
     */
    public ZombieInstance spawnZombieWithTornado(Zombie zombie, int lane, int columnsAhead) {
        ZombieInstance instance = ZombieFactory.createInstance(zombie);
        instance.setCurrentHP(Math.max(1, (int) (instance.getCurrentHP() * difficultyBoost())));
        recordZombieSeen(zombie.getName());
        int col = tornadoColumn(gameMap.getCols(), columnsAhead);
        instance.setContinuousPosition(new FloatPoint(col, lane));
        instance.setGridPosition(new Point(col, lane));
        activeZombies.add(instance);
        gameMap.addZombie(instance, col, lane);
        if (myopointTracker != null) {
            myopointTracker.onZombieSpawned(instance, elapsedSeconds);
        }
        if (waveManager != null) {
            waveManager.onWaveZombieSpawned(instance);
        }
        App.logToShell("[Sandstorm] A " + zombie.getName()
                + " is carried in by a sandstorm and lands " + columnsAhead
                + " column(s) ahead in lane " + (lane + 1) + "!");
        eventBus.dispatch(new GameEvent(GameEvent.Type.ZOMBIE_SPAWNED));
        return instance;
    }

    /** Touchdown column for a storm entry landing inside the right edge. */
    public static int tornadoColumn(int columnCount, int columnsAhead) {
        return Math.max(0, columnCount - Math.max(1, columnsAhead));
    }

    /**
     * Ancient Egypt sandstorm entry: queues a storm that carries
     * {@code zombie} in from off-screen right and spawns it
     * {@code columnsAhead} columns inside the normal entry edge when the
     * storm touches down.
     */
    public void queueSandstormSpawn(Zombie zombie, int lane, int columnsAhead) {
        pendingSandstorms.add(new SandstormSpawn(this, zombie, lane, columnsAhead));
    }

    /** In-flight sandstorms (read-only) for the view layer. */
    public List<SandstormSpawn> getSandstorms() {
        return sandstormsView;
    }

    /**
     * Frostbite Caves ice wind: queues a gust visual sweeping {@code lane}
     * (the frost damage itself is applied by the chapter effects system).
     */
    public void queueIceWindGust(int lane) {
        if (lane >= 0) {
            iceWinds.add(new IceWindGust(lane));
        }
    }

    /** Active ice winds (read-only) for the view layer. */
    public List<IceWindGust> getIceWinds() {
        return iceWindsView;
    }

    /** Hook invoked by the wave manager when a new wave begins. */
    public void onWaveStarted(Wave wave) {
        announceWave(wave);
        if (chapterEffects != null) {
            chapterEffects.onWaveStarted(wave);
        }
    }

    /** Prints a banner so the player always knows a new wave has begun. */
    private void announceWave(Wave wave) {
        if (wave == null) {
            return;
        }
        String text = wave.isFinalWave()
            ? "The final wave has come."
            : "Wave " + wave.getWaveNumber() + " started.";
        enqueueAnnouncement(text);
        App.logToShell(text);
    }

    /** Queue a center-screen sting (wave / necromancy / low tide). */
    public void enqueueAnnouncement(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        pendingAnnouncements.addLast(text);
    }

    /** Next pending sting, or {@code null} if the queue is empty. */
    public String consumeWaveAnnouncement() {
        return pendingAnnouncements.pollFirst();
    }

    @Override
    public ZombieInstance spawnZombieAt(String zombieDefinitionName, int row, int col) {
        ZombieInstance instance = ZombieFactory.createInstance(zombieDefinitionName);
        if (instance == null) {
            return null;
        }
        instance.setCurrentHP(Math.max(1, (int) (instance.getCurrentHP() * difficultyBoost())));
        recordZombieSeen(instance.getDefinition() != null
                ? instance.getDefinition().getName() : zombieDefinitionName);

        int clampedRow = Math.max(0, Math.min(row, gameMap.getRows() - 1));
        int clampedCol = Math.max(0, Math.min(col, gameMap.getCols() - 1));

        instance.setGridPosition(new Point(clampedCol, clampedRow));
        instance.setContinuousPosition(new FloatPoint(clampedCol, clampedRow));
        instance.setState(ZombieState.SPAWNING);

        activeZombies.add(instance);
        gameMap.addZombie(instance, clampedCol, clampedRow);
        if (myopointTracker != null) {
            myopointTracker.onZombieSpawned(instance, elapsedSeconds);
        }
        eventBus.dispatch(new GameEvent(GameEvent.Type.ZOMBIE_SPAWNED));

        return instance;
    }

    public void removeZombie(ZombieInstance zombie) {
        activeZombies.remove(zombie);
        gameMap.removeZombie(zombie);
        if (waveManager != null) {
            waveManager.onZombieRemoved(zombie);
        }
    }

    @Override
    public void orphanPushable(Pushable pushable) {
        if (pushable == null || pushable.isDestroyed()) {
            return;
        }
        if (!orphanedPushables.contains(pushable)) {
            orphanedPushables.add(pushable);
        }
    }

    @Override
    public List<Pushable> getOrphanedPushables() {
        return orphanedPushables;
    }

    @Override
    public void removeOrphanedPushable(Pushable pushable) {
        orphanedPushables.remove(pushable);
    }

    public void spawnProjectile(Projectile projectile, int x, int y) {
        activeProjectiles.add(projectile);
        gameMap.addProjectile(projectile, x, y);
        eventBus.dispatch(new GameEvent(GameEvent.Type.PROJECTILE_FIRED));
    }

    @Override
    public void spawnProjectile(Projectile projectile) {
        if (projectile == null) {
            return;
        }
        spawnProjectile(projectile, (int) projectile.getX(), projectile.getRow());
    }

    @Override
    public void removeProjectile(Projectile projectile) {
        if (projectile == null) return;
        activeProjectiles.remove(projectile);
        gameMap.removeProjectile(projectile);
    }

    public void spawnSun(Sun sun) {
        activeSuns.add(sun);
    }

    public void collectSun(Sun sun) {
        activeSuns.remove(sun);
        resources.addSun(sun.getValue());
        questStats.onSunCollected(sun.getValue());
    }

    public void spawnPlantFood(PlantFoodPickup pickup) {
        if (pickup != null) {
            activePlantFood.add(pickup);
        }
    }

    public void collectPlantFood(PlantFoodPickup pickup) {
        if (pickup == null) {
            return;
        }
        activePlantFood.remove(pickup);
        resources.addPlantFood();
    }

    public void spawnLootPickup(LootPickup pickup) {
        if (pickup != null) {
            activeLootPickups.add(pickup);
        }
    }

    public void removeLootPickup(LootPickup pickup) {
        if (pickup != null) {
            activeLootPickups.remove(pickup);
        }
    }

    /** Credits loot counters after the fly-to-HUD animation finishes. */
    public void applyLootPickup(LootPickup pickup) {
        if (pickup == null) {
            return;
        }
        switch (pickup.getKind()) {
            case COIN_GOLD, COIN_SILVER -> addCoins(pickup.getAmount());
            case DIAMOND -> addDiamonds(pickup.getAmount());
            case FLOWER_POT -> addFlowerPots(pickup.getAmount());
        }
    }

    public void tick(float deltaTime) {
        currentTick += 1;
        elapsedSeconds += deltaTime;
        if (chapterEffects != null) {
            chapterEffects.tick(deltaTime);
        }
        tickSandstorms(deltaTime);
        tickIceWinds(deltaTime);
        tickLaneSlides(deltaTime);
        tickWaterEmerges(deltaTime);
        if (!seedCooldowns.isEmpty()) {
            Iterator<Map.Entry<String, Float>> it = seedCooldowns.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Float> e = it.next();
                float remaining = e.getValue() - deltaTime;
                if (remaining <= 0f) it.remove();
                else e.setValue(remaining);
            }
        }
    }

    /** Advances in-flight sandstorms, spawning each zombie at touchdown. */
    private void tickSandstorms(float deltaTime) {
        if (pendingSandstorms.isEmpty()) {
            return;
        }
        Iterator<SandstormSpawn> iterator = pendingSandstorms.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick(deltaTime)) {
                iterator.remove();
            }
        }
    }

    /** Expires finished ice-wind gusts. */
    private void tickIceWinds(float deltaTime) {
        if (iceWinds.isEmpty()) {
            return;
        }
        Iterator<IceWindGust> iterator = iceWinds.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick(deltaTime)) {
                iterator.remove();
            }
        }
    }

    /** Expires finished lane glides and drops arms of dead zombies. */
    private void tickLaneSlides(float deltaTime) {
        armedSlides.keySet().removeIf(ZombieInstance::isDead);
        if (laneSlides.isEmpty()) {
            return;
        }
        Iterator<LaneSlide> iterator = laneSlides.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick(deltaTime)) {
                iterator.remove();
            }
        }
    }

    /** Expires finished water emergences (and ones whose zombie is gone). */
    private void tickWaterEmerges(float deltaTime) {
        if (waterEmerges.isEmpty()) {
            return;
        }
        Iterator<WaterEmerge> iterator = waterEmerges.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick(deltaTime)) {
                iterator.remove();
            }
        }
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public EndGameCondition getEndGameCondition() {
        return endGameCondition;
    }

    /** True once a zombie has walked into the player's house. */
    public boolean isHouseBreached() {
        return houseBreached;
    }

    public void markHouseBreached() {
        this.houseBreached = true;
    }

    /** Marks a breach in a specific lane. In I, Zombie this is an eaten brain. */
    public void markHouseBreached(int row) {
        this.houseBreached = true;
        this.breachedRows.add(row);
    }

    /**
     * I, Zombie: destroys the brain in {@code row} after a chew finishes.
     * Does not pin a breaching zombie (those walk off the left afterward).
     */
    public void markBrainEaten(int row) {
        this.breachedRows.add(row);
    }

    /**
     * House breach: mark the lane lost and pin the zombie in an eat loop.
     * Leaves continuous X where it is (past the lawn edge into the house).
     */
    public void applyHouseBreach(ZombieInstance zombie, int row) {
        markHouseBreached(row);
        setBreachingZombie(zombie);
        if (zombie != null) {
            if (zombie.getContinuousPosition() == null) {
                zombie.setContinuousPosition(new FloatPoint(HOUSE_CHEW_X, row));
            }
            zombie.setState(ZombieState.EATING);
        }
    }

    /** Rows whose lane end has been breached at least once. */
    public Set<Integer> getBreachedRows() {
        return breachedRows;
    }

    /** Zombie chewing at the house after a breach, or {@code null}. */
    public ZombieInstance getBreachingZombie() {
        return breachingZombie;
    }

    public void setBreachingZombie(ZombieInstance zombie) {
        this.breachingZombie = zombie;
    }

    /** Continuous column of the last kill, or {@link Float#NaN} if none yet. */
    public float getLastZombieDeathX() {
        return lastZombieDeathX;
    }

    /** Lane row of the last kill, or {@link Float#NaN} if none yet. */
    public float getLastZombieDeathY() {
        return lastZombieDeathY;
    }

    public void recordLastZombieDeath(float continuousX, float row) {
        this.lastZombieDeathX = continuousX;
        this.lastZombieDeathY = row;
    }

    public int getZombiesKilled() {
        return zombiesKilled;
    }

    public void incrementZombiesKilled() {
        zombiesKilled++;
    }

    /** Notifies the optional Myopoint scorer that a zombie has just died. */
    public void notifyZombieKilledForScore(ZombieInstance zombie) {
        if (myopointTracker != null) {
            myopointTracker.onZombieKilled(zombie, elapsedSeconds, currentTick);
        }
    }

    /** Records a kill with timing/position details for quest tracking. */
    public void recordZombieKilled(ZombieInstance zombie) {
        zombiesKilled++;
        questStats.onZombieKilled(zombie, elapsedSeconds, gameMap);
    }

    public int getSunCollected() { return questStats.getSunCollected(); }

    public void markLawnMowerUsed() { questStats.markLawnMowerUsed(); }

    public boolean isLawnMowerUsed() { return questStats.isLawnMowerUsed(); }

    public int getKillsWithin30s() { return questStats.getKillsWithin30s(); }

    public int getNoMowerFirstColumnKills() { return questStats.getNoMowerFirstColumnKills(); }

    public int getMowerKills() { return questStats.getMowerKills(); }

    /** Kills where the zombie was damaged exclusively by the named plant. */
    public int getExclusivePlantKills(String plantName) { return questStats.getExclusivePlantKills(plantName); }

    /** Kills where the zombie was damaged exclusively by plants of the named family. */
    public int getExclusiveFamilyKills(String categoryName) { return questStats.getExclusiveFamilyKills(categoryName); }

    /** Raw exclusive-kill maps (diagnostics). */
    public Map<String, Integer> getExclusivePlantKillsMap() { return questStats.getExclusivePlantKillsMap(); }

    public Map<PlantCategory, Integer> getExclusiveFamilyKillsMap() { return questStats.getExclusiveFamilyKillsMap(); }

    public List<Plant> getPlantsPlaced() { return questStats.getPlantsPlaced(); }

    public int getMaxSunProducersAtOnce() { return questStats.getMaxSunProducersAtOnce(); }

    public Set<Integer> getRowsPlanted() { return questStats.getRowsPlanted(); }

    public Set<Integer> getColumnsPlanted() { return questStats.getColumnsPlanted(); }

    public Chapter getChapter() { return chapter; }

    /** Number of plants that have died this level. */
    public int getPlantsLost() {
        return plantsLost;
    }

    /** Seconds of simulated time since the level started. */
    public float getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void queueLootDrop(LootDrop loot) {
        if (loot != null) {
            pendingLootDrops.add(loot);
            if (eventBus != null) {
                eventBus.dispatch(new GameEvent(GameEvent.Type.LOOT_DROPPED));
            }
        }
    }

    public void processLootDrops() {
        if (pendingLootDrops.isEmpty()) return;
        Iterator<LootDrop> iterator = pendingLootDrops.iterator();
        while (iterator.hasNext()) {
            LootDrop drop = iterator.next();
            if (drop != null) {
                drop.apply(this);
            }
            iterator.remove();
        }
    }

    public List<String> getSelectedPlants() {
        return selectedPlants;
    }

    public void setSelectedPlants(List<String> selectedPlants) {
        this.selectedPlants = selectedPlants;
    }

    /** @return the plant Imitater is bound to copy, or {@code null} if unset. */
    public String getImitaterCopyTarget() {
        return imitaterCopyTarget;
    }

    /**
     * Remembers {@code plantName} as Imitater's copy target. Imitater and mint
     * names are ignored so picking Imitater itself does not overwrite the bind.
     */
    public void setImitaterCopyTarget(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return;
        }
        String lower = plantName.toLowerCase();
        if (lower.contains("imitat") || lower.contains("-mint")) {
            return;
        }
        this.imitaterCopyTarget = plantName;
    }

    public int getZombieCount() {
        return activeZombies.size();
    }

    // --- BehaviorContext implementation ---

    @Override
    public void removeSun(Sun sun) {
        activeSuns.remove(sun);
    }

    @Override
    public PlantInstance getPlantAt(int row, int col) {
        if (row < 0 || col < 0 || row >= gameMap.getRows() || col >= gameMap.getCols()) {
            return null;
        }
        return gameMap.getCell(col, row).getTopmostPlant();
    }

    public List<PlantInstance> getAllPlantsAt(int row, int col) {
        if (row < 0 || col < 0 || row >= gameMap.getRows() || col >= gameMap.getCols()) {
            return Collections.emptyList();
        }
        return gameMap.getCell(col, row).getAllPlants();
    }

    @Override
    public List<PlantInstance> getPlantsInLane(int lane) {
        if (lane < 0 || lane >= gameMap.getRows()) {
            return Collections.emptyList();
        }
        List<PlantInstance> plants = new ArrayList<>();
        for (int col = 0; col < gameMap.getCols(); col++) {
            plants.addAll(gameMap.getCell(col, lane).getAllPlants());
        }
        return plants;
    }

    @Override
    public List<PlantInstance> getAllPlants() {
        List<PlantInstance> plants = new ArrayList<>();
        for (int row = 0; row < gameMap.getRows(); row++) {
            for (int col = 0; col < gameMap.getCols(); col++) {
                plants.addAll(gameMap.getCell(col, row).getAllPlants());
            }
        }
        return plants;
    }

    @Override
    public void damagePlant(PlantInstance plant, int damage) {
        if (plant == null || damage <= 0) return;

        boolean wasAlive = plant.getCurrentHP() > 0;
        plant.takeDamage(damage);
        if (wasAlive && plant.getCurrentHP() == 0) {
            plantsLost++;
            hypnotiseHypnoShroomEaters(plant);
        }
    }

    @Override
    public boolean movePlant(PlantInstance plant, int row, int col) {
        if (plant == null || row < 0 || col < 0 || row >= gameMap.getRows() || col >= gameMap.getCols()) {
            return false;
        }

        Point currentPos = plant.getPosition();
        if (currentPos == null) {
            return false;
        }

        Cell destinationCell = gameMap.getCell(col, row);
        if (destinationCell.getPlaceable(plant.getLayer()) != null) {
            return false;
        }

        Cell sourceCell = gameMap.getCell(currentPos.getX(), currentPos.getY());
        sourceCell.removePlaceable(plant);
        destinationCell.addPlaceable(plant);
        plant.setPosition(new Point(col, row));
        questStats.markRowColumnPlanted(row, col);
        return true;
    }

    @Override
    public void destroyPlant(PlantInstance plant) {
        if (plant == null) return;

        if (plant.getCurrentHP() > 0) {
            plantsLost++;
        }
        hypnotiseHypnoShroomEaters(plant);
        plant.setCurrentHP(0);
        eventBus.dispatch(new GameEvent(GameEvent.Type.PLANT_DESTROYED));
    }

    /**
     * Hypno-shroom: the zombie that finished eating (or is standing on)
     * this plant joins the player's side.
     */
    private void hypnotiseHypnoShroomEaters(PlantInstance plant) {
        if (plant == null || !plant.isHypnoShroom() || activeZombies == null) {
            return;
        }
        Point pos = plant.getPosition();
        for (ZombieInstance zombie : activeZombies) {
            if (zombie == null || zombie.isDead()) continue;
            if (zombie.getEatingTarget() == plant) {
                zombie.hypnotise();
                continue;
            }
            if (pos != null
                    && zombie.getGridY() == pos.getY()
                    && zombie.getGridX() == pos.getX()
                    && zombie.isEating()) {
                zombie.hypnotise();
            }
        }
    }


    @Override
    public List<ZombieInstance> getZombiesInLane(int lane) {
        if (lane < 0 || lane >= gameMap.getRows()) {
            return Collections.emptyList();
        }
        List<ZombieInstance> zombies = new ArrayList<>();
        for (ZombieInstance zombie : activeZombies) {
            if (zombie.getGridPosition().getY() == lane && !zombie.isDead()) {
                zombies.add(zombie);
            }
        }
        return zombies;
    }

    @Override
    public List<ZombieInstance> getZombiesInArea(int centerRow, int centerCol, int rowRadius, int colRadius) {
        List<ZombieInstance> zombies = new ArrayList<>();
        for (ZombieInstance zombie : activeZombies) {
            if (zombie.isDead()) continue;
            Point pos = zombie.getGridPosition();
            if (pos == null) continue;

            int rowDiff = Math.abs(pos.getY() - centerRow);
            int colDiff = Math.abs(pos.getX() - centerCol);
            if (rowDiff <= rowRadius && colDiff <= colRadius) {
                zombies.add(zombie);
            }
        }
        return zombies;
    }

    @Override
    public void damageZombie(ZombieInstance zombie, int damage) {
        damageZombie(zombie, damage, null); // no source = non-plant damage
    }

    /** Damage with kill attribution; null source marks non-plant damage (mower, zombies, cheats). */
    public void damageZombie(ZombieInstance zombie, int damage, Plant source) {
        if (zombie == null || damage <= 0) return;

        if (source != null) zombie.recordPlantDamage(source);
        else zombie.recordNonPlantDamage();
        zombie.takeDamage(damage);
    }

    /** Attribution only, for damage applied outside damageZombie (fire/poison paths). */
    public void attributePlantDamage(ZombieInstance zombie, Plant source) {
        if (zombie != null && source != null) zombie.recordPlantDamage(source);
    }

    @Override
    public boolean moveZombieToLane(ZombieInstance zombie, int newRow) {
        if (zombie == null) return false;
        if (newRow < 0 || newRow >= gameMap.getRows()) return false;

        Point pos = zombie.getGridPosition();
        if (pos == null) return false;
        int oldRow = pos.getY();
        int col = pos.getX();
        if (oldRow == newRow) return true;

        // Update grid registration
        Cell oldCell = gameMap.getCell(col, oldRow);
        if (oldCell != null) {
            oldCell.removeZombie(zombie);
        }
        Cell newCell = gameMap.getCell(col, newRow);
        if (newCell != null) {
            newCell.addZombie(zombie);
        }

        // Update the zombie's own position
        zombie.setGridPosition(new Point(col, newRow));
        zombie.setContinuousPosition(new FloatPoint(zombie.getContinuousX(), newRow));
        return true;
    }

    /**
     * Pushes a zombie backward (toward the spawn point) by {@code tiles}
     * grid units. If the zombie is pushed past the right edge of the map
     * it is killed instantly.
     */
    public void pushZombieBack(ZombieInstance zombie, float tiles) {
        if (zombie == null || zombie.isDead() || tiles <= 0) return;

        FloatPoint pos = zombie.getContinuousPosition();
        if (pos == null) return;

        float newX = pos.getX() + tiles;

        // Pushed off the right edge - kill the zombie instantly.
        if (newX >= gameMap.getCols()) {
            zombie.setCurrentHP(0);
            zombie.setState(ZombieState.DEAD);
            return;
        }

        zombie.setContinuousX(newX);

        // Update grid column if the zombie crossed a tile boundary.
        int newGridX = (int) Math.floor(newX);
        Point gridPos = zombie.getGridPosition();
        if (gridPos != null && newGridX != gridPos.getX()) {
            int row = gridPos.getY();
            int oldCol = gridPos.getX();

            Cell oldCell = gameMap.getCell(oldCol, row);
            if (oldCell != null) {
                oldCell.removeZombie(zombie);
            }
            if (newGridX >= 0 && newGridX < gameMap.getCols()) {
                Cell newCell = gameMap.getCell(newGridX, row);
                if (newCell != null) {
                    newCell.addZombie(zombie);
                }
            }
            zombie.setGridX(newGridX);
        }
    }

    @Override
    public List<Projectile> getProjectilesInLane(int lane) {
        if (lane < 0 || lane >= gameMap.getRows()) {
            return Collections.emptyList();
        }
        List<Projectile> inLane = new ArrayList<>();
        for (Projectile projectile : activeProjectiles) {
            if (projectile != null && projectile.getRow() == lane) {
                inLane.add(projectile);
            }
        }
        return inLane;
    }

    @Override
    public int getRowCount() {
        return gameMap.getRows();
    }

    @Override
    public int getColumnCount() {
        return gameMap.getCols();
    }

    @Override
    public Cell getCellAt(int row, int col) {
        if (row < 0 || col < 0 || row >= gameMap.getRows() || col >= gameMap.getCols()) {
            return null;
        }
        return gameMap.getCell(col, row);
    }

    @Override
    public boolean spawnGraveAt(int row, int col) {
        return spawnGraveAt(row, col, GraveType.PLAIN, null);
    }

    @Override
    public boolean spawnGraveAt(int row, int col, ZombieInstance raiser) {
        return spawnGraveAt(row, col, GraveType.PLAIN, raiser);
    }

    public boolean spawnGraveAt(int row, int col, GraveType type) {
        return spawnGraveAt(row, col, type, null);
    }

    public boolean spawnGraveAt(int row, int col, GraveType type, ZombieInstance raiser) {
        Cell cell = getCellAt(row, col);
        if (cell == null) {
            return false;
        }
        if (cell.getPlaceable(PlacableLayer.GROUND) != null) {
            return false;
        }
        if (!cell.getAllPlants().isEmpty()) {
            return false;
        }
        Grave grave = new Grave(Grave.DEFAULT_HP, type);
        grave.setRaiser(raiser);
        boolean placed = cell.addPlaceable(grave);
        if (placed) {
            eventBus.dispatch(new GameEvent(GameEvent.Type.GRAVE_SPAWNED));
            App.logToShell("[Grave] A " + (type == GraveType.PLAIN ? "plain" :
                    type == GraveType.SUN ? "sun" : "plant-food")
                    + " grave surfaced at (" + col + ", " + row + ").");
        }
        return placed;
    }

    @Override
    public int countGravesRaisedBy(ZombieInstance raiser) {
        if (raiser == null || gameMap == null) {
            return 0;
        }
        int n = 0;
        int rows = gameMap.getRows();
        int cols = gameMap.getCols();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Grave grave = getGraveAt(row, col);
                if (grave != null && grave.getRaiser() == raiser && !grave.isDestroyed()) {
                    n++;
                }
            }
        }
        return n;
    }

    public Grave getGraveAt(int row, int col) {
        Cell cell = getCellAt(row, col);
        if (cell == null) return null;
        Placeable p = cell.getPlaceable(PlacableLayer.GROUND);
        return (p instanceof Grave) ? (Grave) p : null;
    }

    public boolean removeGraveAt(int row, int col) {
        Grave grave = getGraveAt(row, col);
        if (grave == null) return false;
        Cell cell = getCellAt(row, col);
        cell.removePlaceable(grave);
        return true;
    }

    /**
     * Turns the cell into an unplantable crater.
     */
    public void createCraterAt(int row, int col) {
        Cell cell = getCellAt(row, col);
        if (cell == null) {
            return;
        }
        cell.setGroundType(GroundType.CRATER);
        cell.setTerrainStrategy(new CraterTerrainStrategy());
    }

    // --- Terrain helpers ---

    /** Applies damage to an ice-terrain block at the given cell. */
    public void damageIceAt(int row, int col, int damage) {
        if (damage <= 0) return;
        Cell cell = getCellAt(row, col);
        if (cell == null) return;
        if (cell.getTerrainStrategy() instanceof IceTerrainStrategy) {
            ((IceTerrainStrategy) cell.getTerrainStrategy()).takeDamage(damage);
        }
    }

    /**
     * Applies damage to every ice-terrain block inside the rectangle
     * centred on ({@code row}, {@code col}) with the given half-extents.
     */
    public void damageIceInArea(int row, int col, int rowRadius, int colRadius, int damage) {
        if (damage <= 0) return;
        for (int rowDist = -rowRadius; rowDist <= rowRadius; rowDist++) {
            for (int colDist = -colRadius; colDist <= colRadius; colDist++) {
                damageIceAt(row + rowDist, col + colDist, damage);
            }
        }
    }

    /**
     * Re-registers an existing zombie instance on the field at the given
     * cell. Used by {@link TerrainSystem} when an ice block shatters and
     * releases a zombie that was frozen inside at level-load time.
     */
    public void addExistingZombie(ZombieInstance zombie, int row, int col) {
        if (zombie == null) return;
        int clampedRow = Math.max(0, Math.min(row, gameMap.getRows() - 1));
        int clampedCol = Math.max(0, Math.min(col, gameMap.getCols() - 1));
        zombie.setGridPosition(new Point(clampedCol, clampedRow));
        if (zombie.getContinuousPosition() == null) {
            zombie.setContinuousPosition(new FloatPoint(clampedCol, clampedRow));
        }
        if (!activeZombies.contains(zombie)) {
            activeZombies.add(zombie);
        }
        Cell cell = gameMap.getCell(clampedCol, clampedRow);
        if (cell != null && !cell.getZombies().contains(zombie)) {
            cell.addZombie(zombie);
        }
    }

    /** Removes a plant from the board immediately (no death FX bookkeeping). */
    public void removePlantFromBoard(PlantInstance plant) {
        if (plant == null || plant.getPosition() == null || gameMap == null) return;
        Cell cell = gameMap.getCell(plant.getPosition().getX(), plant.getPosition().getY());
        if (cell != null) {
            cell.removePlaceable(plant);
        }
    }

    /**
     * Updates a zombie's continuous pose and remaps its grid cell when the
     * floored column/row changes. Used by networked snapshot display sync.
     * Continuous X may be &lt; 0 (brain lane) or past the right edge — those
     * poses stay off-grid and must not call {@link GameMap#getCell}.
     */
    public void syncZombieWorldPose(ZombieInstance zombie, int row, float continuousX, float continuousY) {
        if (zombie == null || gameMap == null) return;
        int clampedRow = Math.max(0, Math.min(row, gameMap.getRows() - 1));
        float y = continuousY;
        if (Float.isNaN(y)) {
            y = clampedRow;
        }
        zombie.setContinuousPosition(new FloatPoint(continuousX, y));

        int newCol = (int) Math.floor(continuousX);
        Point grid = zombie.getGridPosition();
        int oldCol = grid != null ? grid.getX() : newCol;
        int oldRow = grid != null ? grid.getY() : clampedRow;
        if (oldCol != newCol || oldRow != clampedRow) {
            if (inMapBounds(oldCol, oldRow)) {
                Cell oldCell = gameMap.getCell(oldCol, oldRow);
                if (oldCell != null) {
                    oldCell.removeZombie(zombie);
                }
            }
            if (inMapBounds(newCol, clampedRow)) {
                Cell newCell = gameMap.getCell(newCol, clampedRow);
                if (newCell != null) {
                    newCell.addZombie(zombie);
                }
            }
            zombie.setGridPosition(new Point(newCol, clampedRow));
        } else if (grid == null) {
            zombie.setGridPosition(new Point(newCol, clampedRow));
        }
    }

    private boolean inMapBounds(int col, int row) {
        return col >= 0 && row >= 0 && col < gameMap.getCols() && row < gameMap.getRows();
    }
}
