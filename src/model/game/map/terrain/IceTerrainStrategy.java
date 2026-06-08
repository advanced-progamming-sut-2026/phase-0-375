package model.game.map.terrain;

import model.item.placeable.Placeable;
import model.zombie.Zombie;
import model.game.map.Cell;
import model.plant.definition.Plant;

public class IceTerrainStrategy implements TerrainStrategy {
    @Override
    public boolean canPlant(Plant plant, Cell cell) {
        return false;
    }

    @Override
    public boolean isPassable(Zombie zombie, Cell cell) {
        return false;
    }

    @Override
    public void onZombieEnter(Zombie zombie, Cell cell) {

    }

    @Override
    public void onTick(Cell cell, Placeable model) {

    }
}
