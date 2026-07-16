package model.game.systems;

import model.enums.GroundType;
import model.enums.PlacableLayer;
import model.enums.PlantTags;
import model.enums.ZombieState;
import model.event.EventBus;
import model.event.GameEvent;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.game.map.Cell;
import model.game.map.terrain.IceTerrainStrategy;
import model.game.map.terrain.NormalTerrainStrategy;
import model.game.map.terrain.TerrainStrategy;
import model.item.placeable.Placeable;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

public class TerrainSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

    public TerrainSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {
        int rows = gameModel.getRowCount();
        int cols = gameModel.getColumnCount();

        boolean anyIceMelted = false;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Cell cell = gameModel.getCellAt(row, col);
                if (cell == null) {
                    continue;
                }

                TerrainStrategy strategy = cell.getTerrainStrategy();
                if (strategy == null) {
                    continue;
                }

                // Per-tick terrain effect (e.g. ice radiant melt).
                Placeable mainOccupant = cell.getPlaceable(PlacableLayer.MAIN);
                strategy.onTick(cell, mainOccupant, gameModel, deltaTime);

                // Ice-shatter: free the contained entity when the ice breaks.
                if (strategy instanceof IceTerrainStrategy) {
                    IceTerrainStrategy ice = (IceTerrainStrategy) strategy;
                    if (ice.isMelted() && ice.getContainedEntity() != null) {
                        releaseContainedEntity(cell, ice);
                        anyIceMelted = true;
                    }
                }

                // Rising-tide: destroy non-water plants stranded on water.
                checkRisingTide(cell);
            }
        }

        if (anyIceMelted && eventBus != null) {
            eventBus.dispatch(new GameEvent(GameEvent.Type.STATUS_EXPIRED));
        }
    }

    // --- Ice-shatter entity release ---

    /**
     * Frees the plant or zombie that was trapped inside the given ice
     * strategy, then converts the cell back to normal ground.
     */
    private void releaseContainedEntity(Cell cell, IceTerrainStrategy ice) {
        Placeable entity = ice.getContainedEntity();
        ice.setContainedEntity(null);

        if (entity instanceof PlantInstance) {
            releasePlant((PlantInstance) entity, cell);
        } else if (entity instanceof ZombieInstance) {
            releaseZombie((ZombieInstance) entity, cell);
        }

        // The ice is gone, the tile becomes ordinary ground.
        cell.setGroundType(GroundType.NORMAL);
        cell.setTerrainStrategy(new NormalTerrainStrategy());
    }

    /**
     * Unfreezes a plant released from ice and makes sure it occupies the
     * cell's MAIN layer.
     */
    private void releasePlant(PlantInstance plant, Cell cell) {
        if (plant.isFrozen()) {
            plant.unfreeze();
        }
        // If the plant wasn't already seated on this cell (e.g. it was
        // created inside the ice at level-load), place it now.
        if (cell.getPlaceable(PlacableLayer.MAIN) != plant) {
            cell.addPlaceable(plant);
        }
    }

    /**
     * De-chills a zombie released from ice, restores a walking state, and
     * re-registers it on the field if it wasn't already active.
     */
    private void releaseZombie(ZombieInstance zombie, Cell cell) {
        // Strip all chill stacks so the zombie is no longer frozen.
        while (zombie.getChillLevel() > 0) {
            zombie.removeChill();
        }
        // Restore a walking state unless the zombie is dead/dying.
        ZombieState state = zombie.getState();
        if (state != ZombieState.DYING && state != ZombieState.DEAD) {
            zombie.setState(ZombieState.WALKING);
        }
        // Re-register on the field if needed.
        gameModel.addExistingZombie(zombie, cell.getRow(), cell.getColumn());
    }

    // --- Rising-tide plant destruction ---

    /**
     * Checks whether the cell is a water tile carrying a non-water plant
     * without a platform, and if so destroys the plant.
     */
    private void checkRisingTide(Cell cell) {
        GroundType ground = cell.getGroundType();
        if (ground != GroundType.WATER && ground != GroundType.LOW_TIDE) {
            return;
        }

        Placeable main = cell.getPlaceable(PlacableLayer.MAIN);
        if (!(main instanceof PlantInstance)) {
            return;
        }

        PlantInstance plant = (PlantInstance) main;
        // Aquatic plants survive on water.
        if (plant.getDefinition() != null
                && plant.getDefinition().hasTag(PlantTags.WATER)) {
            return;
        }
        // Non-aquatic plants need a platform (e.g. Lily Pad) on the
        // GROUND layer to survive on water.
        if (cell.getPlaceable(PlacableLayer.GROUND) != null) {
            return;
        }

        // Stranded, destroy the plant.
        gameModel.destroyPlant(plant);
    }
}
