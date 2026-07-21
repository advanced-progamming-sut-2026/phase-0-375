package model.game.level.minigame.vasebreaker;

import java.util.ArrayList;
import java.util.List;

/**
 * Tunable configuration for one Vase Breaker stage, populated from the
 * vase-specific keys in minigames.json (see MiniGameDataEntry /
 * MiniGameRegistry).
 */
public class VaseBreakerSettings {

    /** How many of the rightmost lawn columns hold vases. */
    private int vaseColumns = 3;
    /** Vases with random contents: empty, a normal zombie, or a seed packet. */
    private int randomVaseCount;
    /** Vases guaranteed to contain a seed packet. */
    private int seedVaseCount;
    /** Vases guaranteed to contain the giant-vase zombie. */
    private int giantVaseCount;
    /** Relative odds for what a random vase holds (clamped to >= 0). */
    private float randomEmptyWeight = 1f;
    private float randomZombieWeight = 2f;
    private float randomSeedWeight = 1f;
    /** Seconds a revealed seed packet stays plantable. */
    private float seedPacketExpirySeconds = 15f;
    /** Zombie definition hidden inside giant vases. */
    private String giantVaseZombie = "ZombieGargantuar";
    /** Zombie definition names drawn from when a random vase holds a zombie. */
    private List<String> zombiePool = new ArrayList<>();
    /** Plant definition names drawn from for seed packets. */
    private List<String> plantPool = new ArrayList<>();

    public int getVaseColumns() { return vaseColumns; }
    public void setVaseColumns(int vaseColumns) { this.vaseColumns = vaseColumns; }

    public int getRandomVaseCount() { return randomVaseCount; }
    public void setRandomVaseCount(int randomVaseCount) { this.randomVaseCount = randomVaseCount; }

    public int getSeedVaseCount() { return seedVaseCount; }
    public void setSeedVaseCount(int seedVaseCount) { this.seedVaseCount = seedVaseCount; }

    public int getGiantVaseCount() { return giantVaseCount; }
    public void setGiantVaseCount(int giantVaseCount) { this.giantVaseCount = giantVaseCount; }

    public float getRandomEmptyWeight() { return randomEmptyWeight; }
    public void setRandomEmptyWeight(float randomEmptyWeight) { this.randomEmptyWeight = Math.max(0f, randomEmptyWeight); }

    public float getRandomZombieWeight() { return randomZombieWeight; }
    public void setRandomZombieWeight(float randomZombieWeight) { this.randomZombieWeight = Math.max(0f, randomZombieWeight); }

    public float getRandomSeedWeight() { return randomSeedWeight; }
    public void setRandomSeedWeight(float randomSeedWeight) { this.randomSeedWeight = Math.max(0f, randomSeedWeight); }

    /** Sum of the random-vase outcome weights. */
    public float totalRandomWeight() {
        return randomEmptyWeight + randomZombieWeight + randomSeedWeight;
    }

    public float getSeedPacketExpirySeconds() { return seedPacketExpirySeconds; }
    public void setSeedPacketExpirySeconds(float seedPacketExpirySeconds) { this.seedPacketExpirySeconds = seedPacketExpirySeconds; }

    public String getGiantVaseZombie() { return giantVaseZombie; }
    public void setGiantVaseZombie(String giantVaseZombie) {
        if (giantVaseZombie != null && !giantVaseZombie.isBlank()) {
            this.giantVaseZombie = giantVaseZombie;
        }
    }

    public List<String> getZombiePool() { return zombiePool; }
    public void setZombiePool(List<String> zombiePool) {
        this.zombiePool = zombiePool == null ? new ArrayList<>() : new ArrayList<>(zombiePool);
    }

    public List<String> getPlantPool() { return plantPool; }
    public void setPlantPool(List<String> plantPool) {
        this.plantPool = plantPool == null ? new ArrayList<>() : new ArrayList<>(plantPool);
    }
}
