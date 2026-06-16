package model.game.map;

import model.item.placeable.Placeable;
import model.plant.instance.PlantInstance;
import model.zombie.definition.Zombie;
import model.enums.GroundType;
import model.enums.PlacableLayer;
import model.game.map.terrain.TerrainStrategy;
import model.plant.definition.Plant;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

import javax.sound.sampled.Line;
import java.util.*;

public class Cell {
    private int column;
    private int row;
    private GroundType groundType;
    private TerrainStrategy terrainStrategy;
    private Map<PlacableLayer, Placeable> placeables;
    private List<ZombieInstance> zombies; // TODO: matter of discussion imo
    private List<Projectile> projectiles;

    public Cell(int row, int column) {
        this.column = column;
        this.row = row;

        placeables = new LinkedHashMap<>();
        zombies = new LinkedList<>();
        projectiles = new LinkedList<>();
    }

    public boolean canPlant(Plant plant) {
        return placeables.isEmpty();
    }

    public boolean addPlaceable(Placeable placeable) {
        PlacableLayer layer = placeable.getLayer();
        if (placeables.containsKey(layer)) return false;

        placeables.put(layer, placeable);
        return true;
    }

    public void removePlaceable(Placeable placeable) {
        placeables.remove(placeable.getLayer());
    }

    public Placeable getPlaceable(PlacableLayer layer) {
        return placeables.get(layer);
    }

    public Plant getMainPlant() {
        return (Plant) placeables.get(PlacableLayer.MAIN);
    }

    // TODO: getGridItem

    public void addZombie(ZombieInstance zombie) {
        zombies.add(zombie);
    }

    public void removeZombie(ZombieInstance zombie) {
        zombies.remove(zombie);
    }

    public boolean isPassableForZombie(ZombieInstance zombie) {
        return placeables.get(PlacableLayer.MAIN) == null;
    }

    public void onZombieEnter(ZombieInstance zombie) {}

    public void addProjectile(Projectile projectile) {
        projectiles.add(projectile);
    }

    public void removeProjectile(Projectile projectile) {
        projectiles.remove(projectile);
    }
}
