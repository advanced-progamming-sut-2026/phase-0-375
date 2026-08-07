package model.game.level.minigame.beghouled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tunable configuration for one Beghouled stage, populated from the
 * Beghouled specific keys in minigames.json
 */
public class BeghouledSettings {

    /** A board-wide plant upgrade purchasable with sun. */
    public static class UpgradeRule {
        private final String from;
        private final String to;
        private final int cost;

        public UpgradeRule(String from, String to, int cost) {
            this.from = from;
            this.to = to;
            this.cost = cost;
        }

        public String getFrom() { return from; }
        public String getTo() { return to; }
        public int getCost() { return cost; }
    }

    /** The five plant types that fill the board. */
    private final List<String> plantTypes = new ArrayList<>();
    /** Available upgrades for this stage. */
    private final List<UpgradeRule> upgrades = new ArrayList<>();
    /** Zombies drawn from (uniformly) for the endless attack. */
    private final List<String> zombiePool = new ArrayList<>();
    /** Matches (3 or more) needed to win the stage. */
    private int matchTarget = 15;
    /** Seconds before the first zombie spawns. */
    private float firstSpawnDelaySeconds = 15f;
    /** Starting seconds between zombie spawns. */
    private float spawnIntervalSeconds = 10f;
    /** Fastest the spawn interval can get. */
    private float minSpawnIntervalSeconds = 5f;
    /** Seconds shaved off the interval after every spawn. */
    private float spawnIntervalDecaySeconds = 0.25f;

    public List<String> getPlantTypes() {
        return Collections.unmodifiableList(plantTypes);
    }

    public void addPlantType(String plant) {
        if (plant != null && !plant.isBlank()) {
            plantTypes.add(plant);
        }
    }

    public List<UpgradeRule> getUpgrades() {
        return Collections.unmodifiableList(upgrades);
    }

    public void addUpgrade(String from, String to, int cost) {
        if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
            upgrades.add(new UpgradeRule(from, to, cost));
        }
    }

    /** Finds the upgrade whose source plant matches, case-insensitively. */
    public UpgradeRule findUpgrade(String from) {
        if (from == null) {
            return null;
        }
        for (UpgradeRule rule : upgrades) {
            if (rule.getFrom().equalsIgnoreCase(from)) {
                return rule;
            }
        }
        return null;
    }

    public List<String> getZombiePool() {
        return Collections.unmodifiableList(zombiePool);
    }

    public void addZombie(String zombie) {
        if (zombie != null && !zombie.isBlank()) {
            zombiePool.add(zombie);
        }
    }

    public int getMatchTarget() {
        return matchTarget;
    }

    public void setMatchTarget(int matchTarget) {
        this.matchTarget = matchTarget;
    }

    public float getFirstSpawnDelaySeconds() {
        return firstSpawnDelaySeconds;
    }

    public void setFirstSpawnDelaySeconds(float firstSpawnDelaySeconds) {
        this.firstSpawnDelaySeconds = firstSpawnDelaySeconds;
    }

    public float getSpawnIntervalSeconds() {
        return spawnIntervalSeconds;
    }

    public void setSpawnIntervalSeconds(float spawnIntervalSeconds) {
        this.spawnIntervalSeconds = spawnIntervalSeconds;
    }

    public float getMinSpawnIntervalSeconds() {
        return minSpawnIntervalSeconds;
    }

    public void setMinSpawnIntervalSeconds(float minSpawnIntervalSeconds) {
        this.minSpawnIntervalSeconds = minSpawnIntervalSeconds;
    }

    public float getSpawnIntervalDecaySeconds() {
        return spawnIntervalDecaySeconds;
    }

    public void setSpawnIntervalDecaySeconds(float spawnIntervalDecaySeconds) {
        this.spawnIntervalDecaySeconds = spawnIntervalDecaySeconds;
    }
}
