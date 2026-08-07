package model.zombie.behavior.zombotany;

import model.enums.ZombieBehaviorType;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;

/**
 * Zombotany Squash zombie
 */
public class ZombotanySquashBehavior extends ZombotanyAbilityBehavior {
    private boolean squashUsed;

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead() || squashUsed) {
            return;
        }
        PlantInstance target = zombie.getEatingTarget();
        if (target == null || target.getCurrentHP() <= 0) {
            return;
        }
        squashUsed = true;
        context.destroyPlant(target);
        selfDestruct(zombie, context);
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.ZOMBOTANY_SQUASH;
    }

    // --- Getters ---

    public boolean isSquashUsed() {
        return squashUsed;
    }
}
