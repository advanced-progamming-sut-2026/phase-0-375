package model.game.wave;

import model.zombie.definition.Zombie;

import java.util.Objects;

/**
 * One candidate in a {@link WaveZombieEntry}'s weighted zombie pool.
 * A wave entry may define multiple candidates for spawning. A candidate is chose
 * based on their weight and randomness.
 */
public final class ZombieSpawnCandidate {

    private final Zombie zombieDefinition;
    private final double weight;

    public ZombieSpawnCandidate(Zombie zombieDefinition, double weight) {
        if (zombieDefinition == null)
            throw new IllegalArgumentException("Zombie definition cannot be null.");

        this.zombieDefinition = zombieDefinition;
        if (weight < 0.0) {
            throw new IllegalArgumentException("weight must be >= 0.0 (got " + weight + ")");
        }
        this.weight = weight;
    }

    /** Convenience constructor — defaults weight to {@code 1.0}. */
    public ZombieSpawnCandidate(Zombie zombieDefinition) {
        this(zombieDefinition, 1.0);
    }

    public Zombie getZombieDefinition() {
        return zombieDefinition;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZombieSpawnCandidate)) return false;
        ZombieSpawnCandidate that = (ZombieSpawnCandidate) o;
        return Double.compare(that.weight, weight) == 0
                && zombieDefinition.equals(that.zombieDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zombieDefinition, weight);
    }

    @Override
    public String toString() {
        return "ZombieSpawnCandidate{zombie=" + zombieDefinition.getName()
                + ", weight=" + weight + '}';
    }
}
