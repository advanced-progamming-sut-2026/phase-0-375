package model.game.wave;

import model.game.wave.spawn.SingleSpawnStrategy;
import model.game.wave.spawn.SpawnStrategy;
import model.zombie.definition.Zombie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single zombie entry within a model.game.wave.
 * Defines which zombie, how many, and on which lane(s) it spawns.
 */
public class WaveZombieEntry {
    private List<ZombieSpawnCandidate> pool;
    private int minCount;
    private int maxCount;
    private int[] allowedLanes;

    /** Delay for the first spawn. */
    private float minSpawnDelay;
    private float maxSpawnDelay;

    /** Delay interval between consecutive spawns in this entry. */
    private float minSpawnInterval;
    private float maxSpawnInterval;

    private SpawnStrategy pattern; /* How the spawns are distributed. */

    /** For {@link model.enums.SpawnPatternType#STREAM}: total duration over which the count is spread (seconds). */
    private float streamDurationSeconds;

    // -- Builder --

    public WaveZombieEntry() {
        this.pool = new ArrayList<>();
        this.minCount = 1;
        this.maxCount = 1;
        this.minSpawnDelay = 0f;
        this.maxSpawnDelay = 0f;
        this.minSpawnInterval = 1f;
        this.maxSpawnInterval = 1f;
        this.pattern = null;
        this.streamDurationSeconds = 10f;
    }

    private WaveZombieEntry(Builder b) {
        this.pool = new ArrayList<>(b.pool);
        if (pool.isEmpty()) {
            throw new IllegalStateException("WaveZombieEntry pool must not be empty");
        }
        this.minCount = b.minCount;
        this.maxCount = Math.max(b.minCount, b.maxCount);
        this.allowedLanes = b.allowedLanes == null ? null : b.allowedLanes.clone();
        this.minSpawnDelay = b.minSpawnDelay;
        this.maxSpawnDelay = Math.max(b.minSpawnDelay, b.maxSpawnDelay);
        this.minSpawnInterval = b.minSpawnInterval;
        this.maxSpawnInterval = Math.max(b.minSpawnInterval, b.maxSpawnInterval);
        this.pattern = Objects.requireNonNull(b.pattern);
        this.streamDurationSeconds = b.streamDurationSeconds;
    }

    // -- Getters --

    public List<ZombieSpawnCandidate> getPool() {
        return Collections.unmodifiableList(pool);
    }

    public int getMinCount() {
        return minCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public int[] getAllowedLanes() {
        return allowedLanes == null ? null : allowedLanes.clone();
    }

    public float getMinSpawnDelay() {
        return minSpawnDelay;
    }

    public float getMaxSpawnDelay() {
        return maxSpawnDelay;
    }

    public float getMinSpawnInterval() {
        return minSpawnInterval;
    }

    public float getMaxSpawnInterval() {
        return maxSpawnInterval;
    }

    public SpawnStrategy getPattern() {
        return pattern;
    }

    public float getStreamDurationSeconds() {
        return streamDurationSeconds;
    }

    // Setters (primarily for Jackson deserialization).

    public void setPool(List<ZombieSpawnCandidate> pool) {
        this.pool = pool == null ? new ArrayList<>() : new ArrayList<>(pool);
    }

    public void setMinCount(int minCount) {
        if (minCount < 0) throw new IllegalArgumentException("minCount < 0");
        this.minCount = minCount;
    }

    public void setMaxCount(int maxCount) {
        if (maxCount < 0) throw new IllegalArgumentException("maxCount < 0");
        this.maxCount = maxCount;
    }

    public void setAllowedLanes(int[] allowedLanes) {
        this.allowedLanes = allowedLanes == null ? null : allowedLanes.clone();
    }

    public void setMinSpawnDelay(float minSpawnDelay) {
        this.minSpawnDelay = minSpawnDelay;
    }

    public void setMaxSpawnDelay(float maxSpawnDelay) {
        this.maxSpawnDelay = maxSpawnDelay;
    }

    public void setMinSpawnInterval(float minSpawnInterval) {
        this.minSpawnInterval = minSpawnInterval;
    }

    public void setMaxSpawnInterval(float maxSpawnInterval) {
        this.maxSpawnInterval = maxSpawnInterval;
    }

    public void setPattern(SpawnStrategy pattern) {
        this.pattern = Objects.requireNonNull(pattern);
    }

    public void setStreamDurationSeconds(float streamDurationSeconds) {
        this.streamDurationSeconds = streamDurationSeconds;
    }

    // ------------------------------------------------------------------
    // Internal helpers used by Wave when realizing the entry
    // ------------------------------------------------------------------

    public int[] resolveAllowedLanes(int rowCount) {
        if (allowedLanes != null && allowedLanes.length > 0) {
            return allowedLanes;
        }
        int[] all = new int[rowCount];
        for (int i = 0; i < rowCount; i++) all[i] = i;
        return all;
    }

    boolean isRealizable() {
        return pool != null && !pool.isEmpty() && maxCount > 0;
    }

    @Override
    public String toString() {
        return "WaveZombieEntry{pool=" + pool.size()
                + ", count=[" + minCount + ".." + maxCount + "]"
                + ", delay=[" + minSpawnDelay + ".." + maxSpawnDelay + "]"
                + ", interval=[" + minSpawnInterval + ".." + maxSpawnInterval + "]"
                + ", pattern=" + pattern
                + ", lanes=" + Arrays.toString(allowedLanes) + '}';
    }

    // -- Builder --

    public static final class Builder {
        private final List<ZombieSpawnCandidate> pool = new ArrayList<>();
        private int minCount = 1;
        private int maxCount = 1;
        private int[] allowedLanes = null;
        private float minSpawnDelay = 0f;
        private float maxSpawnDelay = 0f;
        private float minSpawnInterval = 1f;
        private float maxSpawnInterval = 1f;
        private SpawnStrategy pattern = null;
        private float streamDurationSeconds = 10f;

        public Builder() {}

        public Builder(Zombie singleZombie) {
            addCandidate(singleZombie, 1.0);
        }

        public Builder addCandidate(Zombie zombie, double weight) {
            pool.add(new ZombieSpawnCandidate(zombie, weight));
            return this;
        }

        public Builder addCandidate(ZombieSpawnCandidate candidate) {
            pool.add(Objects.requireNonNull(candidate));
            return this;
        }

        public Builder setCountRange(int min, int max) {
            this.minCount = min; this.maxCount = max; return this;
        }

        public Builder setAllowedLanes(int... lanes) {
            this.allowedLanes = lanes; return this;
        }

        public Builder setSpawnDelayRange(float min, float max) {
            this.minSpawnDelay = min; this.maxSpawnDelay = max; return this;
        }

        public Builder setSpawnIntervalRange(float min, float max) {
            this.minSpawnInterval = min; this.maxSpawnInterval = max; return this;
        }

        public Builder setPattern(SpawnStrategy pattern) {
            this.pattern = pattern; return this;
        }

        public Builder setStreamDurationSeconds(float seconds) {
            this.streamDurationSeconds = seconds; return this;
        }

        public WaveZombieEntry build() {
            return new WaveZombieEntry(this);
        }
    }
}