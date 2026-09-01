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
import model.game.map.terrain.FireTerrainStrategy;
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
import java.util.function.Consumer;

public class GameModel implements BehaviorContext {
    private long currentTick;
    private final ResourceBank resources; // sun / plant-food economy (composition)
    private int difficultyLevel;
    private GameState gameState;
    private Chapter chapter;

    private final SeedCooldownBank seedCooldowns = new SeedCooldownBank();
    private final LootWallet loot = new LootWallet();
    private final HouseBreachTracker breach = new HouseBreachTracker();
    private final LawnEntityRoster entities = new LawnEntityRoster();
    private final LawnPresentationFx fx = new LawnPresentationFx();
    private final GameBoardCombat combat;
    private final GameZombieSpawns spawns;

    /** Couch-play I, Zombie: plant-side sun is separate from {@link #getSunAmount()} (zombie bank). */
    private boolean couchPlay;
    private int plantSun;

    private Level currentLevel;
    private WaveManager waveManager;

    private EndGameCondition endGameCondition;

    private GameMap gameMap;

    private EventBus eventBus;
    private Consumer<GameEvent> gameEventListener;
    private List<String> selectedPlants;       // plant types chosen for this level
    // Plant Imitater morph target; last non-Imitater plant the player picked or planted.
    private String imitaterCopyTarget;

    /** Continuous X past the lawn left edge where a breacher stands and chews. */
    public static final float HOUSE_CHEW_X = -0.9f;

    private float elapsedSeconds;

    // Optional scorer attached by the daily Myopoint score level
    private MyopointTracker myopointTracker;

    // Chapter-specific ambient effects (tornado, ice wind, tide)
    private ChapterEffectsSystem chapterEffects;

    /** Big Wave Beach tide / static sea; inactive on other chapters. */
    private final TideState tideState;

    // Per-level stats used for quest tracking (extracted component)
    private final LevelQuestStats questStats = new LevelQuestStats();

    LevelQuestStats questStats() {
        return questStats;
    }

    HouseBreachTracker breach() {
        return breach;
    }

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

        this.combat = new GameBoardCombat(this, entities);
        this.spawns = new GameZombieSpawns(this, entities);

        this.gameMap = new GameMap(levelConfig.getRows(), levelConfig.getColumns());

        this.tideState = TideState.fromConfig(levelConfig);

        this.waveManager = new WaveManager(levelConfig.getWaves(), this);

        this.chapterEffects = new ChapterEffectsSystem(this);

