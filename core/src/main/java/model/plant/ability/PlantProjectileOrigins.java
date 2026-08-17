package model.plant.ability;

import model.game.map.FloatPoint;
import model.plant.instance.PlantInstance;

/**
 * Lookup for a projectile spawn point in grid units.
 * Returns {@code null} when unknown so callers can fall back to the plant cell.
 */
@FunctionalInterface
public interface PlantProjectileOrigins {

    PlantProjectileOrigins NONE = plant -> null;

    /**
     * @param plant the plant that is about to fire
     * @return continuous grid position (column, row), or {@code null} if unknown
     */
    FloatPoint origin(PlantInstance plant);
}
