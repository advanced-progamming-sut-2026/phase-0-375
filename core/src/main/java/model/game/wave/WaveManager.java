package model.game.wave;

import model.enums.WaveManagerPhase;
import model.enums.WaveState;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Manages the progression of waves during a level.
 */
public class WaveManager implements Tickable {
    private List<Wave> waves;
    private int currentWaveIndex;

    private WaveManagerPhase phase;

    private float interWaveTimer;
    private GameModel gameModel;

    /** Planned spawns for the current wave (grows if budget spawns more). */
    private int currentWaveTotal;
    /** Current-wave zombies that have died. */
    private int currentWaveKilled;
    /** Living zombies spawned by the current wave. */
    private final Set<ZombieInstance> currentWaveLiving =
        Collections.newSetFromMap(new IdentityHashMap<>());

    public WaveManager(List<Wave> waves) {
        this(waves, null);
    }

    public WaveManager(List<Wave> waves, GameModel gameModel) {
        this.waves = waves == null ? new ArrayList<>() : new ArrayList<>(waves);
        this.gameModel = gameModel;
        this.currentWaveIndex = 0;
        if (this.waves.isEmpty()) {
            // Levels without scripted waves (e.g. Vase Breaker, I,Zombie)
            // are immediately "done": their zombies come from other sources.
            this.phase = WaveManagerPhase.LEVEL_DONE;
            this.interWaveTimer = 0f;
        } else {
            this.phase = WaveManagerPhase.WAITING_FOR_NEXT_WAVE;
            this.interWaveTimer = this.waves.getFirst().getStartDelay();
        }
        // Wire up the game model on every wave so they can spawn zombies
        if (gameModel != null) {
            for (Wave w : this.waves) w.setGameModel(gameModel);
        }
    }

    @Override
    public void tick(float deltaTime) {
        if (phase == WaveManagerPhase.LEVEL_DONE) return;

        if (gameModel != null && gameModel.getCurrentLevel() instanceof model.game.level.special.PlantWhatYouGetLevel lastStand
                && lastStand.isSetupPhase()) {
            return;
        }

        if (phase == WaveManagerPhase.WAITING_FOR_NEXT_WAVE) {
            interWaveTimer -= deltaTime;
            if (interWaveTimer <= 0f) {
                startNextWave();
            }
            return;
        }

        // ACTIVE_WAVE
        Wave current = getCurrentWave();
        current.tick(deltaTime);

        refreshWaveTotal(current);

        // Next wave when 75% of this wave's zombies are dead (never on last).
        boolean killThresholdMet = currentWaveTotal > 0
            && currentWaveKilled >= 0.75 * currentWaveTotal;

        // When the current wave's spawning phase is complete AND no live
        // zombies remain on the map, mark it cleared and either advance
        // to the next wave or finish the level.
        if (current.getState() == WaveState.COMPLETE) {
            boolean mapClear = (gameModel == null) || gameModel.getZombieCount() == 0;
            if (mapClear) {
                current.markCleared();
                advanceWaveIndex();
            }
        } else if (killThresholdMet && hasNextWave()) {
            current.markCleared();
            advanceWaveIndex();
        }
    }

    /** Wave-entry spawn (not plant-food / vase / etc.). */
    public void onWaveZombieSpawned(ZombieInstance zombie) {
        if (zombie == null || phase != WaveManagerPhase.ACTIVE_WAVE) {
            return;
        }
        currentWaveLiving.add(zombie);
        refreshWaveTotal(getCurrentWave());
    }

    /** Any removal: only kills that belong to the current wave move the meter. */
    public void onZombieRemoved(ZombieInstance zombie) {
        if (zombie != null && currentWaveLiving.remove(zombie)) {
            currentWaveKilled++;
        }
    }

    private void resetWaveZombieAccounting(Wave wave) {
        currentWaveLiving.clear();
        currentWaveKilled = 0;
        currentWaveTotal = plannedRemainingSpawns(wave);
    }

    private void refreshWaveTotal(Wave wave) {
        int remaining = plannedRemainingSpawns(wave);
        currentWaveTotal = Math.max(
            currentWaveTotal,
            currentWaveKilled + currentWaveLiving.size() + remaining);
    }

    private static int plannedRemainingSpawns(Wave wave) {
        if (wave == null) {
            return 0;
        }
        int n = 0;
        for (EntryRuntime rt : wave.getRuntimeEntries()) {
            n += Math.max(0, rt.getRemainingSpawns());
        }
        return n;
    }

