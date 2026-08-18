package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantSpecialTag;
import model.enums.PlantTags;
import model.enums.SunType;
import model.item.Sun;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.definition.PlantLevels;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Strategy for the {@link PlantCategory#WALL_NUT} family.
 */
public class WallAbility implements PlantAbility {

    /** Default damage dealt by Explode-o-nut's death (and helmet-break) explosion. */
    private static final int DEFAULT_EXPLODE_O_NUT_DAMAGE = 1800;
    /** Default radius (in tiles) of Explode-o-nut's explosion. */
    private static final int DEFAULT_EXPLODE_O_NUT_RADIUS = 1;
    /** Default sun dropped per bite the zombie takes from a Sun Bean. */
    private static final int DEFAULT_SUN_BEAN_PER_BITE = 5;

    @Override
    public PlantCategory getCategory() { return PlantCategory.WALL_NUT; }

    @Override
    public PlantAction beginAction(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return null;

        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.WALL_NUT);
            return null;
        }

        if (isSweepPotato(def)) {
            attractZombiesTo(plant, context);
            return null;
        }

        if (isEndurian(def) && isBeingEaten(plant, context)) {
            return TimedPlantAction.attackAt(plant, context, this::redirectOrReflect);
        }

        return null;
    }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;

        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.WALL_NUT);
            return;
        }

        if (def.hasTag(PlantTags.SUN)) {
            return;
        }

        if (def.getDamage() > 0 || def.hasTag(PlantTags.MOVE_ZOMBIE)) {
            redirectOrReflect(plant, context);
        }
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;

        switch (def.getPlantFoodType()) {
            case GRANT_PERMANENT_ARMOR:
                grantMetalArmor(plant, def);
                break;
            case KNOCKBACK_BLAST:
                knockbackBlast(plant, context);
                break;
            case ATTRACT_AND_HEAL:
                plant.restoreFullHP();
                attractAllAdjacentLanes(plant, context);
                break;
            default:
                if (isSweepPotato(def)) {
                    plant.restoreFullHP();
                    attractAllAdjacentLanes(plant, context);
                }
                break;
        }
    }

    /**
     * Bite hook used by the zombie system. Sun Bean drops sun; Garlic
     * shoves the chewing zombie into an adjacent lane.
     */
    public static void onBitten(PlantInstance plant, ZombieInstance zombie, PlantAbilityContext context) {
        if (plant != null && plant.getCurrentHP() > 0) {
            onSunBeanBitten(plant, context);
        }
        if (plant == null || zombie == null || context == null || plant.getPosition() == null) return;
        Plant def = plant.getDefinition();
        if (!isGarlic(def)) return;
        int row = plant.getPosition().getY();
        int targetLane = pickAdjacentLane(row, context);
        if (targetLane != row) {
            context.moveZombieToLane(zombie, targetLane);
        }
    }

    private static void grantMetalArmor(PlantInstance plant, Plant def) {
        int bonus = (int) def.getPlantFoodValue();
        if (bonus <= 0) {
            bonus = def.getBaseHP();
        }
        plant.grantArmor(bonus, def.hasTag(PlantTags.EXPLOSIVE));
        if (isEndurian(def)) {
            int extra = def.getDamage() > 0 ? def.getDamage() : (int) def.getAbilityValue();
            plant.addReflectDamageBonus(Math.max(1, extra));
        }
    }

    @Override
    public void onPlantDeath(PlantInstance plant, PlantAbilityContext context) {
        if (plant == null || context == null) return;
        Plant def = plant.getDefinition();
        if (def == null) return;
        if (!def.hasTag(PlantTags.EXPLOSIVE)) return;

        if (plant.getPosition() == null) return;
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();

        int damage = (int) def.getAbilityValue();
        if (damage <= 0) damage = DEFAULT_EXPLODE_O_NUT_DAMAGE;
        damage += cumulativeExplodeDamageBuff(plant);

        int radius = DEFAULT_EXPLODE_O_NUT_RADIUS;
        for (ZombieInstance zombie : context.getZombiesInArea(row, col, radius, radius)) {
            context.damageZombie(zombie, damage);
        }
    }

    private void redirectOrReflect(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        Plant def = plant.getDefinition();

        // Damage reflect (Endurian)
        if (def.getDamage() > 0 && !def.hasTag(PlantTags.EXPLOSIVE)) {
            int reflectDamage = def.getDamage() + cumulativeReflectBuff(plant) + plant.getReflectDamageBonus();
            for (ZombieInstance zombie : context.getZombiesInArea(row, col, 0, 0)) {
                if (zombie.isEating() && zombie.getEatingTarget() == plant) {
                    context.damageZombie(zombie, reflectDamage);
                }
            }
        }

        // Lane redirect (Garlic, Sweet Potato)
        if (def.hasTag(PlantTags.MOVE_ZOMBIE)) {
            if (isSweepPotato(def)) {
                attractZombiesTo(plant, context);
            } else {
                redirectZombiesFrom(plant, context);
            }
        }
    }

    private void knockbackBlast(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        int row = plant.getPosition().getY();

        List<ZombieInstance> zombiesInLane = context.getZombiesInLane(row);
        boolean toUpper = row > 0;
        boolean toLower = row < context.getRowCount() - 1;

        boolean sendUpNext = true;
        for (ZombieInstance zombie : zombiesInLane) {
            if (zombie == null || zombie.isDead()) continue;

            int targetLane;
            if (toUpper && toLower) {
                targetLane = sendUpNext ? row - 1 : row + 1;
                sendUpNext = !sendUpNext;
            } else if (toUpper) {
                targetLane = row - 1;
            } else if (toLower) {
                targetLane = row + 1;
            } else {
                continue;
            }

            context.moveZombieToLane(zombie, targetLane);
        }
    }

    /** Picks an adjacent lane for the regular redirect. */
    private static int pickAdjacentLane(int row, PlantAbilityContext context) {
        boolean toUpper = row > 0;
        boolean toLower = row < context.getRowCount() - 1;
        if (toUpper && toLower) {
            return row % 2 == 0 ? row - 1 : row + 1;
        }
        if (toUpper) return row - 1;
        if (toLower) return row + 1;
        return row;
    }

    private int cumulativeExplodeDamageBuff(PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def == null || def.getLevels() == null) return 0;
        PlantLevels levels = def.getLevels();
        int total = 0;
        for (int lvl = 2; lvl <= 4; lvl++) {
            if (lvl > plant.getLevel()) break;
            LevelUpgrade upgrade = levels.getUpgrade(lvl);
            if (upgrade == null) continue;
            if (upgrade.isSpecialMechanic()
                    && upgrade.getSpecialTag() == PlantSpecialTag.EXPLODE_DAMAGE_BUFF) {
                total += (int) upgrade.getValue();
            }
        }
        return total;
    }

    private int cumulativeReflectBuff(PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def == null || def.getLevels() == null) return 0;
        PlantLevels levels = def.getLevels();
        int total = 0;
        for (int lvl = 2; lvl <= 4; lvl++) {
            if (lvl > plant.getLevel()) break;
            LevelUpgrade upgrade = levels.getUpgrade(lvl);
            if (upgrade == null) continue;
            if (upgrade.isSpecialMechanic()
                    && upgrade.getSpecialTag() == PlantSpecialTag.REFLECT_DAMAGE_BUFF) {
                total += (int) upgrade.getValue();
            }
        }
        return total;
    }

    /**
     * Helper called by the game systems when a zombie bites a Sun Bean
     * plant: drops {@code abilityValue} sun on the field next to the
     * plant.
     */
    public static void onSunBeanBitten(PlantInstance plant, PlantAbilityContext context) {
        if (plant == null || context == null || plant.getPosition() == null) return;
        Plant def = plant.getDefinition();
        if (def == null || !def.hasTag(PlantTags.SUN)) return;

        int amount = (int) def.getAbilityValue();
        if (amount <= 0) amount = DEFAULT_SUN_BEAN_PER_BITE;
        amount += cumulativeSunDropIncrement(plant);

        int col = plant.getPosition().getX();
        int row = plant.getPosition().getY();
        Sun sun = new Sun(SunType.NORMAL, amount, col, row);
        context.spawnSun(sun);
    }

    private static int cumulativeSunDropIncrement(PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def == null || def.getLevels() == null) return 0;
        PlantLevels levels = def.getLevels();
        int total = 0;
        for (int lvl = 2; lvl <= 4; lvl++) {
            if (lvl > plant.getLevel()) break;
            LevelUpgrade upgrade = levels.getUpgrade(lvl);
            if (upgrade == null) continue;
            if (upgrade.isSpecialMechanic()
                    && upgrade.getSpecialTag() == PlantSpecialTag.SUN_DROP_INCREMENT) {
                total += (int) upgrade.getValue();
            }
        }
        return total;
    }

    public boolean isSweepPotato(Plant def) {
        return isSweepPotatoStatic(def);
    }

    private static boolean isSweepPotatoStatic(Plant def) {
        if (def == null || def.getCategory() != PlantCategory.WALL_NUT) return false;
        if (!def.hasTag(PlantTags.MOVE_ZOMBIE)) return false;
        return def.getName() != null && def.getName().toLowerCase().contains("sweet potato");
    }

    private static boolean isGarlic(Plant def) {
        if (def == null || def.getCategory() != PlantCategory.WALL_NUT) return false;
        if (!def.hasTag(PlantTags.MOVE_ZOMBIE)) return false;
        return !isSweepPotatoStatic(def);
    }

    private static boolean isEndurian(Plant def) {
        if (def == null || def.getCategory() != PlantCategory.WALL_NUT) return false;
        if (def.hasTag(PlantTags.EXPLOSIVE)) return false;
        if (def.getName() != null && def.getName().toLowerCase().contains("endurian")) {
            return true;
        }
        return def.getDamage() > 0;
    }

    private static boolean isBeingEaten(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return false;
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        for (ZombieInstance zombie : context.getZombiesInArea(row, col, 0, 0)) {
            if (zombie != null && zombie.isEating() && zombie.getEatingTarget() == plant) {
                return true;
            }
        }
        return false;
    }

    /** Attracts nearby zombies from adjacent lanes onto this plant's lane. */
    public void attractZombiesTo(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        for (ZombieInstance zombie : context.getZombiesInArea(row, col, 1, 1)) {
            if (zombie.getGridPosition().getY() == row) continue;
            context.moveZombieToLane(zombie, row);
        }
    }

    /** Plant-food: pull every zombie in the adjacent lanes onto this lane. */
    private void attractAllAdjacentLanes(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        int row = plant.getPosition().getY();
        pullLaneOnto(context, row - 1, row);
        pullLaneOnto(context, row + 1, row);
    }

    private static void pullLaneOnto(PlantAbilityContext context, int fromRow, int toRow) {
        if (fromRow < 0 || fromRow >= context.getRowCount()) return;
        for (ZombieInstance zombie : context.getZombiesInLane(fromRow)) {
            if (zombie == null || zombie.isDead()) continue;
            context.moveZombieToLane(zombie, toRow);
        }
    }

    /** Redirects zombies that are eating the given {@code plant}. */
    public void redirectZombiesFrom(PlantInstance plant, PlantAbilityContext context) {
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        for (ZombieInstance zombie : context.getZombiesInArea(row, col, 0, 0)) {
            if (zombie.isEating() && zombie.getEatingTarget() == plant) {
                int targetLane = pickAdjacentLane(row, context);
                if (targetLane != row) {
                    context.moveZombieToLane(zombie, targetLane);
                }
            }
        }
    }
}
