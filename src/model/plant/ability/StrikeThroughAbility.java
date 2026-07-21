package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.PlantSpecialTag;
import model.game.map.FloatPoint;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.definition.PlantLevels;
import model.plant.instance.PlantInstance;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

/**
 * Strategy for the {@link PlantCategory#STRIKE_THROUGH} family.
 */
public class StrikeThroughAbility implements PlantAbility {

    private static final float PELLET_VELOCITY = 1f;

    /** Distance (in grid units) that Fume-shroom's plant food pushes zombies back. */
    private static final float KNOCKBACK_TILES = 1.5f;

    /** Cactus projectile damage when it's on plant food. */
    private static final int BURST_DAMAGE = 100;

    /** Cactus projectile count when it's on plant food. */
    private static final int BURST_PROJ_COUNT = 3;

    @Override
    public PlantCategory getCategory() { return PlantCategory.STRIKE_THROUGH; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        Plant def = plant.getDefinition();
        if (def == null) return;

        // Pierce-mint: trigger plant-food on every STRIKE_THROUGH plant.
        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.STRIKE_THROUGH);
            return;
        }

        if (def.getAbilityType() != PlantAbilityType.SHOOT_PROJECTILE) return;
        if (!context.hasZombieInLane(plant.getPosition().getY())) return;

        int row = plant.getPosition().getY();
        float rangeBonus = cumulativeSpecialValue(plant, PlantSpecialTag.TILE_RANGE_EXT);
        FloatPoint origin = new FloatPoint(plant.getPosition().getX() + 0.5f + rangeBonus, row);

        Pellet pellet = new Pellet(
                def.getDamage(),
                new FloatPoint(origin.getX(), origin.getY()),
                row,
                PELLET_VELOCITY,
                Projectile.Element.NONE,
                +1
        );
        pellet.setPierce(true);
        context.spawnProjectile(pellet, pellet.getX(), pellet.getY());
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (plant.getPosition() == null) return;

        if (def.getPlantFoodType() == PlantFoodType.KNOCKBACK_BLAST) {
            int row = plant.getPosition().getY();
            for (ZombieInstance zombie : context.getZombiesInLane(row)) {
                context.pushZombieBack(zombie, KNOCKBACK_TILES);
            }
        }

        else if (def.getPlantFoodType() == PlantFoodType.PROJECTILE_BURST) {
            int row = plant.getPosition().getY();
            for (ZombieInstance zombie : context.getZombiesInLane(row)) {
                int damage = (int) def.getPlantFoodValue();
                if (damage <= 0) {
                    damage = BURST_DAMAGE;
                }
                float rangeBonus = cumulativeSpecialValue(plant, PlantSpecialTag.TILE_RANGE_EXT);
                FloatPoint origin = new FloatPoint(plant.getPosition().getX() + 0.5f + rangeBonus, row);
                for (int i = 0; i < BURST_PROJ_COUNT; i++) {
                    Pellet pellet = new Pellet(
                            def.getDamage(),
                            new FloatPoint(origin.getX() + i * 0.05f, origin.getY()),
                            row,
                            PELLET_VELOCITY,
                            Projectile.Element.NONE,
                            +1
                    );
                    pellet.setPierce(true);
                    context.spawnProjectile(pellet, pellet.getX(), pellet.getY());
                }
            }
        }
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