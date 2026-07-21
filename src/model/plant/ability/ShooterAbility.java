package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.PlantTags;
import model.game.map.FloatPoint;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

import java.util.List;
import java.util.Random;

/**
 * Strategy for the {@link PlantCategory#SHOOTER} family.
 */
public class ShooterAbility implements PlantAbility {

    private static final Random RNG = new Random();

    private static final float PELLET_VELOCITY = 1f;
    private static final float PELLET_SPAWN_OFFSET = 0.5f;

    @Override
    public PlantCategory getCategory() { return PlantCategory.SHOOTER; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;

        // Appease-mint: trigger plant-food on every SHOOTER plant.
        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.SHOOTER);
            return;
        }

        if (def.getAbilityType() != PlantAbilityType.SHOOT_PROJECTILE) return;
        if (!shouldFire(plant, context)) return;

        int pelletCount = (int) def.getAbilityValue();
        if (pelletCount <= 0) pelletCount = 1;

        Projectile.Element element = inferElement(def);
        FloatPoint origin = pelletOrigin(plant);

        for (int i = 0; i < pelletCount; i++) {
            Pellet pellet = new Pellet(
                    def.getDamage(),
                    new FloatPoint(origin.getX() + i * 0.05f, origin.getY()),
                    plant.getPosition().getY(),
                    PELLET_VELOCITY,
                    element,
                    +1
            );
            context.spawnProjectile(pellet, pellet.getX(), pellet.getY());
        }
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def.getPlantFoodType() != PlantFoodType.PROJECTILE_BURST) {
            return;
        }
        int volley = (int) def.getPlantFoodValue();
        if (volley <= 0) return;
        Projectile.Element element = inferElement(def);
        int lane = plant.getPosition().getY();
        FloatPoint origin = pelletOrigin(plant);
        for (int i = 0; i < volley; i++) {
            if (i % 5 == 0) {
                origin.setX(origin.getX() + i * 0.08f);
            }

            float dx = (RNG.nextFloat() - 0.5f) * 0.4f;
            float dy = (RNG.nextFloat() - 0.5f) * 0.4f;

            Pellet pellet = new Pellet(
                    def.getDamage(),
                    new FloatPoint(origin.getX() + dx, origin.getY() + dy),
                    lane,
                    PELLET_VELOCITY * 1.25f,
                    element,
                    +1
            );
            context.spawnProjectile(pellet, pellet.getX(), pellet.getY());
        }
    }

    // --- Helpers ---

    private boolean shouldFire(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return false;
        return context.hasZombieInLane(plant.getPosition().getY());
    }

    private FloatPoint pelletOrigin(PlantInstance plant) {
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        return new FloatPoint(col + PELLET_SPAWN_OFFSET, row);
    }

    private Projectile.Element inferElement(Plant def) {
        if (def.hasTag(PlantTags.ICE)) return Projectile.Element.ICE;
        if (def.hasTag(PlantTags.FIRE)) return Projectile.Element.FIRE;
        if (def.hasTag(PlantTags.POISON)) return Projectile.Element.POISON;
        return Projectile.Element.NONE;
    }
}