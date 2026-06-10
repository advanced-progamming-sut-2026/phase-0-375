package model.collection;

import model.plant.definition.Plant;
import model.zombie.definition.Zombie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds every plant and zombie definition in the game,
 * and knows which ones the current user has unlocked.
 */
public class Collection {
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
        this.unlockedPlants = unlockedPlants;
        this.unlockedZombies = unlockedZombies;
    }

    // --- Registration ---

    public void registerPlant(Plant plant) {

    }

    public void registerZombie(Zombie zombie) {

    }

    // --- Queries ---

    /** Returns only the plants the user has unlocked. */
    public List<Plant> getOwnedPlants() {
        return null;
    }

    /** Returns all plants in the game. */
    public List<Plant> getAllPlants() {
        return null;
    }

    /** Returns only the zombies the user has unlocked. */
    public List<Zombie> getOwnedZombies() {
        return null;
    }

    /** Returns all zombies in the game. */
    public List<Zombie> getAllZombies() {
        return null;
    }

    public Plant getPlant(String name) {
        return null;
    }

    public Zombie getZombie(String name) {
        return null;
    }

    public boolean ownsPlant(String name) {
        return false;
    }

    public boolean ownsZombie(String name) {
        return false;
    }

    public int getPlantLevel(String name) {
        return 0;
    }

    // --- Operations ---

    /**
     * Unlocks a plant for the user.
     */
    public void unlockPlant(String name) {

    }

    /**
     * Levels up a plant.
     *
     * @return true if the upgrade was applied
     */
    public boolean upgradePlant(String name) {
        return false;
    }
}