    /** Advances to the next wave, or finishes the level if none remain. */
    private void advanceWaveIndex() {
        if (hasNextWave()) {
            phase = WaveManagerPhase.WAITING_FOR_NEXT_WAVE;
            interWaveTimer = waves.get(++currentWaveIndex).getStartDelay();
        } else {
            phase = WaveManagerPhase.LEVEL_DONE;
        }
    }

    public void startNextWave() {
        if (phase != WaveManagerPhase.WAITING_FOR_NEXT_WAVE) {
            throw new IllegalStateException("startNextWave() called in phase " + phase);
        }
        if (currentWaveIndex >= waves.size()) {
            phase = WaveManagerPhase.LEVEL_DONE;
            return;
        }
        Wave next = waves.get(currentWaveIndex);
        next.startWave();
        phase = WaveManagerPhase.ACTIVE_WAVE;
        resetWaveZombieAccounting(next);
        if (gameModel != null) {
            gameModel.onWaveStarted(next);
        }
    }

    public Wave getCurrentWave() {
        if (currentWaveIndex >= waves.size()) return null;
        return waves.get(currentWaveIndex);
    }

    public Wave getWave(int index) {
        return waves.get(index);
    }

    /** True when a later wave exists after {@link #currentWaveIndex}. */
    boolean hasNextWave() {
        return currentWaveIndex < waves.size() - 1;
    }

    public boolean hasPendingWaves() {
        return hasNextWave()
            || (getCurrentWave() != null && getCurrentWave().getState() != WaveState.CLEARED);
    }

    public boolean isLevelDone() {
        return phase == WaveManagerPhase.LEVEL_DONE;
    }

    public void advanceToNextWave() {
        if (currentWaveIndex < waves.size() - 1) {
            currentWaveIndex++;
        }
    }

    // -- Getters -------

    public int getTotalWaveCount() { return waves.size(); }
    public int getCurrentWaveIndex() { return currentWaveIndex; }
    public int getRemainingWaveCount() {
        return Math.max(0, waves.size() - currentWaveIndex - 1);
    }
    public List<Wave> getWaves() { return Collections.unmodifiableList(waves); }
    public WaveManagerPhase getPhase() { return phase; }

    /**
     * Level progress in {@code [0, 1]}: the bar is split into {@code n} equal
     * sections (one per wave). The current wave interpolates by zombies killed.
     */
    public float progress01() {
        return progress01(waves.size(), currentWaveIndex, phase, currentWaveFraction());
    }

    /**
     * Flag stops at the end of each section except the last: {@code 1/n},
     * {@code 2/n}, … {@code (n-1)/n}.
     */
    public float[] flagStops01() {
        return flagStops01(waves.size());
    }

    static float progress01(int waveCount, int index, WaveManagerPhase phase,
                            float currentFrac) {
        if (waveCount <= 0) {
            return 0f;
        }
        if (phase == WaveManagerPhase.LEVEL_DONE) {
            return 1f;
        }
        int idx = Math.max(0, Math.min(index, waveCount - 1));
        float section = idx;
        if (phase == WaveManagerPhase.ACTIVE_WAVE) {
            section += clamp01(currentFrac);
        }
        return Math.min(1f, section / waveCount);
    }

    static float[] flagStops01(int waveCount) {
        if (waveCount <= 1) {
            return new float[0];
        }
        float[] flags = new float[waveCount - 1];
        for (int i = 0; i < flags.length; i++) {
            flags[i] = (i + 1) / (float) waveCount;
        }
        return flags;
    }

    private float currentWaveFraction() {
        if (phase != WaveManagerPhase.ACTIVE_WAVE || currentWaveTotal <= 0) {
            return 0f;
        }
        return clamp01(currentWaveKilled / (float) currentWaveTotal);
    }

    private static float clamp01(float v) {
        if (v < 0f) {
            return 0f;
        }
        if (v > 1f) {
            return 1f;
        }
        return v;
    }

    void setPhase(WaveManagerPhase phase) { this.phase = phase; }

    /** Test/debug: current-wave kill fraction in {@code [0, 1]}. */
    float debugWaveKillFraction() {
        return currentWaveFraction();
    }

    void debugSetWaveCounts(int total, int killed) {
        currentWaveTotal = Math.max(0, total);
        currentWaveKilled = Math.max(0, Math.min(killed, currentWaveTotal));
    }
}
