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
        return placeables.get(PlacableLayer.MAIN) == null;
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

    public PlantInstance getTopmostPlant() {
        Placeable overlay = placeables.get(PlacableLayer.OVERLAY);
        if (overlay instanceof PlantInstance) {
            return (PlantInstance) overlay;
        }
        Placeable main = placeables.get(PlacableLayer.MAIN);
        if (main instanceof PlantInstance) {
            return (PlantInstance) main;
        }
        return null;
    }

    public List<PlantInstance> getAllPlants() {
        List<PlantInstance> plants = new ArrayList<>(3);
        for (Placeable placeable : placeables.values()) {
            if (placeable instanceof PlantInstance) {
                plants.add((PlantInstance) placeable);
            }
        }
        return plants;
    }

    // TODO: getGridItem

    public void addZombie(ZombieInstance zombie) {
        zombies.add(zombie);
    }

    public void removeZombie(ZombieInstance zombie) {
        zombies.remove(zombie);
    }

    public List<ZombieInstance> getZombies() {
        return Collections.unmodifiableList(zombies);
    }

    public boolean isPassableForZombie(ZombieInstance zombie) {
        return placeables.get(PlacableLayer.MAIN) == null
                && placeables.get(PlacableLayer.OVERLAY) == null;
    }

    public void onZombieEnter(ZombieInstance zombie) {}

    public void addProjectile(Projectile projectile) {
        projectiles.add(projectile);
    }

    public void removeProjectile(Projectile projectile) {
        projectiles.remove(projectile);
    }

    public void rekeyPlaceable(Placeable placeable, PlacableLayer oldLayer) {
        if (placeable == null) return;
        if (placeables.get(oldLayer) == placeable) {
            placeables.remove(oldLayer);
            placeables.put(placeable.getLayer(), placeable);
        }
    }

    // --- Ground / terrain access ---

    /**
     * @return the {@link GroundType} of this cell.
     */
    public GroundType getGroundType() {
        return groundType;
    }

    /** Sets the ground type for this cell. */
    public void setGroundType(GroundType groundType) {
        this.groundType = groundType;
    }

    /**
     * @return the {@link TerrainStrategy} attached to this cell, or
     *         {@code null} if none has been configured.
     */
    public TerrainStrategy getTerrainStrategy() {
        return terrainStrategy;
    }

    /** Attaches a terrain strategy to this cell. */
    public void setTerrainStrategy(TerrainStrategy terrainStrategy) {
        this.terrainStrategy = terrainStrategy;
    }

    /** @return the column index of this cell on the map. */
    public int getColumn() {
        return column;
    }

    /** @return the row index of this cell on the map. */
    public int getRow() {
        return row;
    }
}