package model.game.core;

import model.app.App;
import model.enums.Chapter;
import model.enums.GameState;
import model.enums.PlacableLayer;
import model.enums.ZombieState;
import model.event.EventBus;
import model.event.GameEvent;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.map.Cell;
import model.game.map.FloatPoint;
import model.game.map.GameMap;
import model.game.map.Point;
import model.game.rule.EndGameCondition;
import model.game.wave.WaveManager;
import model.item.Grave;
import model.item.LootDrop;
import model.item.Sun;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.zombie.ZombieFactory;
import model.zombie.behavior.BehaviorContext;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameModel implements BehaviorContext {
    private long currentTick;
    private int sunAmount;
    private int plantFoodCount;
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

    public GameModel(Level currentLevel) {
        this.currentTick = 0;
        this.difficultyLevel = App.getInstance().getCurrentUser().getDifficultyLevel();
        this.gameState = GameState.RUNNING;

        this.currentLevel = currentLevel;
        LevelConfig levelConfig = this.currentLevel.getConfig();
        this.sunAmount = levelConfig.getRules().getInitialSun();
        this.plantFoodCount = 0;
        this.chapter = levelConfig.getChapter();
        this.endGameCondition = levelConfig.getEndGameCondition();

        this.waveManager = new WaveManager(levelConfig.getWaves());

        this.activeZombies = new ArrayList<>();
        this.activeProjectiles = new ArrayList<>();
        this.activeSuns = new ArrayList<>();
        this.pendingLootDrops = new ArrayList<>();

        this.gameMap = new GameMap(levelConfig.getRows(), levelConfig.getColumns());

        this.eventBus = null;
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
        return true;
    }

    public void spawnZombie(Zombie zombie, int lane) {
        ZombieInstance instance = ZombieFactory.createInstance(zombie);
        instance.setContinuousPosition(new FloatPoint(gameMap.getCols(), lane));
        activeZombies.add(instance);
        gameMap.addZombie(instance, gameMap.getCols(), lane);
        eventBus.dispatch(new GameEvent(GameEvent.Type.ZOMBIE_SPAWNED));
    }

    @Override
    public ZombieInstance spawnZombieAt(String zombieDefinitionName, int row, int col) {
        ZombieInstance instance = ZombieFactory.createInstance(zombieDefinitionName);
        if (instance == null) {
            return null;
        }

        int clampedRow = Math.max(0, Math.min(row, gameMap.getRows() - 1));
        int clampedCol = Math.max(0, Math.min(col, gameMap.getCols() - 1));

        instance.setGridPosition(new Point(clampedCol, clampedRow));
        instance.setContinuousPosition(new FloatPoint(clampedCol, clampedRow));
        instance.setState(ZombieState.SPAWNING);

        activeZombies.add(instance);
        gameMap.addZombie(instance, clampedRow, clampedCol);
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
    }

    public void tick(float deltaTime) {
        currentTick += 1;
    }

    public void setGameState(GameState gameState) {}

    public void queueLootDrop(LootDrop loot) {}

    public void processLootDrops() {}

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
        Object placeable = gameMap.getCell(row, col).getPlaceable(PlacableLayer.MAIN);
        if (placeable instanceof PlantInstance) {
            return (PlantInstance) placeable;
        }
        return null;
    }

    @Override
    public List<PlantInstance> getPlantsInLane(int lane) {
        if (lane < 0 || lane >= gameMap.getRows()) {
            return Collections.emptyList();
        }
        List<PlantInstance> plants = new ArrayList<>();
        for (int col = 0; col < gameMap.getCols(); col++) {
            PlantInstance plant = getPlantAt(lane, col);
            if (plant != null && plant.getCurrentHP() > 0) {
                plants.add(plant);
            }
        }
        return plants;
    }

    @Override
    public List<PlantInstance> getAllPlants() {
        List<PlantInstance> plants = new ArrayList<>();
        for (int row = 0; row < gameMap.getRows(); row++) {
            for (int col = 0; col < gameMap.getCols(); col++) {
                PlantInstance plant = getPlantAt(row, col);
                if (plant != null && plant.getCurrentHP() > 0) {
                    plants.add(plant);
                }
            }
        }
        return plants;
    }

    @Override
    public void damagePlant(PlantInstance plant, int damage) {
        if (plant == null || damage <= 0) return;

        int newHP = Math.max(0, plant.getCurrentHP() - damage);
        plant.setCurrentHP(newHP);
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

        Cell destinationCell = gameMap.getCell(row, col);
        if (destinationCell.getPlaceable(PlacableLayer.MAIN) != null) {
            return false;
        }

        Cell sourceCell = gameMap.getCell(currentPos.getY(), currentPos.getX());
        sourceCell.removePlaceable(plant);
        destinationCell.addPlaceable(plant);
        plant.setPosition(new Point(col, row));
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
        if (zombie == null || damage <= 0) return;

        zombie.takeDamage(damage);
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
        Cell oldCell = gameMap.getCell(oldRow, col);
        if (oldCell != null) {
            oldCell.removeZombie(zombie);
        }
        Cell newCell = gameMap.getCell(newRow, col);
        if (newCell != null) {
            newCell.addZombie(zombie);
        }

        // Update the zombie's own position
        zombie.setGridPosition(new Point(col, newRow));
        zombie.setContinuousPosition(new FloatPoint(zombie.getContinuousX(), newRow));
        return true;
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
        return gameMap.getCell(row, col);
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
}