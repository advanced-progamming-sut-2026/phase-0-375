package model.zombie.behavior.zombotany;

import model.enums.ZombieBehaviorType;
import model.game.map.FloatPoint;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Pellet;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;

/**
 * Zombotany Peashooter zombie: fires a leftward pea at plants in its lane.
 */
public class ZombotanyPeashooterBehavior extends ZombotanyAbilityBehavior {
    public static final float DEFAULT_SHOT_INTERVAL_SECONDS = 1.5f;
    public static final int DEFAULT_PEA_DAMAGE = 20;
    /** Matches {@code ShooterAbility} / {@code TimedPlantAction} presentation. */
    public static final float ATTACK_DURATION = 0.6f;
    public static final float FIRE_FRACTION = 0.4f;
    public static final float PELLET_VELOCITY = 1f;
    public static final float PELLET_X_OFFSET = -0.4f;
    public static final float PELLET_Y_OFFSET = -0.48f;

    private float shotTimer;
    private float attackElapsed;
    private boolean attacking;
    private boolean fired;

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        if (attacking) {
            tickAttack(zombie, context, deltaTime);
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
        attacking = true;
        fired = false;
        attackElapsed = 0f;
    }

    private void tickAttack(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        attackElapsed += deltaTime;
        if (!fired && attackElapsed >= ATTACK_DURATION * FIRE_FRACTION) {
            fired = true;
            spawnPea(zombie, context);
        }
        if (attackElapsed >= ATTACK_DURATION) {
            attacking = false;
            fired = false;
            attackElapsed = 0f;
        }
    }

    private void spawnPea(ZombieInstance zombie, BehaviorContext context) {
        Plant source = plantDefinition("Peashooter");
        int damage = source != null && source.getDamage() > 0
                ? source.getDamage()
                : definitionDamage("Peashooter", DEFAULT_PEA_DAMAGE);
        float x = zombie.getContinuousX() + PELLET_X_OFFSET;
        float y = zombie.getContinuousY() + PELLET_Y_OFFSET;
        int row = zombie.getGridY();
        Pellet pea = new Pellet(damage, new FloatPoint(x, y), row, PELLET_VELOCITY);
        pea.reflect();
        pea.setSourcePlant(source);
        context.spawnProjectile(pea);
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

    public boolean isAttacking() {
        return attacking;
    }

    public float getAttackElapsed() {
        return attackElapsed;
    }
}
