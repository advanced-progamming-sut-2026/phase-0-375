package model.zombie.behavior.zombotany;

import model.enums.ZombieBehaviorType;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;

/**
 * Zombotany Peashooter zombie
 */
public class ZombotanyPeashooterBehavior extends ZombotanyAbilityBehavior {
    public static final float DEFAULT_SHOT_INTERVAL_SECONDS = 1.5f;
    public static final int DEFAULT_PEA_DAMAGE = 20;

    private float shotTimer;

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        float interval = zombie.getDefinition().getBehaviorPropFloat(
                "ShotIntervalSeconds", DEFAULT_SHOT_INTERVAL_SECONDS);
        if (interval <= 0f) {
            interval = DEFAULT_SHOT_INTERVAL_SECONDS;
        }

        shotTimer += deltaTime;
        if (shotTimer < interval) {
            return;
        }
        PlantInstance target = findNearestPlantAhead(zombie, context);
        if (target == null) {
            shotTimer = interval; // stay ready; fire as soon as a target appears
            return;
        }
        shotTimer -= interval;
        context.damagePlant(target, definitionDamage("Peashooter", DEFAULT_PEA_DAMAGE));
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.ZOMBOTANY_PEASHOOTER;
    }

    /**
     * Finds the plant closest to the zombie within its own lane, only
     * considering plants ahead of (to the left of) the zombie.
     */
    private PlantInstance findNearestPlantAhead(ZombieInstance zombie, BehaviorContext context) {
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
                continue;
            }
            if (col > nearestCol) {
                nearestCol = col;
                nearest = plant;
            }
        }
        return nearest;
    }

    // --- Getters ---

    public float getShotTimer() {
        return shotTimer;
    }
}
