package model.plant.ability;

import model.enums.PlantCategory;
import model.enums.PlantState;
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

    /**
     * @return true if the cell at {@code (row, col)} is a water tile.
     *         Used by Lily Pad (and other aquatic plants) to decide
     *         where clones / platforms may be placed.
     */
    boolean isWaterTile(int row, int col);

    // --- Mutations ---

    /** Spawns a projectile at the given pixel position. */
    Projectile spawnProjectile(Projectile projectile, float x, float y);

    /** Drops a sun item at the given pixel position. */
    void spawnSun(Sun sun);

    /** Directly adds {@code amount} sun to the player's bank (no item drop). */
    void addSun(int amount);

    /** Applies damage to a zombie (respects armor / elements on the zombie side). */
    void damageZombie(ZombieInstance zombie, int damage);

    /** Fire-damage variant (respects the zombie's fire multiplier). Default is unattributed. */
    default void damageZombieWithFire(ZombieInstance zombie, int damage) {
        if (zombie != null) zombie.takeFireDamage(damage);
    }

    /** Applies damage to a plant. */
    void damagePlant(PlantInstance plant, int damage);

    /** Removes the plant from the field. */
    void destroyPlant(PlantInstance plant);

    /**
     * Places an already-constructed plant instance onto the field at the
     * given grid cell.
     */
    boolean placePlant(PlantInstance plant, int row, int col);

    /**
     * Moves the given zombie to a different lane (row). Updates both the
     * zombie's grid position and its continuous Y coordinate, and
     * re-registers it on the map's per-cell zombie list.
     *
     * @return true if the move succeeded; false if the
     *         target row is out of bounds or the zombie has no grid position
     */
    boolean moveZombieToLane(ZombieInstance zombie, int newRow);

    /**
     * Pushes the given zombie backward (toward the zombie spawn point)
     * by {@code tiles} grid units. If the zombie is pushed past the
     * right edge of the map it is killed instantly.
     *
     * @param zombie the zombie to push
     * @param tiles the distance to push, in grid units (positive = backward)
     */
    void pushZombieBack(ZombieInstance zombie, float tiles);

    /**
     * Triggers the plant-food effect of every plant on the field that
     * shares the given category. Used by all mint plants.
     */
    void triggerFamilyPlantFood(PlantCategory family);

    /**
     * Applies damage to every ice-terrain block inside the rectangle
     * centred on ({@code row}, {@code col}) with the given half-extents.
     */
    void damageIceInArea(int row, int col, int rowRadius, int colRadius, int damage);

    /**
     * Spawns a zombie at the ({@code col}, {@code row}) coords
     * based on the given {@code zombieDefinitionName}.
     */
    ZombieInstance spawnZombieAt(String zombieDefinitionName, int row, int col);

    /**
     * Removes the given {@code zombie} from the map.
     */
    void removeZombie(ZombieInstance zombie);

    /**
     * Duration in seconds of the plant's presentation clip for {@code presentation}
     * (e.g. {@link PlantState#ATTACKING}), or {@code 0} if unknown.
     */
    default float plantPresentationDuration(PlantInstance plant, PlantState presentation) {
        return 0f;
    }
}
