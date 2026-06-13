package model.game.core;

import model.app.App;
import model.enums.Chapter;
import model.enums.GameState;
import model.event.EventBus;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.map.GameMap;
import model.game.rule.EndGameCondition;
import model.game.wave.WaveManager;
import model.item.LootDrop;
import model.item.Sun;
import model.projectile.Projectile;
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

    public GameState getState() {
        return gameState;
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
        return false;
    }

    public void addPlantFood() {
        plantFoodCount++;
    }

    public boolean usePlantFood() {
        return false;
    }

    public void spawnZombie(Zombie zombie, int x, int y) {

    }

    public void removeZombie(Zombie zombie) {
    }

    public void spawnProjectile(Projectile projectile, int x, int y) {}

    public void removeProjectile(Projectile projectile) {}

    public void spawnSun(Sun sun) {}

    public void collectSun(Sun sun) {}

    public void advanceTick() {}

    public void setGameState(GameState gameState) {}

    public void queueLootDrop(LootDrop loot) {}

    public void processLootDrops() {}
}