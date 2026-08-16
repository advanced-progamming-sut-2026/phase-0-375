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
import model.projectile.FumeCloud;
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

    /** Length of {@code FUMESHROOM_BUBBLES} {@code special} clip, in seconds. */
    private static final float FUME_BUBBLE_LIFETIME = 1.2f;

    @Override
    public PlantCategory getCategory() { return PlantCategory.STRIKE_THROUGH; }

    @Override
    public PlantAction beginAction(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return null;
        Plant def = plant.getDefinition();
        if (def == null) return null;

        // Pierce-mint: trigger plant-food on every STRIKE_THROUGH plant.
        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.STRIKE_THROUGH);
            return null;
        }

        if (def.getAbilityType() != PlantAbilityType.SHOOT_PROJECTILE) return null;
        if (def.isShroom()) {
            if (!hasZombieInFumeRange(plant, context)) return null;
        } else if (!context.hasZombieInLane(plant.getPosition().getY())) {
            return null;
        }

        execute(plant, context);
        return TimedPlantAction.attackHold(plant, context);
    }

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
        if (def.isShroom()) {
            shootFume(plant, context);
            return;
        }
        if (!context.hasZombieInLane(plant.getPosition().getY())) return;

        int row = plant.getPosition().getY();
        FloatPoint origin = new FloatPoint(plant.getPosition().getX() + 0.5f, row);

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

    /**
     * Spawns a stationary bubble on each tile in front of Fume-shroom.
     * Range is {@code abilityValue} tiles, plus {@link PlantSpecialTag#TILE_RANGE_EXT}.
     */
    private void shootFume(PlantInstance plant, PlantAbilityContext context) {
        if (!hasZombieInFumeRange(plant, context)) return;

        Plant def = plant.getDefinition();
        int row = plant.getPosition().getY();
        float plantX = plant.getPosition().getX();
        int tiles = fumeTileCount(plant);

        for (int i = 1; i <= tiles; i++) {
            FumeCloud bubble = new FumeCloud(
                    def.getDamage(),
                    new FloatPoint(plantX + i, row),
                    row,
                    FUME_BUBBLE_LIFETIME
            );
            context.spawnProjectile(bubble, bubble.getX(), bubble.getY());
        }
    }

    private boolean hasZombieInFumeRange(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return false;
        int row = plant.getPosition().getY();
        float plantX = plant.getPosition().getX();
        float range = fumeTileCount(plant);
        for (ZombieInstance zombie : context.getZombiesInLane(row)) {
            if (zombie == null || zombie.isDead() || zombie.getContinuousPosition() == null) continue;
            if (zombie.isHypnotized()) continue;
            float dx = zombie.getContinuousX() - plantX;
            if (dx > 0f && dx <= range) {
                return true;
            }
        }
        return false;
    }

    /** Number of tiles ahead covered by a fume burst. */
    private int fumeTileCount(PlantInstance plant) {
        Plant def = plant.getDefinition();
        float range = (def != null ? def.getAbilityValue() : 0f)
                + cumulativeSpecialValue(plant, PlantSpecialTag.TILE_RANGE_EXT);
        return Math.max(1, Math.round(range));
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (plant.getPosition() == null) return;

        if (def.isShroom()) {
            for (PlantInstance plantInstance : context.getAllPlants()) {
                if (plantInstance.getDefinition().getName().equals(def.getName())) {
                    plantInstance.setLifespanRemaining(PlantInstance.SHROOM_BASE_LIFESPAN);
                }
            }
        }

        if (def.getPlantFoodType() == PlantFoodType.KNOCKBACK_BLAST) {
            int row = plant.getPosition().getY();
            for (ZombieInstance zombie : context.getZombiesInLane(row)) {
                context.pushZombieBack(zombie, KNOCKBACK_TILES);
            }
        }

        else if (def.getPlantFoodType() == PlantFoodType.PROJECTILE_BURST) {
            int row = plant.getPosition().getY();
            for (int i = 0; i < context.getZombiesInLane(row).size(); i++) {
                float rangeBonus = cumulativeSpecialValue(plant, PlantSpecialTag.TILE_RANGE_EXT);
                FloatPoint origin = new FloatPoint(plant.getPosition().getX() + 0.5f + rangeBonus, row);
                for (int j = 0; j < BURST_PROJ_COUNT; j++) {
                    Pellet pellet = new Pellet(
                            def.getDamage(),
                            new FloatPoint(origin.getX() + j * 0.05f, origin.getY()),
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