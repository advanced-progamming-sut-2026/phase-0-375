package model.game.map.terrain;

import model.item.placeable.Placeable;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;
import model.game.map.Cell;
import model.plant.definition.Plant;

/**
 * Unplantable crater left by Doom-shroom (and similar map-wide blasts).
 */
public class CraterTerrainStrategy implements TerrainStrategy {

    @Override
    public boolean canPlant(Plant plant, Cell cell) {
        return false;
    }

    @Override
    public boolean isPassable(ZombieInstance zombie, Cell cell) {
        return true;
    }

    @Override
    public void onZombieEnter(ZombieInstance zombie, Cell cell, BehaviorContext context) {
        // Craters only block planting.
    }

    @Override
    public void onTick(Cell cell, Placeable model, BehaviorContext context, float deltaTime) {
        // No per-tick effect.
    }
}
