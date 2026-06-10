package model.game;

import model.enums.Chapter;
import model.enums.GameState;
import model.game.level.Level;
import model.game.map.GameMap;
import model.game.rule.EndGameCondition;
import model.game.wave.WaveManager;
import model.item.LootDrop;
import model.item.Sun;
import model.projectile.Projectile;
import model.zombie.definition.Zombie;

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
    private List<Zombie> activeZombies;
    private List<Projectile> activeProjectiles;
    private List<Sun> activeSuns;
    private List<LootDrop> pendingLootDrops;

    // eventBus


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

    public List<Zombie> getZombies() {
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