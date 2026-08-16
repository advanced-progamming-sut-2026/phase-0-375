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
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Strategy for the {@link PlantCategory#LOBBER} family.
 */
public class LobberAbility implements PlantAbility {

    private static final float LOB_VELOCITY = 2.2f;
    private static final Random RNG = new Random();

    /** Base chance (0..1) that Kernel-pult throws a butter instead of a kernel. */
    private static final float BASE_BUTTER_CHANCE = 0.25f;

    /** Butter. */
    private boolean butter = false;

    @Override
    public PlantCategory getCategory() { return PlantCategory.LOBBER; }

    @Override
    public PlantAction beginAction(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return null;
        Plant def = plant.getDefinition();
        if (def == null) return null;

        // Arma-mint: trigger plant-food on every LOBBER plant.
        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.LOBBER);
            return null;
        }

        if (def.getAbilityType() != PlantAbilityType.SHOOT_PROJECTILE) return null;
        if (!context.hasZombieInLane(plant.getPosition().getY())) return null;

        execute(plant, context);
        return TimedPlantAction.attackHold(plant, context);
    }

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

        butter = false;
        int shots = (int) def.getAbilityValue();
        if (shots <= 0) shots = 1;

        Projectile.Element element = inferElement(def, plant);
        float splashRadius = inferSplashRadius(def, plant);
        int damage = inferDamage(def, plant);

        int row = plant.getPosition().getY();
        FloatPoint origin = new FloatPoint(plant.getPosition().getX() + 0.5f, row);
        ZombieInstance target = nearestZombieAhead(plant, context, +1);

        for (int i = 0; i < shots; i++) {
            Splash splash = new Splash(
                    damage,
                    new FloatPoint(origin.getX(), origin.getY()),
                    row,
                    LOB_VELOCITY,
                    (butter) ? Projectile.Element.BUTTER : element,
                    +1,
                    splashRadius
            );
            aimLob(splash, origin, target, context.getColumnCount());
            context.spawnProjectile(splash, splash.getX(), splash.getY());
        }
    }

    /** True after the last lob was butter. */
    public boolean isButterShot() {
        return butter;
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;
        butter = false;
        int volley = (int) def.getPlantFoodValue();
        if (volley <= 0) return;

        if (def.getPlantFoodType() == PlantFoodType.PROJECTILE_BURST) {
            int row = plant.getPosition().getY();
            FloatPoint origin = new FloatPoint(plant.getPosition().getX() + 0.5f, row);
            Projectile.Element element = inferElement(def, plant);
            float splashRadius = inferSplashRadius(def, plant);
            int damage = inferDamage(def, plant);

            List<ZombieInstance> allZombies = new ArrayList<>();
            for (int i = 0; i < context.getRowCount(); i++) {
                allZombies.addAll(context.getZombiesInLane(i));
            }

            List<Integer> targetZombiesIndex = new ArrayList<>();

            for (int i = 0; i < 3 && i < allZombies.size(); i++) {
                int randomZombieIndex;
                while (targetZombiesIndex.contains(randomZombieIndex = RNG.nextInt(allZombies.size())));
                targetZombiesIndex.add(randomZombieIndex);
                ZombieInstance target = allZombies.get(randomZombieIndex);

                FloatPoint shotOrigin = new FloatPoint(origin.getX() + i * 0.2f, origin.getY());
                Splash splash = new Splash(
                        damage * 2,
                        shotOrigin,
                        row,
                        LOB_VELOCITY * 1.2f,
                        element,
                        +1,
                        Math.max(1.0f, splashRadius)
                );
                aimLob(splash, shotOrigin, target, context.getColumnCount());
                context.spawnProjectile(splash, splash.getX(), splash.getY());
            }
        }

        else if (def.getPlantFoodType() == PlantFoodType.MAP_WIDE_FREEZE) {
            int row = plant.getPosition().getY();
            FloatPoint origin = new FloatPoint(plant.getPosition().getX() + 0.5f, row);
            float splashRadius = inferSplashRadius(def, plant);
            for (int i = 0; i < context.getRowCount(); i++) {
                for (ZombieInstance zombie : context.getZombiesInLane(i)) {
                    int direction = (zombie.getGridX() < plant.getPosition().getX()) ? -1 : +1;
                    Splash splash = new Splash(
                            inferDamage(def, plant),
                            new FloatPoint(origin.getX(), origin.getY()),
                            row,
                            LOB_VELOCITY,
                            Projectile.Element.BUTTER,
                            direction,
                            Math.max(1.0f, splashRadius)
                    );
                    aimLob(splash, origin, zombie, context.getColumnCount());
                    context.spawnProjectile(splash, splash.getX(), splash.getY());
                }
            }
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
                butter = true;
                return baseDamage * 2;
            }
        }
        return baseDamage;
    }

    private static void aimLob(Splash splash, FloatPoint origin, ZombieInstance target, int columnCount) {
        splash.setHomingTarget(target);
        float landingX;
        float landingY = origin.getY();
        if (target != null && target.getContinuousPosition() != null) {
            landingX = target.getContinuousX();
            landingY = target.getContinuousY();
        } else {
            landingX = splash.getDirection() >= 0 ? columnCount - 0.5f : -0.5f;
        }
        splash.beginLob(origin.getX(), origin.getY(), landingX, landingY);
    }

    /**
     * Closest living zombie in the plant's lane that sits in {@code direction}
     * of the plant (the zombie this lob is attacking).
     */
    private static ZombieInstance nearestZombieAhead(PlantInstance plant, PlantAbilityContext context,
                                                    int direction) {
        if (plant.getPosition() == null || context == null) {
            return null;
        }
        int row = plant.getPosition().getY();
        float originX = plant.getPosition().getX() + 0.5f;
        ZombieInstance best = null;
        float bestDist = Float.MAX_VALUE;
        List<ZombieInstance> lane = context.getZombiesInLane(row);
        if (lane == null) {
            return null;
        }
        for (ZombieInstance zombie : lane) {
            if (zombie == null || zombie.isDead() || zombie.getContinuousPosition() == null) {
                continue;
            }
            if (zombie.isHypnotized()) {
                continue;
            }
            float dx = zombie.getContinuousX() - originX;
            if (direction > 0 && dx < 0f) {
                continue;
            }
            if (direction < 0 && dx > 0f) {
                continue;
            }
            float dist = Math.abs(dx);
            if (dist < bestDist) {
                bestDist = dist;
                best = zombie;
            }
        }
        return best;
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
