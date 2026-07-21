package model.game.wave;

import model.enums.WaveManagerPhase;
import model.enums.WaveState;
import model.game.core.GameModel;
import model.game.core.Tickable;

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

        // When the current wave's spawning phase is complete AND no live
        // zombies remain on the map, mark it cleared and either advance
        // to the next wave or finish the level.
        if (current.getState() == WaveState.COMPLETE) {
            boolean mapClear = (gameModel == null) || gameModel.getZombieCount() == 0;
            if (mapClear) {
                current.markCleared();
                if (hasPendingWaves()) {
                    phase = WaveManagerPhase.WAITING_FOR_NEXT_WAVE;
                    interWaveTimer = waves.get(++currentWaveIndex).getStartDelay();
                } else {
                    phase = WaveManagerPhase.LEVEL_DONE;
                }
            }
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
