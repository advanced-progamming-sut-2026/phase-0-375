package model.game.wave;

import model.zombie.definition.Zombie;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Seeded random-number generator used by the wave runtime to roll
 * spawn timing, lane assignment, and zombie-type selection.
 */
public final class WaveRandomGenerator {

    private final Long seed;
    private java.util.Random seededRandom;

    public WaveRandomGenerator() {
        this.seed = null;
    }

    public WaveRandomGenerator(long seed) {
        this.seed = seed;
    }

    /** Returns the configured seed, or {@code null} if running unseeded. */
    public Long getSeed() {
        return seed;
    }

    private java.util.Random random() {
        if (seed == null) {
            return ThreadLocalRandom.current();
        }
        if (seededRandom == null) {
            seededRandom = new java.util.Random(seed);
        }
        return seededRandom;
    }

    /** Returns a float in [{@code min}, {@code max}]. */
    public float nextFloat(float min, float max) {
        if (min > max) {
            throw new IllegalArgumentException("min > max: " + min + " > " + max);
        }
        if (min == max) return min;
        return min + random().nextFloat() * (max - min);
    }

    /** Returns an int in [{@code min}, {@code max}]. */
    public int nextInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min > max: " + min + " > " + max);
        }
        return min + random().nextInt(max - min + 1);
    }

    /** Returns a lane index from the given array. */
    public int nextLane(int[] allowedLanes) {
        Objects.requireNonNull(allowedLanes, "allowedLanes");
        if (allowedLanes.length == 0) {
            throw new IllegalArgumentException("allowedLanes must not be empty");
        }
        return allowedLanes[random().nextInt(allowedLanes.length)];
    }

    /**
     * Returns a zombie definition from a weighted candidate pool.
     * Returns {@code null} if the pool is empty or all weights are zero.
     */
    public Zombie rollZombiePool(List<ZombieSpawnCandidate> pool) {
        if (pool == null || pool.isEmpty()) return null;

        double totalWeight = 0.0;
        for (ZombieSpawnCandidate c : pool) {
            totalWeight += c.getWeight();
        }
        if (totalWeight <= 0.0) return null;

        double roll = random().nextDouble() * totalWeight;
        double acc = 0.0;
        for (ZombieSpawnCandidate c : pool) {
            acc += c.getWeight();
            if (roll < acc) {
                return c.getZombieDefinition();
            }
        }

        return pool.getLast().getZombieDefinition();
    }

    /** returns a boolean with probability {@code p} (0..1). */
    public boolean nextBoolean(double p) {
        return random().nextDouble() < p;
    }
}