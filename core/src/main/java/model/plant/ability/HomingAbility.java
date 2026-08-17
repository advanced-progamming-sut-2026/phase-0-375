package model.plant.ability;

import model.enums.*;
import model.game.map.FloatPoint;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.zombie.armor.Armor;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Strategy for the {@link PlantCategory#HOMING} family.
 */
public class HomingAbility implements PlantAbility {

    private static final float PELLET_VELOCITY = 1.0f;

    private static final int BURST_PROJ_DAMAGE = 6767;

    private static final int ONE_HIT_DAMAGE = BURST_PROJ_DAMAGE;

    private static final Random RNG = new Random();

    @Override
    public PlantCategory getCategory() { return PlantCategory.HOMING; }

    @Override
    public PlantAction beginAction(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null || plant.getPosition() == null) return null;

        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.HOMING);
            return null;
        }

        if (def.getAbilityType() == PlantAbilityType.SHOOT_PROJECTILE) {
            if (pickTarget(plant, context) == null) return null;
            return TimedPlantAction.attackAt(plant, context, this::execute);
        }

        if (def.getAbilityType() == PlantAbilityType.MODIFIER_UTILITY) {
            if (!hasMetalInRange(plant, context)) return null;
            execute(plant, context);
            return TimedPlantAction.attackHold(plant, context);
        }

        return null;
    }

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
                if (plant.getDefinition().hasTag(PlantTags.MAGIC)) {
                    hypnotiseRandomZombies(context, (int) def.getPlantFoodValue());
                } else {
                    killRandomZombies(context, (int) def.getPlantFoodValue());
                }
                break;
            case KNOCKBACK_BLAST:
                // Magnet-shroom plant-food
                ArmorType pulled = pullMetalAndStunAll(context, (int) def.getPlantFoodValue());
                AbilityState pfState = plant.getAbilityState(def.getAbilityType());
                if (pulled != null && pfState != null) {
                    pfState.setHeldMetal(pulled);
                }
                break;
            case PROJECTILE_BURST:
                // Cat-tail: barrage of homing shots.
                burstHomingVolley(context, plant);
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

        FloatPoint origin = context.plantProjectileOriginOrCell(plant);
        // Caulipower's bolt hypnotizes on hit instead of dealing lethal damage.
        int damage = def.hasTag(PlantTags.MAGIC) ? 0 : def.getDamage();
        Pellet pellet = new Pellet(
                damage,
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
        ZombieInstance nearest = findNearestMetalZombie(plant, context);
        if (nearest == null) {
            return;
        }
        ArmorType pulled = stripMetalArmour(nearest);
        AbilityState state = plant.getAbilityState(plant.getDefinition().getAbilityType());
        if (pulled != null && state != null) {
            state.setHeldMetal(pulled);
        }
    }

    private boolean hasMetalInRange(PlantInstance plant, PlantAbilityContext context) {
        return findNearestMetalZombie(plant, context) != null;
    }

    private ZombieInstance findNearestMetalZombie(PlantInstance plant, PlantAbilityContext context) {
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
        return nearest;
    }

    // --- Magnet-shroom plant-food ---

    /**
     * Strips every piece of metallic armor from every zombie on the
     * field, then freezes each affected zombie solid (3 chill stacks)
     * and deals a small bonus damage.
     */
    private ArmorType pullMetalAndStunAll(PlantAbilityContext context, int stunDamage) {
        ArmorType lastPulled = null;
        for (int lane = 0; lane < context.getRowCount(); lane++) {
            for (ZombieInstance zombie : context.getZombiesInLane(lane)) {
                if (zombie == null || zombie.isDead()) continue;
                ArmorType pulled = stripMetalArmour(zombie);
                if (pulled == null) continue;
                lastPulled = pulled;

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
        return lastPulled;
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
     * @return the first metal type stripped, or {@code null} if none
     */
    private static ArmorType stripMetalArmour(ZombieInstance zombie) {
        ArmorType pulled = null;
        List<Armor> armors = zombie.getArmors();
        if (armors == null || armors.isEmpty()) return null;

        for (Armor armor : armors) {
            if (armor != null && armor.isMetallic() && !armor.isDestroyed()) {
                if (pulled == null) {
                    pulled = armor.getType();
                }
                armor.setCurrentHealth(0);
            }
        }
        if (pulled != null) {
            zombie.removeDestroyedArmor();
        }
        return pulled;
    }

    // --- Caulipower plant-food ---

    private void hypnotiseRandomZombies(PlantAbilityContext context, int count) {
        List<ZombieInstance> allZombies = new ArrayList<>();
        for (int lane = 0; lane < context.getRowCount(); lane++) {
            allZombies.addAll(context.getZombiesInLane(lane));
        }

        while (count > 0 && !allZombies.isEmpty()) {
            int randomZombieIndex = RNG.nextInt(allZombies.size());
            ZombieInstance randomZombie = allZombies.get(randomZombieIndex);
            if (randomZombie.isDead() || randomZombie.isHypnotized()) {
                allZombies.remove(randomZombieIndex);
                continue;
            }
            randomZombie.setState(ZombieState.HYPNOTIZED);
            randomZombie.setMovingBackward(true);
            count--;
        }
    }

    // --- Electric Blueberry plant-food ---

    private void killRandomZombies(PlantAbilityContext context, int count) {
        for (int lane = 0; lane < context.getRowCount() && count > 0; lane++) {
            List<ZombieInstance> zombiesInLine = context.getZombiesInLane(lane);
            for (ZombieInstance zombie : zombiesInLine) {
                if (count <= 0) break;
                zombie.takeDamage(ONE_HIT_DAMAGE);
                count--;
            }
        }
    }

    // --- cat-tail plant-food ---

    private void burstHomingVolley(PlantAbilityContext context, PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def == null || plant.getPosition() == null) return;

        int volley = (int) def.getPlantFoodValue();
        if (volley <= 0) return;

        List<ZombieInstance> targets = listHomingTargets(context);
        if (targets.isEmpty()) return;

        FloatPoint origin = context.plantProjectileOriginOrCell(plant);
        int row = plant.getPosition().getY();
        int damage = Math.max(1, def.getDamage());

        for (int i = 0; i < volley; i++) {
            ZombieInstance target = targets.get(i % targets.size());
            if (target == null || target.isDead() || target.isHypnotized()) {
                target = pickTarget(plant, context);
                if (target == null) break;
            }
            float dx = (i % 5) * 0.08f;
            float dy = ((i % 3) - 1) * 0.05f;
            FloatPoint shotOrigin = new FloatPoint(origin.getX() + dx, origin.getY() + dy);
            Pellet pellet = new Pellet(
                    damage,
                    shotOrigin,
                    row,
                    PELLET_VELOCITY * 1.25f,
                    Projectile.Element.NONE,
                    +1
            );
            pellet.setHomingTarget(target);
            context.spawnProjectile(pellet, pellet.getX(), pellet.getY());
        }
    }

    private List<ZombieInstance> listHomingTargets(PlantAbilityContext context) {
        List<ZombieInstance> targets = new ArrayList<>();
        for (int lane = 0; lane < context.getRowCount(); lane++) {
            for (ZombieInstance zombie : context.getZombiesInLane(lane)) {
                if (zombie == null || zombie.isDead() || zombie.isHypnotized()) continue;
                targets.add(zombie);
            }
        }
        return targets;
    }

    // --- Target selection ---

    private ZombieInstance pickTarget(PlantInstance plant, PlantAbilityContext context) {
        if (hasPrioritizeGargantuars(plant)) {
            ZombieInstance garg = findGargantuar(context);
            if (garg != null) return garg;
        }

        Plant def = plant.getDefinition();
        if (isCatTail(def)) {
            return findNearestZombie(plant, context);
        }

        // Caulipower / Electric Blueberry: a random zombie on the field.
        return pickRandomZombie(context);
    }

    private static boolean isCatTail(Plant def) {
        String name = def != null ? def.getName() : null;
        return name != null && name.equalsIgnoreCase("Cat-tail");
    }

    private ZombieInstance findNearestZombie(PlantInstance plant, PlantAbilityContext context) {
        float originX = plant.getPosition().getX();
        float originY = plant.getPosition().getY();
        ZombieInstance best = null;
        float bestDist = Float.MAX_VALUE;
        for (int lane = 0; lane < context.getRowCount(); lane++) {
            for (ZombieInstance zombie : context.getZombiesInLane(lane)) {
                if (zombie == null || zombie.isDead() || zombie.isHypnotized()) continue;
                if (zombie.getContinuousPosition() == null) continue;
                float dx = zombie.getContinuousX() - originX;
                float dy = zombie.getContinuousY() - originY;
                float dist = dx * dx + dy * dy;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = zombie;
                }
            }
        }
        return best;
    }

    private ZombieInstance pickRandomZombie(PlantAbilityContext context) {
        List<ZombieInstance> candidates = new ArrayList<>();
        for (int lane = 0; lane < context.getRowCount(); lane++) {
            for (ZombieInstance zombie : context.getZombiesInLane(lane)) {
                if (zombie == null || zombie.isDead() || zombie.isHypnotized()) continue;
                candidates.add(zombie);
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(RNG.nextInt(candidates.size()));
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
