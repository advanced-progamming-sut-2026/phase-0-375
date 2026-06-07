package model.game.map.terrain;

import model.Placeable;
import model.Zombie;
import model.enums.SlideDirection;
import model.game.map.Cell;
import model.plant.definition.Plant;

public class SlideTerrainStrategy implements TerrainStrategy {
    SlideDirection slideDirection;

    public SlideTerrainStrategy(SlideDirection slideDirection) {
        this.slideDirection = slideDirection;
    }

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
