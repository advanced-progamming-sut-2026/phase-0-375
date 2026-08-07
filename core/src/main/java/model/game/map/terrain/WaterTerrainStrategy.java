package model.game.map.terrain;

import model.enums.PlacableLayer;
import model.enums.PlantTags;
import model.enums.ZombieBehaviorType;
import model.item.placeable.Placeable;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;
import model.game.map.Cell;
import model.plant.definition.Plant;

/**
 * Terrain strategy for water tiles - both deep water ({@code
 * GroundType.WATER}) and shallow low-tide tiles ({@code GroundType.LOW_TIDE}).
 */
public class WaterTerrainStrategy implements TerrainStrategy {

    public WaterTerrainStrategy() {

    }

    @Override
    public boolean canPlant(Plant plant, Cell cell) {
        if (plant == null) {
            return false;
        }
        // Aquatic plants sit directly in the water.
        if (plant.hasTag(PlantTags.WATER)) {
            return true;
        }
        // Non-aquatic plants need a platform (e.g. Lily Pad) already on
        // the GROUND layer of this cell. Graves cannot spawn on water cells,
        // so any GROUND-layer occupant here is treated as a valid platform.
        return cell.getPlaceable(PlacableLayer.GROUND) != null;
    }

    @Override
    public boolean isPassable(ZombieInstance zombie, Cell cell) {
        if (zombie == null) {
            return false;
        }
        // Swimming zombies dive through water, flying zombies soar over it.
        return zombie.hasBehavior(ZombieBehaviorType.SWIM) || zombie.isFlying();
    }

    @Override
    public void onZombieEnter(ZombieInstance zombie, Cell cell, BehaviorContext context) {
        // Diving/surfacing is handled entirely by SwimBehavior.
        // No terrain-level action is needed here.
    }

    @Override
    public void onTick(Cell cell, Placeable model, BehaviorContext context, float deltaTime) {
        // Rising-tide plant destruction is driven by the wave system when it
        // shifts the water line, not by a per-tick terrain poll.
    }
}
