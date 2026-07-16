package model.game.map.terrain;

import model.enums.PlacableLayer;
import model.enums.PlantTags;
import model.item.placeable.Placeable;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;
import model.game.map.Cell;

/**
 * Terrain strategy for the Frostbite Caves "ice block" tile.
 */
public class IceTerrainStrategy implements TerrainStrategy {

    /** Starting (and maximum) HP of a fresh ice block. */
    public static final int MAX_HP = 600;

    /** HP lost per second while at least one fiery plant neighbors the tile. */
    public static final int MELT_HP_PER_SECOND = 60;

    /** Current ice HP; drains toward zero and then stays there. */
    private int hp;

    /**
     * The plant or zombie frozen inside this tile, or null if the
     * tile is empty.
     */
    private Placeable containedEntity;

    /** true once the ice has shattered; further damage / melt is a no-op. */
    private boolean melted;

    /** Creates an empty ice tile at full HP. */
    public IceTerrainStrategy() {
        this.hp = MAX_HP;
        this.containedEntity = null;
        this.melted = false;
    }

    public IceTerrainStrategy(Placeable containedEntity) {
        this();
        this.containedEntity = containedEntity;
    }

    @Override
    public boolean canPlant(Plant plant, Cell cell) {
        // Frozen ground never accepts a new plant.
        return false;
    }

    @Override
    public boolean isPassable(ZombieInstance zombie, Cell cell) {
        // The ice tile itself is walkable. the frozen entity inside is a
        // separate unit and does not block movement.
        return true;
    }

    @Override
    public void onZombieEnter(ZombieInstance zombie, Cell cell, BehaviorContext context) {
        // The zombie walking over the tile is not the frozen entity, so the
        // terrain does nothing on entry.
    }

    @Override
    public void onTick(Cell cell, Placeable model, BehaviorContext context, float deltaTime) {
        if (melted || cell == null || context == null || deltaTime <= 0f) {
            return;
        }
        if (hasFieryNeighbour(cell, context)) {
            melt(MELT_HP_PER_SECOND, deltaTime);
        }
    }

    /** Applies direct damage to the ice */
    public void takeDamage(int damage) {
        if (melted || damage <= 0) {
            return;
        }
        hp -= damage;
        if (hp <= 0) {
            hp = 0;
            melted = true;
        }
    }

    /** Melts the ice at the given rate for the given duration. */
    public void melt(int hpPerSecond, float deltaTime) {
        if (melted || hpPerSecond <= 0 || deltaTime <= 0f) {
            return;
        }
        int loss = (int) (hpPerSecond * deltaTime);
        if (loss <= 0) {
            // Sub-tick accumulation: round up so very small deltas still melt.
            loss = 1;
        }
        hp -= loss;
        if (hp <= 0) {
            hp = 0;
            melted = true;
        }
    }

    // --- Queries ---

    /** @return remaining HP of the ice block (0 once shattered). */
    public int getHp() {
        return hp;
    }

    /** @return {@code true} once the ice has shattered. */
    public boolean isMelted() {
        return melted;
    }

    /**
     * @return the plant or zombie frozen inside, or {@code null} if the tile
     *         is empty.
     */
    public Placeable getContainedEntity() {
        return containedEntity;
    }

    /**
     * Sets or clears the contained entity. Pass {@code null} after the
     * system has freed the previously-trapped unit.
     */
    public void setContainedEntity(Placeable containedEntity) {
        this.containedEntity = containedEntity;
    }

    // --- Internal helpers ---

    /**
     * Scans the eight cells surrounding the given cell and returns
     * true if at least one of them carries a fiery plant on its MAIN
     * layer.
     */
    private boolean hasFieryNeighbour(Cell cell, BehaviorContext context) {
        int row = cell.getRow();
        int col = cell.getColumn();
        for (int rowDist = -1; rowDist <= 1; rowDist++) {
            for (int colDist = -1; colDist <= 1; colDist++) {
                if (rowDist == 0 && colDist == 0) {
                    continue;
                }
                Cell neighbor = context.getCellAt(row + rowDist, col + colDist);
                if (neighbor == null) {
                    continue;
                }
                Placeable occupant = neighbor.getPlaceable(PlacableLayer.MAIN);
                if (isFieryPlant(occupant)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param placeable a placeable (possibly {@code null}).
     * @return true if {@code placeable} is a plant instance whose definition
     *         carries the {@link PlantTags#FIRE} tag.
     */
    private boolean isFieryPlant(Placeable placeable) {
        if (!(placeable instanceof PlantInstance)) {
            return false;
        }
        PlantInstance plant = (PlantInstance) placeable;
        Plant def = plant.getDefinition();
        return def != null && def.hasTag(PlantTags.FIRE);
    }
}