        this.eventBus = event -> {
            if (gameEventListener != null) {
                gameEventListener.accept(event);
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

    public boolean isCouchPlay() {
        return couchPlay;
    }

    public void setCouchPlay(boolean couchPlay) {
        this.couchPlay = couchPlay;
    }

    public int getPlantSun() {
        return plantSun;
    }

    public void setPlantSun(int amount) {
        plantSun = Math.max(0, amount);
    }

    public void addPlantSun(int amount) {
        plantSun = Math.min(9990, plantSun + Math.max(0, amount));
    }

    public boolean spendPlantSun(int amount) {
        if (amount < 0 || plantSun < amount) {
            return false;
        }
        plantSun -= amount;
        return true;
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
        return seedCooldowns.isReady(plantName);
    }

    public float getSeedCooldown(String plantName) {
        return seedCooldowns.remaining(plantName);
    }

    public void startSeedRecharge(String plantName, float seconds) {
        seedCooldowns.start(plantName, seconds);
    }

    public void disableSeedCooldowns() {
        seedCooldowns.disable();
    }

    public boolean areSeedCooldownsDisabled() {
        return seedCooldowns.isDisabled();
    }

    public Map<String, Float> getSeedCooldownsSnapshot() {
        return seedCooldowns.snapshot();
    }

    /** Restores seed cooldowns from a mid-level save. */
    public void restoreSeedCooldowns(Map<String, Float> cooldowns, boolean disabled) {
        seedCooldowns.restore(cooldowns, disabled);
    }

    /** Overwrites sun / plant-food balances from a mid-level save. */
    public void restoreResources(int sun, int plantFood, int persistentPlantFood) {
        resources.restore(sun, plantFood, persistentPlantFood);
    }

    public int getPersistentPlantFood() {
        return resources.getPersistentPlantFood();
    }

    /** Restores bookkeeping counters from a mid-level save. */
    public void restoreProgress(long tick, float elapsed, int difficulty,
                                boolean breached, Set<Integer> breachedLaneRows,
                                int killed, int lost,
                                int diamonds, int coins, int pots) {
        this.currentTick = Math.max(0L, tick);
        this.elapsedSeconds = Math.max(0f, elapsed);
        this.difficultyLevel = Math.max(1, difficulty);
        this.breach.restore(breached, breachedLaneRows, killed, lost);
        this.loot.restore(diamonds, coins, pots);
    }

    /** Clears plants/graves/zombies/projectiles/pickups before applying a save. */
    public void clearBoardForRestore() {
        entities.clearBoard(gameMap);
        fx.clear();
        breach.setBreachingZombie(null);
    }

    /** Places a plant without spending sun or updating quest placement stats. */
    public boolean restorePlant(PlantInstance plant, int row, int col) {
        return entities.restorePlant(gameMap, plant, row, col);
    }

    /** Re-adds a zombie that was already constructed from a save. */
    public void restoreZombie(ZombieInstance instance) {
        entities.restoreZombie(gameMap, instance);
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

    /** Optional GUI hook for {@link GameEvent}s (e.g. combat SFX). */
    public void setGameEventListener(Consumer<GameEvent> gameEventListener) {
        this.gameEventListener = gameEventListener;
    }

    public List<ZombieInstance> getZombies() {
        return entities.zombies;
    }

    public List<ZombieInstance> getActiveZombies() {
        return entities.zombies;
    }

    public List<Projectile> getProjectiles() {
        return entities.projectiles;
    }

    public List<Projectile> getActiveProjectiles() {
        return entities.projectiles;
    }

    /** Records a projectile impact for one-shot splat / hit PAM playback. */
    public void recordProjectileHit(Projectile projectile) {
        fx.recordProjectileHit(projectile);
    }

    /**
     * Projectile impacts since the last drain. The lawn renderer consumes this
     * each frame; empty when nothing hit.
     */
    public List<Projectile> drainProjectileHits() {
        return fx.drainProjectileHits();
    }

    /** Drops cues the view never consumed (TUI, skipped frames). */
    public void discardUnreadProjectileHits() {
        fx.discardUnreadProjectileHits();
    }

    public void recordRadioactiveSunExplosion(int col, int row) {
        fx.recordRadioactiveSunExplosion(col, row);
    }

    public List<Point> drainRadioactiveSunExplosions() {
        return fx.drainRadioactiveSunExplosions();
    }

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

    /**
     * Marks a freshly spawned ambush zombie as surfacing from the shallow
     * water; the view plays the Snorkel-style mask + ripple while it rises.
     */
    public void beginWaterEmerge(ZombieInstance zombie) {
        fx.beginWaterEmerge(zombie);
    }

    /** In-flight water emergences (read-only) for the view layer. */
    public List<WaterEmerge> getWaterEmerges() {
        return fx.waterEmerges();
    }

    /** True while a low-tide ambush zombie is still surfacing. */
    public boolean isWaterEmerging(ZombieInstance zombie) {
        return fx.isWaterEmerging(zombie);
    }

    @Override
    public void armLaneSlide(ZombieInstance zombie, Cell slideTile, int toRow) {
        fx.armLaneSlide(zombie, slideTile, toRow);
    }

    /**
     * Fires {@code zombie}'s armed slide once it reaches the slide tile's
     * middle. Zombies walk left, and integer continuous X is a tile centre,
     * so the midpoint is crossed at {@code continuousX <= tileColumn}.
     *
     * @return true exactly when the slide fired this tick
     */
    public boolean tickArmedSlide(ZombieInstance zombie, float continuousX) {
        return fx.tickArmedSlide(this, zombie, continuousX);
    }

    /**
     * Slide activations since the last drain. The lawn renderer consumes this
     * each frame to play the slider active_start / active_end clips; empty
     * when nothing slid.
     */
    public List<Point> drainSlideStarts() {
        return fx.drainSlideStarts();
    }

    /** Drops cues the view never consumed (TUI, skipped frames). */
    public void discardUnreadSlideStarts() {
        fx.discardUnreadSlideStarts();
    }

    /** In-flight slide glides (read-only) for the view layer. */
    public List<LaneSlide> getLaneSlides() {
        return fx.laneSlides();
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
        return entities.suns;
    }

    public List<PlantFoodPickup> getActivePlantFood() {
        return entities.plantFood;
    }

    public List<LootPickup> getActiveLootPickups() {
        return entities.lootPickups;
    }

    public boolean isNightLevel() {
        return chapter == Chapter.DARK_AGES;
    }

    public void addDiamonds(int amount) {
        loot.addDiamonds(amount);
    }

    public void addCoins(int amount) {
        loot.addCoins(amount);
    }

    public void addFlowerPots(int amount) {
        loot.addFlowerPots(amount);
    }

    public int getDiamondCount() { return loot.diamonds(); }

    public int getCoinCount() { return loot.coins(); }

    public int getFlowerPotCount() { return loot.flowerPots(); }

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
        return combat.placePlant(plant, row, col);
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
        spawns.recordZombieSeen(zombieName);
    }

    public void spawnZombie(Zombie zombie, int lane) {
        spawns.spawnZombie(zombie, lane);
    }

    /**
     * Ancient Egypt tornado entry (final wave): the zombie is carried in by a
     * sandstorm and touches down 1-4 columns ahead of the normal entry edge.
     *
     * @return the landed instance, so sandstorm records can hide it behind
     *         the outro fade
     */
    public ZombieInstance spawnZombieWithTornado(Zombie zombie, int lane, int columnsAhead) {
        return spawns.spawnZombieWithTornado(zombie, lane, columnsAhead);
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
        fx.queueSandstormSpawn(this, zombie, lane, columnsAhead);
    }

    /** In-flight sandstorms (read-only) for the view layer. */
    public List<SandstormSpawn> getSandstorms() {
        return fx.sandstorms();
    }

    /**
     * Frostbite Caves ice wind: queues a gust visual sweeping {@code lane}
     * (the frost damage itself is applied by the chapter effects system).
     */
    public void queueIceWindGust(int lane) {
        fx.queueIceWindGust(lane);
    }

    /** Active ice winds (read-only) for the view layer. */
    public List<IceWindGust> getIceWinds() {
        return fx.iceWinds();
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
        fx.enqueueAnnouncement(text);
    }

    /** Next pending sting, or {@code null} if the queue is empty. */
    public String consumeWaveAnnouncement() {
        return fx.consumeAnnouncement();
    }

    @Override
    public ZombieInstance spawnZombieAt(String zombieDefinitionName, int row, int col) {
        return spawns.spawnZombieAt(zombieDefinitionName, row, col);
    }

    public void removeZombie(ZombieInstance zombie) {
        spawns.removeZombie(zombie);
    }

    @Override
    public void orphanPushable(Pushable pushable) {
        entities.orphanPushable(pushable);
    }

    @Override
    public List<Pushable> getOrphanedPushables() {
        return entities.orphanedPushables;
    }

    @Override
    public void removeOrphanedPushable(Pushable pushable) {
        entities.orphanedPushables.remove(pushable);
    }

    public void spawnProjectile(Projectile projectile, int x, int y) {
        entities.projectiles.add(projectile);
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
        entities.projectiles.remove(projectile);
        gameMap.removeProjectile(projectile);
    }

    public void spawnSun(Sun sun) {
        entities.suns.add(sun);
    }

    public void collectSun(Sun sun) {
        entities.suns.remove(sun);
        if (couchPlay) {
            addPlantSun(sun.getValue());
        } else {
            resources.addSun(sun.getValue());
        }
        questStats.onSunCollected(sun.getValue());
    }

    public void spawnPlantFood(PlantFoodPickup pickup) {
        if (pickup != null) {
            entities.plantFood.add(pickup);
        }
    }

    public void collectPlantFood(PlantFoodPickup pickup) {
        if (pickup == null) {
            return;
        }
        entities.plantFood.remove(pickup);
        resources.addPlantFood();
    }

    public void spawnLootPickup(LootPickup pickup) {
        if (pickup != null) {
            entities.lootPickups.add(pickup);
        }
    }

    public void removeLootPickup(LootPickup pickup) {
        if (pickup != null) {
            entities.lootPickups.remove(pickup);
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
        fx.tick(deltaTime);
        seedCooldowns.tick(deltaTime);
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public EndGameCondition getEndGameCondition() {
        return endGameCondition;
    }

    /** True once a zombie has walked into the player's house. */
    public boolean isHouseBreached() {
        return breach.isHouseBreached();
    }

    public void markHouseBreached() {
        breach.markHouseBreached();
    }

    /** Marks a breach in a specific lane. In I, Zombie this is an eaten brain. */
    public void markHouseBreached(int row) {
        breach.markHouseBreached(row);
    }

    /**
     * I, Zombie: destroys the brain in {@code row} after a chew finishes.
     * Does not pin a breaching zombie (those walk off the left afterward).
     */
    public void markBrainEaten(int row) {
        breach.markBrainEaten(row);
    }

    /**
     * House breach: mark the lane lost and pin the zombie in an eat loop.
     * Leaves continuous X where it is (past the lawn edge into the house).
     */
    public void applyHouseBreach(ZombieInstance zombie, int row) {
        breach.applyHouseBreach(zombie, row);
    }

    /** Rows whose lane end has been breached at least once. */
    public Set<Integer> getBreachedRows() {
        return breach.breachedRows();
    }

    /** Authoritative breach list from a networked snapshot. */
    public void syncBreachedRows(java.util.Collection<Integer> rows) {
        breach.syncBreachedRows(rows);
    }

    /** Replaces falling/collectible sun tokens for display sync. */
    public void replaceActiveSuns(List<Sun> suns) {
        entities.replaceSuns(suns);
    }

    /** Plant-side seed packet cooldowns from the authoritative server. */
    public void syncSeedCooldowns(java.util.Map<String, Float> cooldowns) {
        seedCooldowns.syncFromNetwork(cooldowns);
    }

    /** Zombie chewing at the house after a breach, or {@code null}. */
    public ZombieInstance getBreachingZombie() {
        return breach.breachingZombie();
    }

    public void setBreachingZombie(ZombieInstance zombie) {
        breach.setBreachingZombie(zombie);
    }

    /** Continuous column of the last kill, or {@link Float#NaN} if none yet. */
    public float getLastZombieDeathX() {
        return breach.lastDeathX();
    }

    /** Lane row of the last kill, or {@link Float#NaN} if none yet. */
    public float getLastZombieDeathY() {
        return breach.lastDeathY();
    }

    public void recordLastZombieDeath(float continuousX, float row) {
        breach.recordLastDeath(continuousX, row);
    }

    public int getZombiesKilled() {
        return breach.zombiesKilled();
    }

    public void incrementZombiesKilled() {
        breach.incrementZombiesKilled();
    }

    /** Notifies the optional Myopoint scorer that a zombie has just died. */
    public void notifyZombieKilledForScore(ZombieInstance zombie) {
        if (myopointTracker != null) {
            myopointTracker.onZombieKilled(zombie, elapsedSeconds, currentTick);
        }
    }

    /** Records a kill with timing/position details for quest tracking. */
    public void recordZombieKilled(ZombieInstance zombie) {
        breach.incrementZombiesKilled();
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
        return breach.plantsLost();
    }

    /** Seconds of simulated time since the level started. */
    public float getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void queueLootDrop(LootDrop lootDrop) {
        entities.queueLootDrop(lootDrop, this);
    }

    public void processLootDrops() {
        entities.processLootDrops(this);
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
        return entities.zombies.size();
    }

    // --- BehaviorContext implementation ---

    @Override
    public void removeSun(Sun sun) {
        entities.suns.remove(sun);
    }

    @Override
    public PlantInstance getPlantAt(int row, int col) {
        return combat.getPlantAt(row, col);
    }

    public List<PlantInstance> getAllPlantsAt(int row, int col) {
        return combat.getAllPlantsAt(row, col);
    }

    @Override
    public List<PlantInstance> getPlantsInLane(int lane) {
        return combat.getPlantsInLane(lane);
    }

    @Override
    public List<PlantInstance> getAllPlants() {
        return combat.getAllPlants();
    }

    @Override
    public void damagePlant(PlantInstance plant, int damage) {
        combat.damagePlant(plant, damage);
    }

    @Override
    public boolean movePlant(PlantInstance plant, int row, int col) {
        return combat.movePlant(plant, row, col);
    }

    @Override
    public void destroyPlant(PlantInstance plant) {
        combat.destroyPlant(plant);
    }

    @Override
    public List<ZombieInstance> getZombiesInLane(int lane) {
        return combat.getZombiesInLane(lane);
    }

    @Override
    public List<ZombieInstance> getZombiesInArea(int centerRow, int centerCol, int rowRadius, int colRadius) {
        return combat.getZombiesInArea(centerRow, centerCol, rowRadius, colRadius);
    }

    @Override
    public void damageZombie(ZombieInstance zombie, int damage) {
        combat.damageZombie(zombie, damage, null);
    }

    /** Damage with kill attribution; null source marks non-plant damage (mower, zombies, cheats). */
    public void damageZombie(ZombieInstance zombie, int damage, Plant source) {
        combat.damageZombie(zombie, damage, source);
    }

    /** Attribution only, for damage applied outside damageZombie (fire/poison paths). */
    public void attributePlantDamage(ZombieInstance zombie, Plant source) {
        if (zombie != null && source != null) zombie.recordPlantDamage(source);
    }

    @Override
    public boolean moveZombieToLane(ZombieInstance zombie, int newRow) {
        return combat.moveZombieToLane(zombie, newRow);
    }

    /**
     * Pushes a zombie backward (toward the spawn point) by {@code tiles}
     * grid units. If the zombie is pushed past the right edge of the map
     * it is killed instantly.
     */
    public void pushZombieBack(ZombieInstance zombie, float tiles) {
        combat.pushZombieBack(zombie, tiles);
    }

    @Override
    public List<Projectile> getProjectilesInLane(int lane) {
        return combat.getProjectilesInLane(lane);
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
        return combat.spawnGraveAt(row, col, GraveType.PLAIN, null);
    }

    @Override
    public boolean spawnGraveAt(int row, int col, ZombieInstance raiser) {
        return combat.spawnGraveAt(row, col, GraveType.PLAIN, raiser);
    }

    public boolean spawnGraveAt(int row, int col, GraveType type) {
        return combat.spawnGraveAt(row, col, type, null);
    }

    public boolean spawnGraveAt(int row, int col, GraveType type, ZombieInstance raiser) {
        return combat.spawnGraveAt(row, col, type, raiser);
    }

    @Override
    public int countGravesRaisedBy(ZombieInstance raiser) {
        return combat.countGravesRaisedBy(raiser);
    }

    public Grave getGraveAt(int row, int col) {
        return combat.getGraveAt(row, col);
    }

    public boolean removeGraveAt(int row, int col) {
        return combat.removeGraveAt(row, col);
    }

    /**
     * Turns the cell into an unplantable crater.
     */
    public void createCraterAt(int row, int col) {
        combat.createCraterAt(row, col);
    }

    /**
     * Sets a temporary fire tile. Replaces any prior ground
     * strategy on that cell for {@code durationSeconds}.
     */
    public void igniteTile(int row, int col, float durationSeconds) {
        combat.igniteTile(row, col, durationSeconds);
    }

    public void plantFrozenZombieAt(int row, int col, String zombieDefinitionName) {
        combat.plantFrozenZombieAt(row, col, zombieDefinitionName);
    }

    /** @return the living Zomboss on the field, or {@code null}. */
    public ZombieInstance findZomboss() {
        return combat.findZomboss();
    }

    // --- Terrain helpers ---

    /** Applies damage to an ice-terrain block at the given cell. */
    public void damageIceAt(int row, int col, int damage) {
        combat.damageIceAt(row, col, damage);
    }

    /**
     * Applies damage to every ice-terrain block inside the rectangle
     * centred on ({@code row}, {@code col}) with the given half-extents.
     */
    public void damageIceInArea(int row, int col, int rowRadius, int colRadius, int damage) {
        combat.damageIceInArea(row, col, rowRadius, colRadius, damage);
    }

    /**
     * Re-registers an existing zombie instance on the field at the given
     * cell. Used by {@link TerrainSystem} when an ice block shatters and
     * releases a zombie that was frozen inside at level-load time.
     */
    public void addExistingZombie(ZombieInstance zombie, int row, int col) {
        combat.addExistingZombie(zombie, row, col);
    }

    /** Removes a plant from the board immediately (no death FX bookkeeping). */
    public void removePlantFromBoard(PlantInstance plant) {
        combat.removePlantFromBoard(plant);
    }

    /**
     * Updates a zombie's continuous pose and remaps its grid cell when the
     * floored column/row changes. Used by networked snapshot display sync.
     * Continuous X may be &lt; 0 (brain lane) or past the right edge — those
     * poses stay off-grid and must not call {@link GameMap#getCell}.
     */
    public void syncZombieWorldPose(ZombieInstance zombie, int row, float continuousX, float continuousY) {
        combat.syncZombieWorldPose(zombie, row, continuousX, continuousY);
    }
}
