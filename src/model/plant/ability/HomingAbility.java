package model.plant.ability;

import model.enums.*;
import model.game.map.FloatPoint;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Strategy for the {@link PlantCategory#HOMING} family.
 */
public class HomingAbility implements PlantAbility {

    private static final float PELLET_VELOCITY = 1.0f;

    private static final int BURST_PROJ_DAMAGE = 6767;

    @Override
    public PlantCategory getCategory() { return PlantCategory.HOMING; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null || plant.getPosition() == null) return;

        switch (def.getAbilityType()) {
            case MINT_FAMILY_BOOST:
                context.triggerFamilyPlantFood(PlantCategory.HOMING);
                break;

            case SHOOT_PROJECTILE:
                fireHomingPellet(plant, context);
                break;

            case MODIFIER_UTILITY:
                // Magnet-shroom: pull metal from the nearest
                // metal-carrying zombie in the lane.
                pullMetalFromNearest(plant, context);
                break;

            default:
                break;
        }
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        switch (def.getPlantFoodType()) {
            case RANDOM_HYPNOTIZE:
                hypnotiseRandomZombies(context, (int) def.getPlantFoodValue());
                break;
            case KNOCKBACK_BLAST:
                // Magnet-shroom plant-food
                pullMetalAndStunAll(context, (int) def.getPlantFoodValue());
                break;
            case PROJECTILE_BURST:
                // cat-tail plant-food
                burstProjectile(context, plant);
                break;
            default:
                break;
        }
    }

    // --- Homing shooter ---

    private void fireHomingPellet(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
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
        pellet.setHomingTarget(target);
        context.spawnProjectile(pellet, pellet.getX(), pellet.getY());
    }

    // --- Magnet-shroom regular action ---

    /**
     * Finds the nearest metal-carrying zombie in the Magnet-shroom's
     * lane and destroys its metallic armor.
     */
    private void pullMetalFromNearest(PlantInstance plant, PlantAbilityContext context) {
        int row = plant.getPosition().getY();
        int plantCol = plant.getPosition().getX();

        ZombieInstance nearest = null;
        int bestDist = Integer.MAX_VALUE;

        for (ZombieInstance zombie : context.getZombiesInLane(row)) {
            if (zombie == null || zombie.isDead()) continue;
            if (!hasMetal(zombie)) continue;

            int zCol = zombie.getGridPosition() != null ? zombie.getGridPosition().getX() : 0;
            int dist = Math.abs(zCol - plantCol);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = zombie;
            }
        }
        if (nearest != null) {
            stripMetalArmour(nearest);
        }
    }

    // --- Magnet-shroom plant-food ---

    /**
     * Strips every piece of metallic armor from every zombie on the
     * field, then freezes each affected zombie solid (3 chill stacks)
     * and deals a small bonus damage.
     */
    private void pullMetalAndStunAll(PlantAbilityContext context, int stunDamage) {
        for (int lane = 0; lane < context.getRowCount(); lane++) {
            for (ZombieInstance zombie : context.getZombiesInLane(lane)) {
                if (zombie == null || zombie.isDead()) continue;
                if (!stripMetalArmour(zombie)) continue;

                // Freeze the zombie solid (3 chill stacks). The existing
                // chill / unfreeze machinery ticks the stacks back down.
                zombie.applyChill();
                zombie.applyChill();
                zombie.applyChill();

                if (stunDamage > 0) {
                    context.damageZombie(zombie, stunDamage);
                }
            }
        }
    }

    // --- Metal-stripping helpers ---

    /** @return true if the zombie currently carries at least one metallic armour piece. */
    private static boolean hasMetal(ZombieInstance zombie) {
        List<Armor> armors = zombie.getArmors();
        if (armors == null || armors.isEmpty()) return false;
        for (Armor armor : armors) {
            if (armor != null && armor.isMetallic() && !armor.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Destroys every metallic armor piece on the zombie.
     *
     * @return true if at least one metal piece was stripped
     */
    private static boolean stripMetalArmour(ZombieInstance zombie) {
        boolean stripped = false;
        List<Armor> armors = zombie.getArmors();
        if (armors == null || armors.isEmpty()) return false;

        for (Armor armor : armors) {
            if (armor != null && armor.isMetallic() && !armor.isDestroyed()) {
                armor.setCurrentHealth(0);
                stripped = true;
            }
        }
        if (stripped) {
            zombie.removeDestroyedArmor();
        }
        return stripped;
    }

    // --- Caulipower plant-food ---

    private void hypnotiseRandomZombies(PlantAbilityContext context, int count) {
        for (int lane = 0; lane < context.getRowCount() && count > 0; lane++) {
            List<ZombieInstance> zombiesInLine = context.getZombiesInLane(lane);
            for (ZombieInstance zombie : zombiesInLine) {
                if (count <= 0) break;
                zombie.setState(ZombieState.HYPNOTIZED);
                zombie.setMovingBackward(true);
                count--;
            }
        }
    }

    // --- cat-tail plant-food ---

    private void burstProjectile(PlantAbilityContext context, PlantInstance plant) {
        ZombieInstance target = pickTarget(plant, context);
        if (target == null) return;

        Plant def = plant.getDefinition();
        if (def == null) return;

        FloatPoint origin = new FloatPoint(
                plant.getPosition().getX() + 0.5f,
                plant.getPosition().getY()
        );

        Pellet pellet = new Pellet(
                BURST_PROJ_DAMAGE,
                origin,
                plant.getPosition().getY(),
                PELLET_VELOCITY,
                Projectile.Element.NONE,
                +1
        );
        pellet.setHomingTarget(target);
        context.spawnProjectile(pellet, pellet.getX(), pellet.getY());
    }

    // --- Target selection ---

    private ZombieInstance pickTarget(PlantInstance plant, PlantAbilityContext context) {
        if (hasPrioritizeGargantuars(plant)) {
            ZombieInstance garg = findGargantuar(context);
            if (garg != null) return garg;
        }

        // Default policy: prefer the highest-HP zombie on the field.
        ZombieInstance best = null;
        for (int lane = 0; lane < context.getRowCount(); lane++) {
            for (ZombieInstance zombie : context.getZombiesInLane(lane)) {
                if (best == null || zombie.getCurrentHP() > best.getCurrentHP()) {
                    best = zombie;
                }
            }
        }
        return best;
    }

    /** @return true if the plant has the PRIORITIZE_GARGANTUARS upgrade. */
    private boolean hasPrioritizeGargantuars(PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def == null || def.getLevels() == null) return false;
        for (int lvl = 2; lvl <= 4; lvl++) {
            if (lvl > plant.getLevel()) break;
            LevelUpgrade upgrade = def.getLevels().getUpgrade(lvl);
            if (upgrade == null) continue;
            if (upgrade.isSpecialMechanic()
                    && upgrade.getSpecialTag() == PlantSpecialTag.PRIORITIZE_GARGANTUARS) {
                return true;
            }
        }
        return false;
    }

    /** @return the first alive Gargantuar on the field, or {@code null}. */
    private ZombieInstance findGargantuar(PlantAbilityContext context) {
        for (int lane = 0; lane < context.getRowCount(); lane++) {
            for (ZombieInstance zombie : context.getZombiesInLane(lane)) {
                if (zombie == null || zombie.isDead()) continue;
                String name = zombie.getDefinition().getName();
                if (name != null && name.toLowerCase().contains("gargantuar")) {
                    return zombie;
                }
            }
        }
        return null;
    }
}