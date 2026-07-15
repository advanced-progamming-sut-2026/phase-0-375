package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.PlantSpecialTag;
import model.enums.PlantTags;
import model.game.map.FloatPoint;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.definition.PlantLevels;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.projectile.Splash;

import java.util.Random;

/**
 * Strategy for the {@link PlantCategory#LOBBER} family.
 */
public class LobberAbility implements PlantAbility {

    private static final float LOB_VELOCITY = 0.8f;
    private static final Random RNG = new Random();

    /** Base chance (0..1) that Kernel-pult throws a butter instead of a kernel. */
    private static final float BASE_BUTTER_CHANCE = 0.25f;

    @Override
    public PlantCategory getCategory() { return PlantCategory.LOBBER; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        Plant def = plant.getDefinition();
        if (def == null) return;

        // Arma-mint: trigger plant-food on every LOBBER plant.
        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.LOBBER);
            return;
        }

        if (def.getAbilityType() != PlantAbilityType.SHOOT_PROJECTILE) return;
        if (!context.hasZombieInLane(plant.getPosition().getY())) return;

        int shots = (int) def.getAbilityValue();
        if (shots <= 0) shots = 1;

        Projectile.Element element = inferElement(def, plant);
        float splashRadius = inferSplashRadius(def, plant);
        int damage = inferDamage(def, plant);

        int row = plant.getPosition().getY();
        FloatPoint origin = new FloatPoint(plant.getPosition().getX() + 0.5f, row);

        for (int i = 0; i < shots; i++) {
            Splash splash = new Splash(
                    damage,
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
        Projectile.Element element = inferElement(def, plant);
        float splashRadius = inferSplashRadius(def, plant);
        int damage = inferDamage(def, plant);

        for (int i = 0; i < volley; i++) {
            Splash splash = new Splash(
                    damage * 2,
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

    private Projectile.Element inferElement(Plant def, PlantInstance plant) {
        if (def.hasTag(PlantTags.ICE)) return Projectile.Element.ICE;
        if (def.hasTag(PlantTags.FIRE)) return Projectile.Element.FIRE;
        if (def.hasTag(PlantTags.POISON)) return Projectile.Element.POISON;
        return Projectile.Element.NONE;
    }

    /**
     * Lobbers default to a small splash; tag-based modifiers and the
     * SPLASH_DAMAGE_BUFF upgrade can extend this.
     */
    private float inferSplashRadius(Plant def, PlantInstance plant) {
        float radius = def.hasTag(PlantTags.AOE) ? 1.0f : 0.0f;
        // SPLASH_DAMAGE_BUFF adds to the splash radius.
        radius += cumulativeSpecialValue(plant, PlantSpecialTag.SPLASH_DAMAGE_BUFF);
        return Math.max(0f, radius);
    }

    /**
     * Returns the per-shot damage, taking into account the
     * BUTTER_CHANCE_BUFF upgrade for Kernel-pult (chance to throw a
     * butter that deals extra damage and stuns).
     */
    private int inferDamage(Plant def, PlantInstance plant) {
        int baseDamage = def.getDamage();
        // Kernel-pult: chance to throw a butter for double damage.
        if (isKernelPult(def)) {
            float butterChance = BASE_BUTTER_CHANCE
                    + cumulativeSpecialValue(plant, PlantSpecialTag.BUTTER_CHANCE_BUFF);
            if (RNG.nextFloat() < butterChance) {
                return baseDamage * 2;
            }
        }
        return baseDamage;
    }

    /** @return true if this plant is a Kernel-pult. */
    private boolean isKernelPult(Plant def) {
        return def.getName() != null
                && def.getName().toLowerCase().contains("kernel");
    }

    /** Sums up every upgrade value with the given special tag. */
    private float cumulativeSpecialValue(PlantInstance plant, PlantSpecialTag tag) {
        Plant def = plant.getDefinition();
        if (def == null || def.getLevels() == null) return 0f;
        PlantLevels levels = def.getLevels();
        float total = 0f;
        for (int lvl = 2; lvl <= 4; lvl++) {
            if (lvl > plant.getLevel()) break;
            LevelUpgrade upgrade = levels.getUpgrade(lvl);
            if (upgrade == null) continue;
            if (upgrade.isSpecialMechanic() && upgrade.getSpecialTag() == tag) {
                total += upgrade.getValue();
            }
        }
        return total;
    }
}