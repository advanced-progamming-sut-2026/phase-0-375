package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Strategy for the {@link PlantCategory#HOMING} family.
 */
public class HomingAbility implements PlantAbility {

    private static final float PELLET_VELOCITY = 6.0f;

    @Override
    public PlantCategory getCategory() { return PlantCategory.HOMING; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def.getAbilityType() != PlantAbilityType.SHOOT_PROJECTILE) return;
        if (plant.getPosition() == null) return;

        ZombieInstance target = pickTarget(plant, context);
        if (target == null) return;

        FloatPoint origin = new FloatPoint(
                plant.getPosition().getX() + 0.5f,
                plant.getPosition().getY()
        );
        Pellet pellet = new Pellet(
                def.getDamage(),
                origin,
                plant.getPosition().getY(),
                PELLET_VELOCITY,
                Projectile.Element.NONE,
                +1
        );
        // Tag the pellet as homing so the projectile system steers it
        // toward the target each tick. (Implementation of steering is
        // in ProjectileSystem; here we only spawn.)
        context.spawnProjectile(pellet, pellet.getX(), pellet.getY());
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def.getPlantFoodType() == PlantFoodType.RANDOM_HYPNOTIZE) {
            int count = (int) def.getPlantFoodValue();
            hypnotiseRandomZombies(context, count);
        }
    }

    // --- Helpers ---

    private ZombieInstance pickTarget(PlantInstance plant, PlantAbilityContext context) {
        // Default policy: prefer the highest-HP zombie on the field
        ZombieInstance best = null;
        for (int lane = 0; lane < context.getRowCount(); lane++) {
            for (ZombieInstance z : context.getZombiesInLane(lane)) {
                if (best == null || z.getCurrentHP() > best.getCurrentHP()) {
                    best = z;
                }
            }
        }
        return best;
    }

    private void hypnotiseRandomZombies(PlantAbilityContext context, int count) {
        for (int lane = 0; lane < context.getRowCount() && count > 0; lane++) {
            List<ZombieInstance> zombiesInLine = context.getZombiesInLane(lane);
            for (ZombieInstance zombie : zombiesInLine) {
                if (count <= 0) break;
                zombie.setState(ZombieState.HYPNOTIZED);
                count--;
            }
        }
    }
}