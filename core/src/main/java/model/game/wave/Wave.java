package model.game.wave;

import model.enums.Chapter;
import model.enums.WaveState;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.game.map.Point;
import model.zombie.definition.Zombie;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single model.game.wave in a level.
 */
public class Wave implements Tickable {

    /** Ancient Egypt: chance that a final-wave zombie arrives by tornado. */
    public static final double TORNADO_CHANCE = 0.2;

    /** Ancient Egypt: max columns ahead a tornado can drop a zombie. */
    public static final int TORNADO_MAX_COLUMNS_AHEAD = 4;

    private int waveNumber;
    private WaveState state;
    private float spawnInterval; // seconds between zombie spawns within the model.game.wave
    private float startDelay; // delay before this model.game.wave starts after previous model.game.wave is cleared
    private boolean isFinalWave; // last model.game.wave of the level
    private boolean isHugeWave; // triggers the "huge wave" banner

    /** Phase-2 budget: total wavePointCost of zombies this wave should spawn. 0 = scripted entries. */
    private int waveBudget;

    /** These objects encapsulate {@link WaveZombieEntry} in a wrapper class that stores other non-related
     *  data that {@link WaveZombieEntry} wouldn't concern.
     */
    private List<EntryRuntime> runtimeEntries;

    private GameModel gameModel; // set with setGameModel, used internally for spawning zombie instances
    private WaveRandomGenerator rng;
    private float waveClock;
    private int rowCount;

    /** Big Wave Beach: cells that may ambush when submerged during this wave. */
    private List<Point> lowTideTiles = List.of();

    public Wave(int waveNumber,
                List<EntryRuntime> zombieEntries,
                float startDelay,
                boolean isHugeWave,
                boolean isFinalWave) {
        this(waveNumber, zombieEntries, startDelay, isHugeWave, isFinalWave, new WaveRandomGenerator());
    }

    public Wave(int waveNumber,
                List<EntryRuntime> zombieEntries,
                float startDelay,
                boolean isHugeWave,
                boolean isFinalWave,
                long seed) {
        this(waveNumber, zombieEntries, startDelay, isHugeWave, isFinalWave, new WaveRandomGenerator(seed));
    }

    private Wave(int waveNumber,
                 List<EntryRuntime> zombieEntries,
                 float startDelay,
                 boolean isHugeWave,
                 boolean isFinalWave,
                 WaveRandomGenerator rng) {
        if (waveNumber < 1) throw new IllegalArgumentException("waveNumber must be >= 1");
        if (zombieEntries == null) throw new IllegalArgumentException("zombieEntries is null");
        this.waveNumber = waveNumber;
        this.runtimeEntries = new ArrayList<>(zombieEntries);
        this.startDelay = startDelay;
        this.isHugeWave = isHugeWave;
        this.isFinalWave = isFinalWave;
        this.rng = rng;
        this.state = WaveState.PENDING;
        this.waveClock = 0f;
    }

    // -- Tick --------

    @Override
    public void tick(float deltaTime) {
        if (state != WaveState.ACTIVE) return;
        waveClock += deltaTime;

        boolean anyAlive = false;
        for (EntryRuntime rtEntry : runtimeEntries) {
            WaveZombieEntry entry = rtEntry.getWaveZombieEntry();
            if (rtEntry.isExhausted()) continue;
            anyAlive = true;

            if (!rtEntry.isActivated()) {
                if (waveClock < rtEntry.getFirstSpawnAt()) continue;
                rtEntry.setActivated(true);
                rtEntry.setNextSpawnAt(waveClock);
            }

            entry.getPattern().tick(deltaTime);
        }

        // If every entry is exhausted, the wave's spawning phase is done.
        if (!anyAlive && state == WaveState.ACTIVE) {
            markComplete();
        }
    }

