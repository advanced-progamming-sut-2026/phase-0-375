package model.zombie.behavior;

import model.event.GameEvent;
import model.game.map.Cell;
import model.item.Sun;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * {@link ZombieBehavior}s use this interface to interact with the game world.
 */
public interface BehaviorContext {

    // --- Active suns ---

    /** @return current sun balance. */
    int getSunAmount();

    /**
     * Deducts {@code amount} from the player's sun reserve.
     *
     * @return true if the player had enough sun and the deduction succeeded, false otherwise.
     */
    boolean spendSun(int amount);

    /** Adds {@code amount} directly to the player's sun reserve. */
    void addSun(int amount);

    /** @return a list of suns currently on the field. */
    List<Sun> getActiveSuns();

    /** Removes a sun from the field without adding its value to the player's reserve. */
    void removeSun(Sun sun);

    /** Drops a new sun on the field for the player to collect. */
    void spawnSun(Sun sun);

    // --- Zombies (spawning) ---

    /** Creates a new {@link ZombieInstance} of the given {@code zombieName}. */
    ZombieInstance spawnZombieAt(String zombieName, int row, int col);

    // --- Plants ---

    /**
     * @return the {@link PlantInstance} occupying the MAIN layer at the given grid cell,
     * or null if the cell is empty.
     */
    PlantInstance getPlantAt(int row, int col);

    /** @return a list of {@link PlantInstance}s in the given lane (row), left-to-right. */
    List<PlantInstance> getPlantsInLane(int lane);

    /** Applies {@code damage} to the {@code plant}. */
    void damagePlant(PlantInstance plant, int damage);

    // --- Zombies ---

    /** @return a list of {@link ZombieInstance}s in the given lane (row). */
    List<ZombieInstance> getZombiesInLane(int lane);

    /** Applies {@code damage} to the {@code zombie}. */
    void damageZombie(ZombieInstance zombie, int damage);

    // --- Game map ---

    /** @return number of rows on the map. */
    int getRowCount();

    /** @return number of columns on the map. */
    int getColumnCount();

    /** @return the {@link Cell} at the given coordinates. */
    Cell getCellAt(int row, int col);
}
