package model.plant.ability;

import model.enums.PlantCategory;
import model.enums.PlantTags;
import model.game.map.FloatPoint;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Strategy for the {@link PlantCategory#EXPLOSIVE} family.
 */
public class ExplosiveAbility implements PlantAbility {

    /** Radius threshold above which an explosion is treated as mapwide. */
    private static final int MAPWIDE_THRESHOLD = 40;
    /** Radius threshold at/below which an explosion is a 3x3 or lane-clear. */
    private static final int LARGE_RADIUS = 9;

    /** Velocity of Grapeshot's bouncing grape projectiles. */
    private static final float GRAPE_VELOCITY = 2.0f;
    /** Number of grape projectiles Grapeshot fires per adjacent lane. */
    private static final int GRAPES_PER_LANE = 2;
    /** Damage dealt by each Grapeshot grape projectile. */
    private static final int GRAPE_DAMAGE = 300;

    @Override
    public PlantCategory getCategory() { return PlantCategory.EXPLOSIVE; }

    // --- Regular action ---

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;

        switch (def.getAbilityType()) {
            case INSTANT_EXPLOSIVE:
                handleInstant(plant, context);
                break;
            case DELAYED_EXPLOSIVE:
                handleDelayed(plant, context);
                break;
            case MINT_FAMILY_BOOST:
                // Bombard-mint: trigger plant-food on every EXPLOSIVE plant.
                context.triggerFamilyPlantFood(PlantCategory.EXPLOSIVE);
                break;
            default:
                break;
        }
    }

    // --- Instant explosives ---

    /**
     * Handles one-shot explosives that detonate immediately on placement.
     * After the effect resolves the plant is always destroyed.
     */
    private void handleInstant(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();

        // Ice-shroom: mapwide freeze, no damage.
        if (def.hasTag(PlantTags.ICE) && def.getDamage() == 0) {
            freezeAllZombies(context);
            context.destroyPlant(plant);
            return;
        }

        // Ice-shroom variant with damage (level 4 upgrade): freeze + damage.
        if (def.hasTag(PlantTags.ICE) && def.getDamage() > 0) {
            freezeAllZombies(context);
            // Fall through to also deal damage.
        }

        // Grapeshot: 3x3 explosion + bouncing grape projectiles.
        if (isGrapeshot(def)) {
            detonate(plant, context);
            spawnGrapes(plant, context);
            context.destroyPlant(plant);
            return;
        }

        // All other instant explosives (Cherry Bomb, Jalapeno,
        // Doom-shroom, Hot Potato, Grave Buster, …).
        detonate(plant, context);
        context.destroyPlant(plant);
    }

    // --- Delayed explosives (traps) ---

    /**
     * Handles trap-style explosives that arm and then wait for a zombie
     * to enter their trigger area.
     */
    private void handleDelayed(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        AbilityState state = plant.getAbilityState(plant.getDefinition().getAbilityType());
        if (state == null) return;

        Plant def = plant.getDefinition();
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();

        // Phase 1: Arm the trap if it hasn't been armed yet.
        if (!state.isArmed()) {
            state.setArmed(true);
            return;
        }

        // Phase 2: Check trigger conditions.
        List<ZombieInstance> triggers = getTriggerZombies(plant, context, row, col);
        if (triggers.isEmpty()) return;

        // Phase 3: Detonate.
        if (def.hasTag(PlantTags.ICE)) {
            // Iceberg Lettuce: freeze the triggering zombies (no damage).
            for (ZombieInstance zombie : triggers) {
                freezeZombie(zombie);
            }
        } else {
            // All other traps: standard detonation.
            detonate(plant, context);
        }
        context.destroyPlant(plant);
    }

    /**
     * Returns the zombies that trigger this trap. Most traps trigger on
     * same-tile contact. Squash triggers on a wider area - any zombie
     * in the same tile or up to 2 tiles ahead in the lane.
     */
    private List<ZombieInstance> getTriggerZombies(PlantInstance plant,
                                                   PlantAbilityContext context,
                                                   int row, int col) {
        Plant def = plant.getDefinition();

        // Squash: checks 2 tiles ahead (toward the zombie spawn).
        if (isSquash(def)) {
            return context.getZombiesInArea(row, col, 0, 2);
        }

        // Default: same-tile trigger.
        return context.getZombiesInArea(row, col, 0, 0);
    }

    // --- Detonation core ---

    /**
     * Detonates the plant at its position. The blast radius is read
     * from the plant's {@code abilityValue}. The blast shape depends
     * on the radius and the plant's tags.
     */
    private void detonate(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        int radius = (int) def.getAbilityValue();
        if (radius <= 0) radius = 1;
        detonateAt(plant, context, radius);
    }

    /**
     * Detonates the plant with a caller-specified radius. Used by both
     * the regular detonation and the plant-food LOCAL_AOE_ATTACK path.
     */
    private void detonateAt(PlantInstance plant, PlantAbilityContext context, int radius) {
        if (plant.getPosition() == null) return;
        Plant def = plant.getDefinition();
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        boolean isFire = def.hasTag(PlantTags.FIRE);
        int damage = def.getDamage();

        // Mapwide explosion (Doom-shroom).
        if (radius >= MAPWIDE_THRESHOLD) {
            for (int lane = 0; lane < context.getRowCount(); lane++) {
                for (ZombieInstance zombie : context.getZombiesInLane(lane)) {
                    applyExplosionDamage(zombie, damage, isFire);
                }
            }
            return;
        }

        // Lane-clearing explosion (Jalapeno).
        if (isFire && radius >= LARGE_RADIUS) {
            for (ZombieInstance zombie : context.getZombiesInLane(row)) {
                applyExplosionDamage(zombie, damage, isFire);
            }
            return;
        }

        // 3x3 AoE (Cherry Bomb, Grapeshot, Primal Potato Mine).
        if (radius >= LARGE_RADIUS) {
            for (ZombieInstance zombie : context.getZombiesInArea(row, col, 1, 1)) {
                applyExplosionDamage(zombie, damage, isFire);
            }
            return;
        }

        // Localised explosion (Potato Mine).
        for (ZombieInstance zombie : context.getZombiesInArea(row, col, radius, radius)) {
            applyExplosionDamage(zombie, damage, isFire);
        }
    }

    /** Applies explosion damage to a single zombie. */
    private void applyExplosionDamage(ZombieInstance zombie, int damage, boolean isFire) {
        if (zombie == null || zombie.isDead() || damage <= 0) return;
        if (isFire) {
            zombie.takeFireDamage(damage);
        } else {
            zombie.takeDamage(damage);
        }
    }

    // --- Grapeshot bouncing grapes ---

    /**
     * Fires a volley of grape projectiles down the plant's lane and
     * each adjacent lane. Each grape deals {@value #GRAPE_DAMAGE}
     * damage and travels at {@value #GRAPE_VELOCITY} grid-units/sec.
     */
    private void spawnGrapes(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        int row = plant.getPosition().getY();
        float originX = plant.getPosition().getX() + 0.5f;

        // Fire grapes in the plant's lane and each adjacent lane.
        for (int laneOffset = -1; laneOffset <= 1; laneOffset++) {
            int lane = row + laneOffset;
            if (lane < 0 || lane >= context.getRowCount()) continue;

            for (int i = 0; i < GRAPES_PER_LANE; i++) {
                Pellet grape = new Pellet(
                        GRAPE_DAMAGE,
                        new FloatPoint(originX + i * 0.3f, lane),
                        lane,
                        GRAPE_VELOCITY,
                        Projectile.Element.NONE,
                        +1
                );
                context.spawnProjectile(grape, grape.getX(), grape.getY());
            }
        }
    }

    // --- Freeze helpers ---

    /** Applies 3 chill stacks (full freeze) to every zombie on the field. */
    private void freezeAllZombies(PlantAbilityContext context) {
        for (int lane = 0; lane < context.getRowCount(); lane++) {
            for (ZombieInstance zombie : context.getZombiesInLane(lane)) {
                freezeZombie(zombie);
            }
        }
    }

    /** Applies 3 chill stacks (full freeze) to a single zombie. */
    private void freezeZombie(ZombieInstance zombie) {
        if (zombie == null || zombie.isDead()) return;
        zombie.applyChill();
        zombie.applyChill();
        zombie.applyChill();
    }

    // --- Plant food ---

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        switch (def.getPlantFoodType()) {
            case SPAWN_CLONES:
                spawnClones(plant, context, (int) def.getPlantFoodValue());
                break;
            case MAP_WIDE_FREEZE:
                freezeAllZombies(context);
                break;
            case LOCAL_AOE_ATTACK:
                detonateAt(plant, context, (int) def.getPlantFoodValue());
                break;
            case PULL_UNDERWATER:
                pullZombiesUnderwater(plant, context, (int) def.getPlantFoodValue());
                break;
            default:
                detonate(plant, context);
                break;
        }
    }

    // --- Plant-food: SPAWN_CLONES (Potato Mine, Primal Potato Mine) ---

    /**
     * Spawns up to {@code count} armed clones of this plant on empty
     * tiles near the original. Clones are pre-armed so they detonate
     * immediately on zombie contact (no charge delay).
     */
    private void spawnClones(PlantInstance plant, PlantAbilityContext context, int count) {
        if (count <= 0 || plant.getPosition() == null) return;
        Plant def = plant.getDefinition();
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        int spawned = 0;

        // Search outward from the plant for empty tiles.
        for (int radius = 1; radius <= 3 && spawned < count; radius++) {
            for (int rowDist = -radius; rowDist <= radius && spawned < count; rowDist++) {
                for (int colDist = -radius; colDist <= radius && spawned < count; colDist++) {
                    if (Math.abs(rowDist) != radius && Math.abs(colDist) != radius) continue;
                    int targetRow = row + rowDist;
                    int targetCol = col + colDist;
                    if (targetRow < 0 || targetCol < 0
                            || targetRow >= context.getRowCount()
                            || targetCol >= context.getColumnCount()) continue;
                    if (context.getPlantAt(targetRow, targetCol) != null) continue;

                    PlantInstance clone = PlantFactory.createInstance(def.getName());
                    if (clone == null) continue;

                    // Pre-arm the clone so it triggers immediately.
                    AbilityState cloneState = clone.getAbilityState(def.getAbilityType());
                    if (cloneState != null) {
                        cloneState.setArmed(true);
                        cloneState.setCooldownRemaining(0);
                    }

                    if (context.placePlant(clone, targetRow, targetCol)) {
                        spawned++;
                    }
                }
            }
        }
    }

    // --- Plant-food: PULL_UNDERWATER (Tangle Kelp) ---

    /**
     * Grabs up to {@code maxTargets} zombies in the plant's lane and
     * drags them underwater (instant kill).
     */
    private void pullZombiesUnderwater(PlantInstance plant, PlantAbilityContext context, int maxTargets) {
        if (plant.getPosition() == null || maxTargets <= 0) return;
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        int damage = plant.getDefinition().getDamage();
        int grabbed = 0;

        // Prioritize zombies closest to the plant.
        List<ZombieInstance> zombiesInLane = context.getZombiesInLane(row);
        for (ZombieInstance zombie : zombiesInLane) {
            if (grabbed >= maxTargets) break;
            if (zombie == null || zombie.isDead()) continue;
            int zombieCol = zombie.getGridPosition() != null ? zombie.getGridPosition().getX() : 0;
            if (Math.abs(zombieCol - col) > 4) continue;  // only grab nearby zombies
            context.damageZombie(zombie, damage);
            grabbed++;
        }
    }

    // --- Plant-type detection helpers ---

    /** @return true if the plant is a Grapeshot (fires bouncing grapes). */
    private boolean isGrapeshot(Plant def) {
        return def.getName() != null
                && def.getName().toLowerCase().contains("grape");
    }

    /** @return true if the plant is a Squash (smashes adjacent zombies). */
    private boolean isSquash(Plant def) {
        return def.getName() != null
                && def.getName().toLowerCase().contains("squash");
    }
}