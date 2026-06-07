package model.game.map.terrain;

import model.Placeable;
import model.Zombie;
import model.game.map.Cell;
import model.plant.definition.Plant;

public class WaterTerrainStrategy implements TerrainStrategy {
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
