//package model.game.systems;
//
//import model.game.core.Tickable;
//import model.enums.Chapter;
//import model.event.EventBus;
//import model.game.core.GameModel;
//import model.zombie.ZombieFactory;
//import model.zombie.definition.Zombie;
//
//import java.util.List;
//import java.util.Random;
//
//
//public class WaveSpawnerSystem implements Tickable {
//
//    private int currentWave;
//    private float baseDifficulty;          // wave point budget for wave 1
//    private boolean isFinalWave;
//    private int totalWaves;
//    private Chapter chapterContext;
//    private float totalZombieHP;           // total HP of current wave's zombies
//    private float damageDealt;             // damage dealt to current wave's zombies
//    private boolean waveInProgress;
//    private final GameModel gameModel;
//    private final EventBus eventBus;
//    private final ZombieFactory zombieFactory;
//    private final Random random;
//
//    public WaveSpawnerSystem(GameModel gameModel, EventBus eventBus,
//                             ZombieFactory zombieFactory, int totalWaves, float baseDifficulty) {
//        this.gameModel = gameModel;
//        this.eventBus = eventBus;
//        this.zombieFactory = zombieFactory;
//        this.totalWaves = totalWaves;
//        this.baseDifficulty = baseDifficulty;
//        this.currentWave = 0;
//        this.isFinalWave = false;
//        this.waveInProgress = false;
//        this.chapterContext = Chapter.ANCIENT_EGYPT;
//        this.random = new Random();
//    }
//
//    @Override
//    public void tick(float deltaTime) {
//
//    }
//
//    /**
//     * Checks if the current wave's zombies are 75% depleted.
//     * If so, starts the next wave.
//     */
//    public void checkWaveProgress() {
//
//    }
//
//    /**
//     * Starts the next wave of zombies.
//     * Wave difficulty: base * 1.25^(wave-1), final wave = 2x previous.
//     */
//    public void startNextWave() {
//    }
//
//    /**
//     * Calculates the difficulty (wave point budget) for a given wave number.
//     * Each wave is 25% harder; final wave is 2x the previous.
//     */
//    private float calculateWaveDifficulty(int wave) {
//        return 0;
//    }
//
//    /**
//     * Spawns zombies to fill the wave's point budget.
//     * Zombies are selected randomly, weighted by their wavePointCost.
//     */
//    private void spawnWaveZombies(float waveDifficulty) {
//
//    }
//
//    /**
//     * Selects a random zombie from the available list that fits the budget.
//     * Uses weighted random selection based on zombie weight property.
//     */
//    private Zombie selectRandomZombie(List<Zombie> available, float remainingBudget) {
//        return null;
//    }
//
//    /**
//     * Records damage dealt to current wave zombies (for wave progression).
//     */
//    public void recordDamage(int damage) {
//        damageDealt += damage;
//    }
//
//    public void spawnZombie(String zombieType, int lane) {
//
//    }
//
//    public void applyChapterRules(Chapter chapter) {
//        this.chapterContext = chapter;
//    }
//
//    public int getCurrentWave() {
//        return currentWave;
//    }
//
//    public boolean isFinalWave() {
//        return isFinalWave;
//    }
//
//    public boolean isWaveInProgress() {
//        return waveInProgress;
//    }
//
//    public void setTotalWaves(int totalWaves) {
//        this.totalWaves = totalWaves;
//    }
//
//    public void setBaseDifficulty(float baseDifficulty) {
//        this.baseDifficulty = baseDifficulty;
//    }
//}