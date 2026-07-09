package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

/**
 * Fish behavior.
 */
public class FishBehavior implements ZombieBehavior {

    // --- Constants ---

    /** Seconds between two consecutive hook casts. */
    public static final float DELAY_BETWEEN_CASTING = 2.5f;

    // --- State ---

    /** Seconds elapsed since the last cast. */
    private float castTimer = 0f;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        castTimer += deltaTime;
        if (castTimer < DELAY_BETWEEN_CASTING) {
            return;
        }
        castTimer -= DELAY_BETWEEN_CASTING;

        castHook(zombie, context);
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.FISH;
    }

    // --- Core logic ---

    /**
     * Hooks the plant in the Fisherman's row, if any, and reels it in.
     */
    private void castHook(ZombieInstance zombie, BehaviorContext context) {
        PlantInstance hookedPlant = findNearestPlantInLane(zombie, context);
        if (hookedPlant == null || hookedPlant.getCurrentHP() <= 0) {
            return;
        }

        reelIn(hookedPlant, context);
    }

    /**
     * Pulls the hooked plant one tile forward. If that tile is
     * occupied,the plant is thrown away and destroyed instead.
     */
    private void reelIn(PlantInstance hookedPlant, BehaviorContext context) {
        int currentCol = hookedPlant.getPosition().getX();
        int targetCol = currentCol + 1;

        int currentRow = hookedPlant.getPosition().getY();

        if (targetCol >= context.getColumnCount() || context.getPlantAt(currentRow, targetCol) != null) {
            context.destroyPlant(hookedPlant);
            return;
        }

        context.movePlant(hookedPlant, currentRow, targetCol);
    }

    /**
     * Finds the plant closest to the zombie's current position within its own lane.
     */
    private PlantInstance findNearestPlantInLane(ZombieInstance zombie, BehaviorContext context) {
        int row = zombie.getGridY();
        int zombieCol = zombie.getGridX();

        PlantInstance nearest = null;
        int nearestCol = -1;
        for (PlantInstance plant : context.getPlantsInLane(row)) {
            if (plant == null || plant.getCurrentHP() <= 0) {
                continue;
            }
            int col = plant.getPosition() != null ? plant.getPosition().getX() : -1;
            if (col < 0 || col >= zombieCol) {
                continue; // only plants ahead of (to the left of) the zombie are valid targets
            }
            if (col > nearestCol) {
                nearestCol = col;
                nearest = plant;
            }
        }
        return nearest;
    }

    // --- Getters ---

    public float getCastTimer() {
        return castTimer;
    }

    // --- Setters ---

    public void setCastTimer(float castTimer) {
        this.castTimer = castTimer;
    }
}