    public void startWave() {
        if (state != WaveState.PENDING)
            throw new IllegalStateException("startWave() called on wave in state " + state);
        if (gameModel != null)
            rowCount = gameModel.getMap().getRows();
        this.state = WaveState.ACTIVE;
        this.waveClock = 0f;

        float ts = gameModel == null ? 1f : gameModel.difficultyPenalty();
        for (EntryRuntime rtEntry : runtimeEntries) {
            WaveZombieEntry e = rtEntry.getWaveZombieEntry();
            e.setMinSpawnDelay(e.getMinSpawnDelay() * ts);
            e.setMaxSpawnDelay(e.getMaxSpawnDelay() * ts);
            e.setMinSpawnInterval(e.getMinSpawnInterval() * ts);
            e.setMaxSpawnInterval(e.getMaxSpawnInterval() * ts);
            e.setStreamDurationSeconds(e.getStreamDurationSeconds() * ts);
            rtEntry.setFirstSpawnAt(rng.nextFloat(e.getMinSpawnDelay(), e.getMaxSpawnDelay()));
            rtEntry.setRemainingSpawns(rng.nextInt(e.getMinCount(), e.getMaxCount()));
            rtEntry.setNextSpawnAt(rtEntry.getFirstSpawnAt());
            rtEntry.setActivated(false);
            rtEntry.setExhausted(rtEntry.getRemainingSpawns() <= 0 || !e.isRealizable());
            rtEntry.setGroupVolleyFired(false);
        }
    }

    public boolean hasNextZombie() {
        return false;
    }

    public WaveZombieEntry getNextZombie() {
        return null;
    }

    public boolean isWaveCleared() {
        return state == WaveState.CLEARED;
    }

    public boolean isSpawningDone() {
        return state == WaveState.COMPLETE || state == WaveState.CLEARED;
    }

    public boolean allEntriesExhausted() {
        for (EntryRuntime rt : runtimeEntries) {
            if (!rt.isExhausted()) return false;
        }
        return true;
    }

    public void markComplete() {
        if (state == WaveState.ACTIVE) this.state = WaveState.COMPLETE;
    }

    public void markCleared() {
        // ACTIVE → CLEARED: 75% kill early-advance clears before spawn-COMPLETE.
        if (state == WaveState.COMPLETE || state == WaveState.ACTIVE) {
            this.state = WaveState.CLEARED;
        }
    }

    // --- Getters ---

    public int getWaveNumber() {
        return waveNumber;
    }

    public WaveState getState() {
        return state;
    }

    public List<EntryRuntime> getRuntimeEntries() {
        return runtimeEntries;
    }

    public float getSpawnInterval() {
        return spawnInterval;
    }

    public float getStartDelay() {
        return startDelay * (gameModel == null ? 1f : gameModel.difficultyPenalty());
    }

    public boolean isHugeWave() {
        return isHugeWave;
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

    public void setRuntimeEntries(List<EntryRuntime> runtimeEntries) {
        this.runtimeEntries = runtimeEntries;
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

    public void setGameModel(GameModel gameModel) {
        this.gameModel = gameModel;
        if (gameModel != null)
            rowCount = gameModel.getMap().getRows();
    }

    public float getWaveClock() {
        return waveClock;
    }

    public WaveRandomGenerator getRng() {
        return rng;
    }

    public int getRowCount() {
        return rowCount;
    }

    /** @return this wave's wavePointCost budget (Phase-2); 0 = scripted entries. */
    public int getWaveBudget() {
        return waveBudget;
    }

    /** Sets this wave's wavePointCost budget (Phase-2). */
    public void setWaveBudget(int waveBudget) {
        this.waveBudget = waveBudget;
    }

    /** Big Wave Beach ambush cells for this wave; never null. */
    public List<Point> getLowTideTiles() {
        return lowTideTiles;
    }

    public void setLowTideTiles(List<Point> lowTideTiles) {
        this.lowTideTiles = lowTideTiles == null || lowTideTiles.isEmpty()
                ? List.of()
                : List.copyOf(lowTideTiles);
    }

    // -- spawn functions --------

    public void spawnOne(WaveZombieEntry entry) {
        int[] lanes = entry.resolveAllowedLanes(rowCount);
        int lane = rng.nextLane(lanes);
        spawnOnLane(entry, lane);
    }

    public void spawnOnLane(WaveZombieEntry entry, int lane) {
        if (gameModel == null) return;   // not wired up yet — silently skip
        Zombie z = rng.rollZombiePool(entry.getPool());
        if (z == null) return;
        // Ancient Egypt: final-wave zombies may ride in on a sandstorm and
        // touch down 1-4 columns ahead of the normal entry point.
        if (isFinalWave
                && gameModel.getChapter() == Chapter.ANCIENT_EGYPT
                && rng.nextBoolean(TORNADO_CHANCE)) {
            gameModel.queueSandstormSpawn(z, lane,
                    rng.nextInt(1, TORNADO_MAX_COLUMNS_AHEAD));
            return;
        }
        gameModel.spawnZombie(z, lane);
    }

    public void shuffle(int[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rng.nextInt(0, i);
            int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
    }
}
