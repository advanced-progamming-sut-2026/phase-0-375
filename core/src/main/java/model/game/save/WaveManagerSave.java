package model.game.save;

import model.enums.WaveManagerPhase;

import java.util.ArrayList;
import java.util.List;

/** Snapshot of {@link model.game.wave.WaveManager} progress. */
public class WaveManagerSave {
    private int currentWaveIndex;
    private WaveManagerPhase phase = WaveManagerPhase.WAITING_FOR_NEXT_WAVE;
    private float interWaveTimer;
    private int currentWaveTotal;
    private int currentWaveKilled;
    private float maxReportedProgress;
    private List<WaveSave> waves = new ArrayList<>();

    public int getCurrentWaveIndex() { return currentWaveIndex; }
    public void setCurrentWaveIndex(int currentWaveIndex) { this.currentWaveIndex = currentWaveIndex; }
    public WaveManagerPhase getPhase() { return phase; }
    public void setPhase(WaveManagerPhase phase) { this.phase = phase; }
    public float getInterWaveTimer() { return interWaveTimer; }
    public void setInterWaveTimer(float interWaveTimer) { this.interWaveTimer = interWaveTimer; }
    public int getCurrentWaveTotal() { return currentWaveTotal; }
    public void setCurrentWaveTotal(int currentWaveTotal) { this.currentWaveTotal = currentWaveTotal; }
    public int getCurrentWaveKilled() { return currentWaveKilled; }
    public void setCurrentWaveKilled(int currentWaveKilled) { this.currentWaveKilled = currentWaveKilled; }
    public float getMaxReportedProgress() { return maxReportedProgress; }
    public void setMaxReportedProgress(float maxReportedProgress) {
        this.maxReportedProgress = maxReportedProgress;
    }
    public List<WaveSave> getWaves() { return waves; }
    public void setWaves(List<WaveSave> waves) {
        this.waves = waves == null ? new ArrayList<>() : waves;
    }
}
