package model.zombie.behavior.zombotany;

import model.enums.ZombieBehaviorType;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;

/**
 * Zombotany Squash zombie: leaps onto the first plant it reaches and
 * destroys both itself and that plant.
 */
public class ZombotanySquashBehavior extends ZombotanyAbilityBehavior {
    /** Jump-up + jump-down fallback when PAM durations are unknown. */
    public static final float ATTACK_DURATION = 1.0f;
    public static final float IMPACT_FRACTION = 0.55f;

    private boolean squashUsed;
    private boolean squashing;
    private boolean impacted;
    private float attackElapsed;
    private PlantInstance smashTarget;
    private int smashGridX = -1;
    private int smashGridY = -1;

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead() || squashUsed) {
            return;
        }
        if (squashing) {
            tickSquash(zombie, context, deltaTime);
            return;
        }
        PlantInstance target = zombie.getEatingTarget();
        if (target == null || target.getCurrentHP() <= 0) {
            return;
        }
        smashTarget = target;
        if (target.getPosition() != null) {
            smashGridX = target.getPosition().getX();
            smashGridY = target.getPosition().getY();
        } else {
            smashGridX = zombie.getGridX();
            smashGridY = zombie.getGridY();
        }
        squashing = true;
        attackElapsed = 0f;
        beginSpecialAction(zombie);
    }

    private void tickSquash(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        attackElapsed += deltaTime;
        if (!impacted && attackElapsed >= ATTACK_DURATION * IMPACT_FRACTION) {
            impacted = true;
            if (smashTarget != null && smashTarget.getCurrentHP() > 0) {
                context.destroyPlant(smashTarget);
            }
            squashUsed = true;
            selfDestruct(zombie, context);
            return;
        }
        if (attackElapsed >= ATTACK_DURATION) {
            squashing = false;
            clearSpecialAction(zombie);
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.ZOMBOTANY_SQUASH;
    }

    // --- Getters ---

    public boolean isSquashUsed() {
        return squashUsed;
    }

    public boolean isSquashing() {
        return squashing;
    }

    public float getAttackElapsed() {
        return attackElapsed;
    }

    public int getSmashTargetGridX() {
        return smashGridX;
    }

    public int getSmashTargetGridY() {
        return smashGridY;
    }
}
