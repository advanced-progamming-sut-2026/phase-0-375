package model.game.map.terrain;

import model.item.placeable.Placeable;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;
import model.game.map.Cell;
import model.plant.definition.Plant;

/**
 * Strategy that encapsulates the gameplay rules of a single ground type.
 */
public interface TerrainStrategy {
    boolean canPlant(Plant plant, Cell cell);

    boolean isPassable(ZombieInstance zombie, Cell cell);

    void onZombieEnter(ZombieInstance zombie, Cell cell, BehaviorContext context);

    void onTick(Cell cell, Placeable model, BehaviorContext context, float deltaTime);
}