package model.game.map.terrain;

import model.enums.SlideDirection;
import model.item.placeable.Placeable;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;
import model.game.map.Cell;
import model.plant.definition.Plant;

/**
 * Terrain strategy for the Frostbite Caves slide tile.
 *
 * <p>Entering zombies do not slide immediately; the slide is armed here and
 * fires once the zombie reaches the middle of the tile (see
 * {@code GameModel#tickArmedSlide}), so it visibly steps onto the slider
 * first.
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
        // Zombies enter the tile freely; the slide effect triggers at midpoint.
        return true;
    }

    @Override
    public void onZombieEnter(ZombieInstance zombie, Cell cell, BehaviorContext context) {
        if (zombie == null || cell == null || context == null) {
            return;
        }
        int targetRow = targetRowFor(zombie, cell, context.getRowCount());
        if (targetRow < 0) {
            return;
        }
        context.armLaneSlide(zombie, cell, targetRow);
    }

    @Override
    public void onTick(Cell cell, Placeable model, BehaviorContext context, float deltaTime) {
        // The slide is an armed on-entry effect, not a per-tick one.
    }

    /** @return the direction this tile shunts zombies. */
    public SlideDirection getSlideDirection() {
        return slideDirection;
    }

    /**
     * @param zombie the zombie stepping onto this tile.
     * @param cell the slide tile's cell.
     * @param rowCount total lane count for bounds checking.
     * @return the row this zombie will be shunted to, or {@code -1} when it
     *         flies over the slide or the target row would leave the board.
     */
    public int targetRowFor(ZombieInstance zombie, Cell cell, int rowCount) {
        // Flying zombies (e.g. Dodo Rider) soar over slides untouched.
        if (zombie == null || cell == null || zombie.isFlying()) {
            return -1;
        }
        int row = targetRow(cell);
        // Off the board: no slide.
        return row >= 0 && row < rowCount ? row : -1;
    }

    /**
     * @param cell the slide tile's cell.
     * @return the row index the zombie should be moved to.
     */
    private int targetRow(Cell cell) {
        int row = cell.getRow();
        return slideDirection == SlideDirection.UP ? row - 1 : row + 1;
    }
}
