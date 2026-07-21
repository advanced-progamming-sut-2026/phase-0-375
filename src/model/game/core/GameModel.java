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
import model.game.map.terrain.IceTerrainStrategy;
import model.game.map.terrain.TerrainStrategy;
import model.game.systems.TerrainSystem;
import model.game.map.Point;
import model.game.rule.EndGameCondition;
import model.game.score.MyopointTracker;
import model.game.wave.WaveManager;
import model.item.Grave;
import model.item.LootDrop;
import model.item.Sun;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.user.User;
import model.zombie.ZombieFactory;
import model.zombie.behavior.BehaviorContext;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;

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
    private int sunAmount;
    private int plantFoodCount;
    private int persistentPlantFood; // portion of plantFoodCount backed by the user profile
    private int difficultyLevel;
    private GameState gameState;
    private Chapter chapter;

    private Level currentLevel;
    private WaveManager waveManager;

    private EndGameCondition endGameCondition;

    private GameMap gameMap;
    private List<ZombieInstance> activeZombies;
    private List<Projectile> activeProjectiles;
    private List<Sun> activeSuns;
    private List<LootDrop> pendingLootDrops;

    private EventBus eventBus;
    private List<String> selectedPlants;       // plant types chosen for this level

    // End-game bookkeeping (read by EndGameCondition implementations)
    private boolean houseBreached;
    private final Set<Integer> breachedRows = new HashSet<>();
    private int zombiesKilled;
    private int plantsLost;
    private float elapsedSeconds;

    // Optional scorer attached by the daily Myopoint score level
    private MyopointTracker myopointTracker;

    // Per-level stats used for quest tracking
    private int sunCollected;
    private boolean lawnMowerUsed;
    private float firstZombieSpawnTime = -1f;
    private int killsWithin30s;
    private int noMowerFirstColumnKills;
    private List<Plant> plantsPlaced;
    private Set<Integer> rowsPlanted;
    private Set<Integer> columnsPlanted;
    private int maxSunProducersAtOnce; // peak simultaneous sun producers on the field
    private final Map<String, Integer> exclusivePlantKills = new HashMap<>();          // kills damaged only by one plant type
    private final Map<PlantCategory, Integer> exclusiveFamilyKills = new HashMap<>();  // kills damaged only by one family

    public GameModel(Level currentLevel) {
        this.currentTick = 0;
        this.difficultyLevel = App.getInstance().getCurrentUser().getDifficultyLevel();
        this.gameState = GameState.RUNNING;

        this.currentLevel = currentLevel;
        LevelConfig levelConfig = this.currentLevel.getConfig();
        this.sunAmount = levelConfig.getRules().getInitialSun();
        // load plant food bought from the shop (stored on the user profile)
        User pfOwner = App.getInstance().getCurrentUser();
        this.plantFoodCount = pfOwner != null ? Math.max(0, pfOwner.getPlantFoodCount()) : 0;
        this.persistentPlantFood = this.plantFoodCount;
        this.chapter = levelConfig.getChapter();
        this.endGameCondition = levelConfig.getEndGameCondition();

        this.activeZombies = new ArrayList<>();
        this.activeProjectiles = new ArrayList<>();
        this.activeSuns = new ArrayList<>();
        this.pendingLootDrops = new ArrayList<>();
        this.plantsPlaced = new ArrayList<>();
        this.rowsPlanted = new HashSet<>();
        this.columnsPlanted = new HashSet<>();

        this.gameMap = new GameMap(levelConfig.getRows(), levelConfig.getColumns());

        this.waveManager = new WaveManager(levelConfig.getWaves(), this);

        this.eventBus = new EventBus() {
            @Override
            public void dispatch(GameEvent event) {

            }
        };
    }

    public GameMap getMap() {
        return gameMap;
    }

    public long getTick() {
        return currentTick;
    }

    @Override
    public int getSunAmount() {
        return sunAmount;
    }

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public int getDifficulty() {
        return difficultyLevel;
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

    public List<Projectile> getProjectiles() {
        return activeProjectiles;
    }

    @Override
    public List<Sun> getActiveSuns() {
        return activeSuns;
    }

    public boolean isNightLevel() {
        return chapter == Chapter.DARK_AGES;
    }

    /**
     * @return true if the cell at {@code (row, col)} is a water tile.
     */
    public boolean isWaterTile(int row, int col) {
        Cell cell = getCellAt(row, col);
        return cell != null && cell.getGroundType() == GroundType.WATER;
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
            plantsPlaced.add(plant.getDefinition());
            rowsPlanted.add(row);
            columnsPlanted.add(col);
            updateMaxSunProducers(); // count can only grow on placement
        }
        if (added && eventBus != null) {
            eventBus.dispatch(new GameEvent(GameEvent.Type.PLANT_PLACED));
        }
        return added;
    }

    @Override
    public void addSun(int amount) {
        sunAmount += amount;
    }

    @Override
    public boolean spendSun(int amount) {
        if (sunAmount < amount) return false;
        sunAmount -= amount;
        return true;
    }

    public void addPlantFood() {
        plantFoodCount++;
    }

    public boolean usePlantFood() {
        if (plantFoodCount < 1) return false;
        plantFoodCount--;
        // purchased plant food is consumed from the profile too
        if (persistentPlantFood > 0) {
            persistentPlantFood--;
            User owner = App.getInstance().getCurrentUser();
            if (owner != null && owner.getPlantFoodCount() > 0) {
                owner.setPlantFoodCount(owner.getPlantFoodCount() - 1);
                App.getInstance().getUserRepository().flush();
            }
        }
        return true;
    }

    /** Records a zombie type as seen for the collection; saves only on first sighting. */
    public void recordZombieSeen(String zombieName) {
        if (firstZombieSpawnTime < 0) {
            firstZombieSpawnTime = elapsedSeconds;
        }
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
            App.getInstance().getUserRepository().flush();
        }
    }

    public void spawnZombie(Zombie zombie, int lane) {
        ZombieInstance instance = ZombieFactory.createInstance(zombie);
        recordZombieSeen(zombie.getName());
        instance.setContinuousPosition(new FloatPoint(gameMap.getCols(), lane));
        instance.setGridPosition(new Point(gameMap.getCols(), lane));
        activeZombies.add(instance);
        gameMap.addZombie(instance, gameMap.getCols(), lane);
        if (myopointTracker != null) {
            myopointTracker.onZombieSpawned(instance, elapsedSeconds);
        }
        eventBus.dispatch(new GameEvent(GameEvent.Type.ZOMBIE_SPAWNED));
    }

    @Override
    public ZombieInstance spawnZombieAt(String zombieDefinitionName, int row, int col) {
        ZombieInstance instance = ZombieFactory.createInstance(zombieDefinitionName);
        if (instance == null) {
            return null;
        }
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
    }

    public void spawnProjectile(Projectile projectile, int x, int y) {
        activeProjectiles.add(projectile);
        gameMap.addProjectile(projectile, x, y);
        eventBus.dispatch(new GameEvent(GameEvent.Type.PROJECTILE_FIRED));
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
        sunAmount += sun.getValue();
        sunCollected += sun.getValue();
    }

    public void tick(float deltaTime) {
        currentTick += 1;
        elapsedSeconds += deltaTime;
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

    /** Rows whose lane end has been breached at least once. */
    public Set<Integer> getBreachedRows() {
        return breachedRows;
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
        if (firstZombieSpawnTime >= 0 && elapsedSeconds - firstZombieSpawnTime <= 30f) {
            killsWithin30s++;
        }
        if (zombie == null) return;
        // exclusive-kill bookkeeping for plant/family quests
        if (!zombie.isNonPlantDamaged()) {
            if (zombie.getPlantDamagers().size() == 1) {
                exclusivePlantKills.merge(zombie.getPlantDamagers().iterator().next(), 1, Integer::sum);
            }
            if (zombie.getPlantDamagerFamilies().size() == 1) {
                exclusiveFamilyKills.merge(zombie.getPlantDamagerFamilies().iterator().next(), 1, Integer::sum);
            }
        }
        if (zombie.getGridPosition() == null) return;
        if (zombie.getGridX() <= 0) {
            Lane lane = gameMap.getLane(zombie.getGridY());
            if (lane == null || !lane.hasActiveLawnMower()) {
                noMowerFirstColumnKills++;
            }
        }
    }

    public int getSunCollected() { return sunCollected; }

    public void markLawnMowerUsed() { lawnMowerUsed = true; }

    public boolean isLawnMowerUsed() { return lawnMowerUsed; }

    public int getKillsWithin30s() { return killsWithin30s; }

    public int getNoMowerFirstColumnKills() { return noMowerFirstColumnKills; }

    /** Kills where the zombie was damaged exclusively by the named plant. */
    public int getExclusivePlantKills(String plantName) {
        if (plantName == null) return 0;
        for (Map.Entry<String, Integer> e : exclusivePlantKills.entrySet()) {
            if (plantName.equalsIgnoreCase(e.getKey())) return e.getValue();
        }
        return 0;
    }

    /** Kills where the zombie was damaged exclusively by plants of the named family. */
    public int getExclusiveFamilyKills(String categoryName) {
        if (categoryName == null) return 0;
        for (Map.Entry<PlantCategory, Integer> e : exclusiveFamilyKills.entrySet()) {
            if (e.getKey().name().equalsIgnoreCase(categoryName)) return e.getValue();
        }
        return 0;
    }

    public List<Plant> getPlantsPlaced() { return plantsPlaced; }

    public int getMaxSunProducersAtOnce() { return maxSunProducersAtOnce; }

    /** Recounts sun producers on the field and updates the peak. */
    private void updateMaxSunProducers() {
        int count = 0;
        for (int r = 0; r < gameMap.getRows(); r++) {
            for (int c = 0; c < gameMap.getCols(); c++) {
                PlantInstance p = getPlantAt(r, c);
                if (p != null && p.getDefinition() != null
                        && p.getDefinition().getCategory() == PlantCategory.SUN_PRODUCER) {
                    count++;
                }
            }
        }
        if (count > maxSunProducersAtOnce) {
            maxSunProducersAtOnce = count;
        }
    }

    public Set<Integer> getRowsPlanted() { return rowsPlanted; }

    public Set<Integer> getColumnsPlanted() { return columnsPlanted; }

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

        int newHP = Math.max(0, plant.getCurrentHP() - damage);
        boolean wasAlive = plant.getCurrentHP() > 0;
        plant.setCurrentHP(newHP);
        if (wasAlive && newHP == 0) {
            plantsLost++;
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
        rowsPlanted.add(row);
        columnsPlanted.add(col);
        return true;
    }

    @Override
    public void destroyPlant(PlantInstance plant) {
        if (plant == null) return;

        Point pos = plant.getPosition();
        if (pos != null) {
            Cell cell = getCellAt(pos.getY(), pos.getX());
            if (cell != null) {
                cell.removePlaceable(plant);
            }
        }
        if (plant.getCurrentHP() > 0) {
            plantsLost++;
        }
        plant.setCurrentHP(0);
        eventBus.dispatch(new GameEvent(GameEvent.Type.PLANT_DESTROYED));
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
        Cell cell = getCellAt(row, col);
        if (cell == null) {
            return false;
        }
        if (cell.getPlaceable(PlacableLayer.GROUND) != null) {
            return false;
        }
        boolean placed = cell.addPlaceable(new Grave());
        if (placed) {
            eventBus.dispatch(new GameEvent(GameEvent.Type.GRAVE_SPAWNED));
        }
        return placed;
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
        if (!activeZombies.contains(zombie)) {
            activeZombies.add(zombie);
        }
        Cell cell = gameMap.getCell(col, row);
        if (cell != null && !cell.getZombies().contains(zombie)) {
            cell.addZombie(zombie);
        }
    }
}