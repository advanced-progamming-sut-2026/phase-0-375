package model.game.map;

import model.item.placeable.Placeable;
import model.Zombie;
import model.enums.GroundType;
import model.enums.PlacableLayer;
import model.game.map.terrain.TerrainStrategy;
import model.plant.definition.Plant;
import model.projectile.Projectile;

import java.util.List;
import java.util.Map;

public class Cell {
    private int column;
    private int row;
    private GroundType groundType;
    private TerrainStrategy terrainStrategy;
    private Map<PlacableLayer, Placeable> placeables;
    private List<Zombie> zombies; // TODO: matter of discussion imo
    private List<Projectile> projectiles;

    public Cell(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public boolean canPlant(Plant plant) { return false; }

    public boolean addPlaceable(Placeable placeable) { return false; }

    public void removePlaceable(Placeable placeable) { }

    public Placeable getPlaceable(PlacableLayer layer) { return placeables.get(layer); }

    public Plant getMainPlant() {
        return (Plant) placeables.get(PlacableLayer.MAIN);
    }

    // TODO: getGridItem

    public void addZombie(Zombie zombie) {}

    public void removeZombie(Zombie zombie) {}

    public boolean isPassableForZombie(Zombie zombie) { return false; }

    public void onZombieEnter(Zombie zombie) {}
}
