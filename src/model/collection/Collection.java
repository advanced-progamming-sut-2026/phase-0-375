package model.collection;

import model.plant.definition.Plant;
import model.zombie.definition.Zombie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds every plant and zombie definition in the game,
 * and knows which ones the current user has unlocked.
 */
public class Collection {

    public static final int MIN_PLANT_LEVEL = 1;
    public static final int MAX_PLANT_LEVEL = 4;

    private static final int SEED_COST_PER_LEVEL = 10;
    private static final int COIN_COST_PER_LEVEL = 500;

    /** All plants registered in the game, keyed by name. */
    private final Map<String, Plant> allPlants = new HashMap<>();

    /** All zombies registered in the game, keyed by name. */
    private final Map<String, Zombie> allZombies = new HashMap<>();

    /** Plant names the user has unlocked. */
    private Set<String> unlockedPlants;

    /** Zombie names the user has unlocked. */
    private Set<String> unlockedZombies;

    /** Current level of each plant the user owns. */
    private final Map<String, Integer> plantLevels = new HashMap<>();

    public Collection(Set<String> unlockedPlants, Set<String> unlockedZombies) {
        this.unlockedPlants = unlockedPlants != null ? unlockedPlants : new HashSet<>();
        this.unlockedZombies = unlockedZombies != null ? unlockedZombies : new HashSet<>();
    }

    // --- Registration ---

    public void registerPlant(Plant plant) {
        if (plant != null && plant.getName() != null) {
            allPlants.put(plant.getName(), plant);
        }
    }

    public void registerZombie(Zombie zombie) {
        if (zombie != null && zombie.getName() != null) {
            allZombies.put(zombie.getName(), zombie);
        }
    }

    /**
     * Hydrates known plant levels (e.g. from persisted user data).
     * Plants not present in {@code levels} default to {@link #MIN_PLANT_LEVEL}.
     */
    public void loadPlantLevels(Map<String, Integer> levels) {
        if (levels != null) {
            plantLevels.putAll(levels);
        }
    }

    /** Returns an unmodifiable snapshot of the tracked plant levels, for persistence. */
    public Map<String, Integer> exportPlantLevels() {
        return Collections.unmodifiableMap(new HashMap<>(plantLevels));
    }

    // --- Queries ---

    /** Returns only the plants the user has unlocked. */
    public List<Plant> getOwnedPlants() {
        List<Plant> result = new ArrayList<>();
        for (String name : unlockedPlants) {
            Plant plant = allPlants.get(name);
            if (plant != null) {
                result.add(plant);
            }
        }
        return result;
    }

    /** Returns all plants in the game. */
    public List<Plant> getAllPlants() {
        return new ArrayList<>(allPlants.values());
    }

    /** Returns only the zombies the user has unlocked. */
    public List<Zombie> getOwnedZombies() {
        List<Zombie> result = new ArrayList<>();
        for (String name : unlockedZombies) {
            Zombie zombie = allZombies.get(name);
            if (zombie != null) {
                result.add(zombie);
            }
        }
        return result;
    }

    /** Returns all zombies in the game. */
    public List<Zombie> getAllZombies() {
        return new ArrayList<>(allZombies.values());
    }

    public Plant getPlant(String name) {
        return name != null ? allPlants.get(name) : null;
    }

    public Zombie getZombie(String name) {
        return name != null ? allZombies.get(name) : null;
    }

    public boolean ownsPlant(String name) {
        return name != null && unlockedPlants.contains(name);
    }

    public boolean ownsZombie(String name) {
        return name != null && unlockedZombies.contains(name);
    }

    /**
     * @return the plant's current level, or 0 if the user does not own it
     */
    public int getPlantLevel(String name) {
        if (!ownsPlant(name)) {
            return 0;
        }
        return plantLevels.getOrDefault(name, MIN_PLANT_LEVEL);
    }

    /** Seed packets required to upgrade this plant to its next level. */
    public int getUpgradeSeedCost(String name) {
        int nextLevel = Math.max(getPlantLevel(name), MIN_PLANT_LEVEL) + 1;
        return SEED_COST_PER_LEVEL * (nextLevel - 1);
    }

    /** Coins required to upgrade this plant to its next level. */
    public int getUpgradeCoinCost(String name) {
        int nextLevel = Math.max(getPlantLevel(name), MIN_PLANT_LEVEL) + 1;
        return COIN_COST_PER_LEVEL * (nextLevel - 1);
    }

    // --- Operations ---

    /**
     * Unlocks a plant for the user, starting it at {@link #MIN_PLANT_LEVEL}.
     */
    public void unlockPlant(String name) {
        if (name == null || !allPlants.containsKey(name)) {
            return;
        }
        unlockedPlants.add(name);
        plantLevels.putIfAbsent(name, MIN_PLANT_LEVEL);
    }

    /**
     * Levels up a plant.
     *
     * @return true if the upgrade was applied
     */
    public boolean upgradePlant(String name) {
        if (!ownsPlant(name)) {
            return false;
        }
        int current = getPlantLevel(name);
        if (current >= MAX_PLANT_LEVEL) {
            return false;
        }
        plantLevels.put(name, current + 1);
        return true;
    }
}
