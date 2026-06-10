package model.game.wave;

import java.util.List;

/**
 * Manages the progression of waves during a level.
 */
public class WaveManager {
    private List<Wave> waves;
    private int currentWaveIndex;
    private float waveTimer;
    private boolean allWavesSpawned;
    private boolean allWavesCleared;

    public WaveManager(List<Wave> waves) {
        this.waves = waves;
        this.currentWaveIndex = 0;
        this.waveTimer = 0;
        this.allWavesSpawned = false;
        this.allWavesCleared = false;
    }

    public void tick(float deltaTime) {

    }

    public void startNextWave() {

    }

    public Wave getCurrentWave() {
        return waves.get(currentWaveIndex);
    }

    public Wave getWave(int index) {
        return waves.get(index);
    }

    public boolean hasPendingWaves() {
        return false;
    }

    public boolean isAllWavesCleared() {
        return false;
    }

    public int getTotalWaveCount() {
        return waves.size();
    }

    public int getCurrentWaveIndex() {
        return currentWaveIndex;
    }

    public int getRemainingWaveCount() {
        return waves.size() - currentWaveIndex - 1;
    }
}
