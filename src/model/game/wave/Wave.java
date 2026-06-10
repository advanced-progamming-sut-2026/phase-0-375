package model.game.wave;

import model.enums.WaveState;

import java.util.List;

/**
 * Represents a single model.game.wave in a level.
 */
public class Wave {
    private int waveNumber;
    private WaveState state;
    private List<WaveZombieEntry> zombieEntries;
    private float spawnInterval;       // seconds between zombie spawns within the model.game.wave
    private float startDelay;          // delay before this model.game.wave starts after previous model.game.wave is cleared
    private boolean isFinalWave;       // last model.game.wave of the level

    public Wave(int waveNumber, WaveState state, List<WaveZombieEntry> zombieEntries,
                float spawnInterval, float startDelay, boolean isHugeWave, boolean isFinalWave) {
        this.waveNumber = waveNumber;
        this.state = state;
        this.zombieEntries = zombieEntries;
        this.spawnInterval = spawnInterval;
        this.startDelay = startDelay;
        this.isFinalWave = isFinalWave;
    }

    public void startWave() {

    }

    public boolean hasNextZombie() {
        return false;
    }

    public WaveZombieEntry getNextZombie() {
        return null;
    }

    public boolean isWaveCleared() {
        return false;
    }

    public void markComplete() {

    }

    public void markCleared() {

    }

    // --- Getters ---

    public int getWaveNumber() {
        return waveNumber;
    }

    public WaveState getState() {
        return state;
    }

    public List<WaveZombieEntry> getZombieEntries() {
        return zombieEntries;
    }

    public float getSpawnInterval() {
        return spawnInterval;
    }

    public float getStartDelay() {
        return startDelay;
    }

    public boolean isFinalWave() {
        return isFinalWave;
    }

    // --- Setters ---

    public void setWaveNumber(int waveNumber) {
        this.waveNumber = waveNumber;
    }

    public void setState(WaveState state) {
        this.state = state;
    }

    public void setZombieEntries(List<WaveZombieEntry> zombieEntries) {
        this.zombieEntries = zombieEntries;
    }

    public void setSpawnInterval(float spawnInterval) {
        this.spawnInterval = spawnInterval;
    }

    public void setStartDelay(float startDelay) {
        this.startDelay = startDelay;
    }

    public void setFinalWave(boolean finalWave) {
        isFinalWave = finalWave;
    }
}
