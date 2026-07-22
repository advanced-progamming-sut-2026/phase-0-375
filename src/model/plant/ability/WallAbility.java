package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.PlantSpecialTag;
import model.enums.PlantTags;
import model.enums.SunType;
import model.game.map.FloatPoint;
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

    /** Default damage dealt by Explode-o-nut's death explosion. */
    private static final int DEFAULT_EXPLODE_O_NUT_DAMAGE = 1800;
    /** Default radius (in tiles) of Explode-o-nut's death explosion. */
    private static final int DEFAULT_EXPLODE_O_NUT_RADIUS = 1;
    /** Default sun dropped per bite the zombie takes from a Sun Bean. */
    private static final int DEFAULT_SUN_BEAN_PER_BITE = 5;

    @Override
    public PlantCategory getCategory() { return PlantCategory.WALL_NUT; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;

        // Reinforce-mint: trigger plant-food on every WALL_NUT plant.
        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.WALL_NUT);
            return;
        }

        // Sun Bean: passive - drop sun when a zombie eats it. The actual
        // sun-drop is triggered from ZombieSystem.handleEating via the
        // plant's SUN tag, so execute() is a no-op for Sun Bean.
        if (def.hasTag(PlantTags.SUN)) {
            return;
        }

        if (def.getDamage() > 0) {
            redirectOrReflect(plant, context);
        } else if (def.hasTag(PlantTags.MOVE_ZOMBIE)) {
            redirectOrReflect(plant, context);
        }
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        switch (def.getPlantFoodType()) {
            case GRANT_PERMANENT_ARMOR:
                int bonus = (int) def.getPlantFoodValue();
                plant.setCurrentHP(plant.getCurrentHP() + bonus);
                break;
            case KNOCKBACK_BLAST:
                // Garlic plant-food: shove every zombie in the lane
                // to an adjacent lane.
                knockbackBlast(plant, context);
                break;
            default:
                break;
        }
    }

    /**
     * Called by the game systems when this wall-nut plant dies. If the
     * plant has the {@link PlantTags#EXPLOSIVE} tag (Explode-o-nut), it
     * detonates in a 3x3 area dealing damage.
     */
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
            int reflectDamage = def.getDamage() + cumulativeReflectBuff(plant);
            for (ZombieInstance zombie : context.getZombiesInArea(row, col, 0, 0)) {
                if (zombie.isEating() && zombie.getEatingTarget() == plant) {
                    context.damageZombie(zombie, reflectDamage);
                }
            }
        }

        // Lane redirect (Garlic, Sweet Potato)
        if (def.hasTag(PlantTags.MOVE_ZOMBIE)) {
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

    private void knockbackBlast(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        int row = plant.getPosition().getY();

        List<ZombieInstance> zombiesInLane = context.getZombiesInLane(row);
        boolean toUpper = row > 0;
        boolean toLower = row < context.getRowCount() - 1;

        // If both adjacent lanes are available, alternate between them
        // to distribute the load. If only one is available, send all
        // zombies there.
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
                // nowhere to move.
                continue;
            }

            context.moveZombieToLane(zombie, targetLane);
        }
    }

    /** Picks an adjacent lane for the regular redirect. */
    private int pickAdjacentLane(int row, PlantAbilityContext context) {
        boolean toUpper = row > 0;
        boolean toLower = row < context.getRowCount() - 1;
        if (toUpper && toLower) {
            return row % 2 == 0 ? row - 1 : row + 1;
        }
        if (toUpper) return row - 1;
        if (toLower) return row + 1;
        return row;
    }

    /**
     * Sums up every {@link PlantSpecialTag#EXPLODE_DAMAGE_BUFF} upgrade
     * value the plant has accumulated.
     */
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

    /**
     * Sums up every {@link PlantSpecialTag#REFLECT_DAMAGE_BUFF} upgrade
     * value the plant has accumulated.
     */
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

    /**
     * Sums up every {@link PlantSpecialTag#SUN_DROP_INCREMENT} upgrade
     * value the plant has accumulated.
     */
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
}