package model.plant.ability;

import model.enums.PlantCategory;
import model.game.map.Point;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.item.Sun;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Read+act interface that plant abilities use to interact with the game
 * world. Passed by {@code PlantSystem} into every ability call so the
 * abilities themselves stay stateless and testable.
 */
public interface PlantAbilityContext {

    // --- Read queries ---

    /** @return current sun currency the player owns. */
    int getSunAmount();

    /** @return row count of the playing field. */
    int getRowCount();

    /** @return column count of the playing field. */
    int getColumnCount();

    /** @return the plant occupying the given cell, or {@code null}. */
    PlantInstance getPlantAt(int row, int col);

    /** @return every alive plant in the given lane. */
    List<PlantInstance> getPlantsInLane(int lane);

    /** @return every alive plant on the field. */
    List<PlantInstance> getAllPlants();

    /**
     * @return every alive zombie whose grid row matches {@code lane}.
     *         Sorted by column descending (closest to the house first).
     */
    List<ZombieInstance> getZombiesInLane(int lane);

    /**
     * @return every alive zombie within the rectangle centred on
     *         {@code (row, col)} with the given half-extents.
     */
    List<ZombieInstance> getZombiesInArea(int row, int col, int rowRadius, int colRadius);

    /** @return true if at least one alive zombie is in the given lane. */
    boolean hasZombieInLane(int lane);

    /** @return true if at least one alive zombie is adjacent to the given cell. */
    boolean hasAdjacentZombie(int row, int col);

    /** @return true if the level is a night / dark-ages level. */
    boolean isNightLevel();

    // --- Mutations ---

    /** Spawns a projectile at the given pixel position. */
    Projectile spawnProjectile(Projectile projectile, float x, float y);

    /** Drops a sun item at the given pixel position. */
    void spawnSun(Sun sun);

    /** Directly adds {@code amount} sun to the player's bank (no item drop). */
    void addSun(int amount);

    /** Applies damage to a zombie (respects armor / elements on the zombie side). */
    void damageZombie(ZombieInstance zombie, int damage);

    /** Applies damage to a plant. */
    void damagePlant(PlantInstance plant, int damage);

    /** Removes the plant from the field. */
    void destroyPlant(PlantInstance plant);

    /**
     * Triggers the plant-food effect of every plant on the field that
     * shares the given category. Used by all mint plants.
     */
    void triggerFamilyPlantFood(PlantCategory family);
}
