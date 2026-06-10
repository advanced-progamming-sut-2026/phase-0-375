package model.game.wave;

import model.zombie.definition.Zombie;

/**
 * Represents a single zombie entry within a model.game.wave.
 * Defines which zombie, how many, and on which lane(s) it spawns.
 */
public class WaveZombieEntry {
    private Zombie zombieDefinition;
    private int count;                  // how many of this zombie type in this model.game.wave
    private int[] spawnLanes;           // lane indices where this zombie can spawn (null = any lane)
    private float spawnDelay;           // delay after model.game.wave start before this entry begins spawning

    public WaveZombieEntry(Zombie zombieDefinition, int count, int[] spawnLanes, float spawnDelay) {
        this.zombieDefinition = zombieDefinition;
        this.count = count;
        this.spawnLanes = spawnLanes;
        this.spawnDelay = spawnDelay;
    }

    // --- Getters ---

    public Zombie getZombieDefinition() { return zombieDefinition; }

    public int getCount() { return count; }

    public int[] getSpawnLanes() { return spawnLanes; }

    public float getSpawnDelay() { return spawnDelay; }

    // --- Setters ---

    public void setZombieDefinition(Zombie zombieDefinition) {
        this.zombieDefinition = zombieDefinition;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setSpawnLanes(int[] spawnLanes) {
        this.spawnLanes = spawnLanes;
    }

    public void setSpawnDelay(float spawnDelay) {
        this.spawnDelay = spawnDelay;
    }
}
