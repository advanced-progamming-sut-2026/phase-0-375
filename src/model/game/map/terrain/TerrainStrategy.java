package model.game.map.terrain;

import model.Placeable;
import model.Zombie;
import model.game.map.Cell;
import model.plant.definition.Plant;

public interface TerrainStrategy {
    boolean canPlant(Plant plant, Cell cell);
    boolean isPassable(Zombie zombie, Cell cell);
    void onZombieEnter(Zombie zombie, Cell cell);
    void onTick(Cell cell, Placeable model);
}

