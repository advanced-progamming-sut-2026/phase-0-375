package model.game.wave;

import model.app.App;
import model.enums.WaveManagerPhase;
import model.enums.WaveState;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the progression of waves during a level.
 */
public class WaveManager implements Tickable {
    private List<Wave> waves;
    private int currentWaveIndex;

    private WaveManagerPhase phase;

    private float interWaveTimer;
    private GameModel gameModel;

    /** Cumulative max HP of every zombie spawned by the current wave. */
    private int currentWaveTotalHP;
    /** HP of the current wave already depleted (sum of dead zombies' max HP). */
    private int currentWaveDepletedHP;

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

        // Track HP depletion for the 75% rule.
        updateWaveHPAccounting();

        // Spec: next wave starts when 75% of current wave's HP is depleted.
        boolean hpThresholdMet = currentWaveTotalHP > 0
                && currentWaveDepletedHP >= 0.75 * currentWaveTotalHP;

        // When the current wave's spawning phase is complete AND no live
        // zombies remain on the map, mark it cleared and either advance
        // to the next wave or finish the level.
        if (current.getState() == WaveState.COMPLETE) {
            boolean mapClear = (gameModel == null) || gameModel.getZombieCount() == 0;
            if (mapClear) {
                current.markCleared();
                advanceWaveIndex();
            }
        } else if (hpThresholdMet && hasPendingWaves()) {
            // 75% HP depleted — fire the next wave immediately.
            current.markCleared();
            advanceWaveIndex();
        }
    }

    /** Recomputes depleted HP by scanning the current wave's spawned zombies. */
    private void updateWaveHPAccounting() {
        if (gameModel == null) return;
        if (currentWaveTotalHP == 0) return;
        int depleted = 0;
        for (ZombieInstance z : gameModel.getZombies()) {
            if (z == null || z.getDefinition() == null) continue;
            // Only count zombies belonging to the current wave — approximate
            // by counting all current zombies; dead ones have already been
            // removed from the list, so depleted = total - alive.
            depleted += Math.max(0, z.getDefinition().getBaseHP() - z.getCurrentHP());
        }
        currentWaveDepletedHP = depleted;
    }

    /** Advances to the next wave, or finishes the level if none remain. */
    private void advanceWaveIndex() {
        if (hasPendingWaves()) {
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
        if (!hasPendingWaves()) {
            phase = WaveManagerPhase.LEVEL_DONE;
            return;
        }
        Wave next = waves.get(currentWaveIndex);
        next.startWave();
        phase = WaveManagerPhase.ACTIVE_WAVE;
        // Reset HP accounting for the new wave.
        currentWaveTotalHP = computeWaveMaxHP(next);
        currentWaveDepletedHP = 0;
        if (gameModel != null) {
            gameModel.onWaveStarted(next);
        }
    }

    /** Sums the max HP of every zombie this wave is expected to spawn. */
    private int computeWaveMaxHP(Wave wave) {
        if (wave == null) return 0;
        int total = 0;
        for (EntryRuntime rt : wave.getRuntimeEntries()) {
            for (model.game.wave.ZombieSpawnCandidate c : rt.getWaveZombieEntry().getPool()) {
                if (c.getZombieDefinition() != null) {
                    total += c.getZombieDefinition().getBaseHP();
                }
            }
        }
        return total;
    }

    public Wave getCurrentWave() {
        if (currentWaveIndex >= waves.size()) return null;
        return waves.get(currentWaveIndex);
    }

    public Wave getWave(int index) {
        return waves.get(index);
    }

    public boolean hasPendingWaves() {
        if (currentWaveIndex < waves.size() - 1) return true;
        Wave current = getCurrentWave();
        return current != null && current.getState() != WaveState.CLEARED;
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

    void setPhase(WaveManagerPhase phase) { this.phase = phase; }
}
