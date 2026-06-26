package model.game.core;

import model.app.App;
import model.enums.Chapter;
import model.enums.GameState;
import model.event.EventBus;
import model.event.GameEvent;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.map.FloatPoint;
import model.game.map.GameMap;
import model.game.rule.EndGameCondition;
import model.game.wave.WaveManager;
import model.item.LootDrop;
import model.item.Sun;
import model.projectile.Projectile;
import model.zombie.ZombieFactory;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;

public class GameModel {
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

    public List<Sun> getSuns() {
        return activeSuns;
    }

    public boolean isNightLevel() {
        return chapter == Chapter.DARK_AGES;
    }

    public void addSun(int amount) {
        sunAmount += amount;
    }

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

    public void removeZombie(ZombieInstance zombie) {
        activeZombies.remove(zombie);
        gameMap.removeZombie(zombie);
    }

    public void spawnProjectile(Projectile projectile, int x, int y) {
        activeProjectiles.add(projectile);
        gameMap.addProjectile(projectile, x, y);
        eventBus.dispatch(new GameEvent(GameEvent.Type.PROJECTILE_FIRED));
    }

    public void removeProjectile(Projectile projectile) {
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
}