package model.game.map.terrain;

import model.enums.SlideDirection;
import model.item.placeable.Placeable;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;
import model.game.map.Cell;
import model.plant.definition.Plant;

/**
 * Terrain strategy for the Frostbite Caves slide tile.
 */
public class SlideTerrainStrategy implements TerrainStrategy {

    /** Which way this tile shunts zombies. */
    private final SlideDirection slideDirection;

    public SlideTerrainStrategy(SlideDirection slideDirection) {
        if (slideDirection == null) {
            throw new IllegalArgumentException("slideDirection must not be null");
        }
        this.slideDirection = slideDirection;
    }

    @Override
    public boolean canPlant(Plant plant, Cell cell) {
        // Slide ground never accepts a plant.
        return false;
    }

    @Override
    public boolean isPassable(ZombieInstance zombie, Cell cell) {
        // Zombies enter the tile freely; the slide effect triggers on entry.
        return true;
    }

    @Override
    public void onZombieEnter(ZombieInstance zombie, Cell cell, BehaviorContext context) {
        if (zombie == null || cell == null || context == null) {
            return;
        }
        // Flying zombies (e.g. Dodo Rider) soar over slides untouched.
        if (zombie.isFlying()) {
            return;
        }
        int targetRow = targetRow(cell, context);
        if (targetRow < 0 || targetRow >= context.getRowCount()) {
            // Off the board: leave the zombie where it is.
            return;
        }
        // Delegate the actual relocation so cell lists & continuous position
        // stay consistent with the rest of the movement code.
        context.moveZombieToLane(zombie, targetRow);
    }

    @Override
    public void onTick(Cell cell, Placeable model, BehaviorContext context, float deltaTime) {
        // The slide is an instantaneous on-entry effect, not a per-tick one.
    }

    /** @return the direction this tile shunts zombies. */
    public SlideDirection getSlideDirection() {
        return slideDirection;
    }

    /**
     * @param cell the slide tile's cell.
     * @param context the game context.
     * @return the row index the zombie should be moved to.
     */
    private int targetRow(Cell cell, BehaviorContext context) {
        int row = cell.getRow();
        return slideDirection == SlideDirection.UP ? row - 1 : row + 1;
    }
}