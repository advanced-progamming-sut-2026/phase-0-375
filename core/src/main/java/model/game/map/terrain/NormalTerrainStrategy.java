package model.game.map.terrain;

import model.item.placeable.Placeable;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;
import model.game.map.Cell;
import model.plant.definition.Plant;

/**
 * Terrain strategy for ordinary, plantable ground.
 */
public class NormalTerrainStrategy implements TerrainStrategy {

    public NormalTerrainStrategy() {

    }

    @Override
    public boolean canPlant(Plant plant, Cell cell) {
        return true;
    }

    @Override
    public boolean isPassable(ZombieInstance zombie, Cell cell) {
        // Every zombie can walk on ordinary ground.
        return true;
    }

    @Override
    public void onZombieEnter(ZombieInstance zombie, Cell cell, BehaviorContext context) {
        // No terrain effect on a normal tile.
    }

    @Override
    public void onTick(Cell cell, Placeable model, BehaviorContext context, float deltaTime) {
        // Normal ground has no per-tick effect.
    }
}
