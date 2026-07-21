package model.zombie.behavior.zombotany;

import model.enums.ZombieBehaviorType;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;

/**
 * Zombotany Jalapeno zombie: after staying on the lawn for its fuse time it
 * ignites, burning every plant in its lane and destroying itself.
 */
public class ZombotanyJalapenoBehavior extends ZombotanyAbilityBehavior {

    public static final float DEFAULT_FUSE_SECONDS = 10f;
    public static final int DEFAULT_BURN_DAMAGE = 1800;

    private float fuseTimer;

    private boolean ignited;

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead() || ignited) {
            return;
        }

        float fuse = zombie.getDefinition().getBehaviorPropFloat(
                "FuseSeconds", DEFAULT_FUSE_SECONDS);
        if (fuse <= 0f) {
            fuse = DEFAULT_FUSE_SECONDS;
        }

        fuseTimer += deltaTime;
        if (fuseTimer < fuse) {
            return;
        }
        ignited = true;

        int burn = definitionDamage("Jalapeno", DEFAULT_BURN_DAMAGE);
        for (PlantInstance plant : new ArrayList<>(context.getPlantsInLane(zombie.getGridY()))) {
            if (plant != null && plant.getCurrentHP() > 0) {
                context.damagePlant(plant, burn);
            }
        }
        selfDestruct(zombie, context);
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.ZOMBOTANY_JALAPENO;
    }

    // --- Getters ---

    public float getFuseTimer() {
        return fuseTimer;
    }

    public boolean isIgnited() {
        return ignited;
    }
}
