package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.PlantTags;
import model.game.map.FloatPoint;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.projectile.Splash;

/**
 * Strategy for the {@link PlantCategory#LOBBER} family.
 */
public class LobberAbility implements PlantAbility {

    private static final float LOB_VELOCITY = 3.5f;

    @Override
    public PlantCategory getCategory() { return PlantCategory.LOBBER; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        if (!context.hasZombieInLane(plant.getPosition().getY())) return;

        Plant def = plant.getDefinition();
        int shots = (int) def.getAbilityValue();
        if (shots <= 0) shots = 1;

        Projectile.Element element = inferElement(def);
        float splashRadius = inferSplashRadius(def);

        int row = plant.getPosition().getY();
        FloatPoint origin = new FloatPoint(plant.getPosition().getX() + 0.5f, row);

        for (int i = 0; i < shots; i++) {
            Splash splash = new Splash(
                    def.getDamage(),
                    new FloatPoint(origin.getX(), origin.getY()),
                    row,
                    LOB_VELOCITY,
                    element,
                    +1,
                    splashRadius
            );
            context.spawnProjectile(splash, splash.getX(), splash.getY());
        }
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def.getPlantFoodType() != PlantFoodType.PROJECTILE_BURST) return;
        int volley = (int) def.getPlantFoodValue();
        if (volley <= 0) return;

        int row = plant.getPosition().getY();
        FloatPoint origin = new FloatPoint(plant.getPosition().getX() + 0.5f, row);
        Projectile.Element element = inferElement(def);
        float splashRadius = inferSplashRadius(def);

        for (int i = 0; i < volley; i++) {
            Splash splash = new Splash(
                    def.getDamage() * 2,
                    new FloatPoint(origin.getX() + i * 0.2f, origin.getY()),
                    row,
                    LOB_VELOCITY * 1.2f,
                    element,
                    +1,
                    Math.max(1.0f, splashRadius)
            );
            context.spawnProjectile(splash, splash.getX(), splash.getY());
        }
    }

    private Projectile.Element inferElement(Plant def) {
        if (def.hasTag(PlantTags.ICE)) return Projectile.Element.ICE;
        if (def.hasTag(PlantTags.FIRE)) return Projectile.Element.FIRE;
        return Projectile.Element.NONE;
    }

    /** Lobbers default to a small splash; tag-based modifiers can extend this. */
    private float inferSplashRadius(Plant def) {
        if (def.hasTag(PlantTags.AOE)) return 1.0f;
        return 0.0f;
    }
}