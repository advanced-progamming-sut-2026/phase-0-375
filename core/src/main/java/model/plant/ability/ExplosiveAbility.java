package model.plant.ability;

import model.enums.PlantCategory;
import model.enums.PlantSpecialTag;
import model.enums.PlantState;
import model.enums.PlantTags;
import model.game.map.FloatPoint;
import model.game.map.terrain.IceTerrainStrategy;
import model.plant.PlantFactory;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.definition.PlantLevels;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    /** Grapes despawn this many seconds after being thrown. */
    private static final float GRAPE_LIFETIME = 5.0f;

    private static final Random RNG = new Random();

    @Override
    public PlantCategory getCategory() { return PlantCategory.EXPLOSIVE; }

    @Override
    public PlantAction beginAction(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return null;

        switch (def.getAbilityType()) {
            case INSTANT_EXPLOSIVE:
                return explodeClipThen(plant, context, this::resolveInstant);
            case DELAYED_EXPLOSIVE:
                return beginDelayed(plant, context);
            case MINT_FAMILY_BOOST:
                context.triggerFamilyPlantFood(PlantCategory.EXPLOSIVE);
                return null;
            default:
                return null;
        }
    }

    // --- Regular action ---

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;

        switch (def.getAbilityType()) {
            case INSTANT_EXPLOSIVE:
                resolveInstant(plant, context);
                break;
            case DELAYED_EXPLOSIVE:
                handleDelayed(plant, context, true);
                break;
            case MINT_FAMILY_BOOST:
                context.triggerFamilyPlantFood(PlantCategory.EXPLOSIVE);
                break;
            default:
                break;
        }
    }

    /**
     * Holds {@link PlantState#ATTACKING} for the explode/smash clip, fires the
     * effect mid-clip, then removes the plant when the clip ends.
     */
    private PlantAction explodeClipThen(PlantInstance plant, PlantAbilityContext context,
                                        TimedPlantAction.Effect onFire) {
        return new TimedPlantAction(
                PlantState.ATTACKING,
                TimedPlantAction.presentationDurationFor(
                        plant, context, PlantState.ATTACKING, TimedPlantAction.DEFAULT_ATTACK_DURATION),
                null,
                onFire,
                TimedPlantAction.DEFAULT_ATTACK_FIRE_FRACTION,
                ExplosiveAbility::finishAndDestroy);
    }

    private static void finishAndDestroy(PlantInstance plant, PlantAbilityContext context) {
        if (plant != null && context != null) {
            context.destroyPlant(plant);
        }
    }

    // --- Instant explosives ---

    /**
     * One-shot explosives that detonate immediately on placement.
     * The plant is destroyed by the timed attack clip, not here.
     */
    private void resolveInstant(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;

        if (isGraveBuster(def)) {
            bustGrave(plant, context);
            return;
        }

        if (isHotPotato(def)) {
            meltIceUnder(plant, context);
            return;
        }

        // Ice-shroom: mapwide freeze, no damage.
        if (def.hasTag(PlantTags.ICE) && def.getDamage() == 0) {
            freezeAllZombies(context);
            return;
        }

        // Ice-shroom variant with damage (level 4 upgrade): freeze + damage.
        if (def.hasTag(PlantTags.ICE) && def.getDamage() > 0) {
            freezeAllZombies(context);
        }

        // Grapeshot: 3x3 explosion + bouncing grape projectiles.
        if (isGrapeshot(def)) {
            detonate(plant, context);
            spawnGrapes(plant, context);
            return;
        }

        detonate(plant, context);
        if (isDoomShroom(def)) {
            craterAt(plant, context);
        }
    }

    // --- Delayed explosives (traps) ---

    private PlantAction beginDelayed(PlantInstance plant, PlantAbilityContext context) {
        if (!armTrap(plant)) {
            return null;
        }
        if (!hasTrigger(plant, context)) {
            return null;
        }
        return explodeClipThen(plant, context, (p, ctx) -> handleDelayed(p, ctx, false));
    }

    /**
     * Arms a trap that has finished charging. {@code destroy} is false when
     * the timed attack clip will remove the plant after the smash/explode.
     */
    private void handleDelayed(PlantInstance plant, PlantAbilityContext context, boolean destroy) {
        if (plant.getPosition() == null) return;
        if (!armTrap(plant)) return;

        Plant def = plant.getDefinition();
        List<ZombieInstance> triggers = getTriggerZombies(plant, context);
        if (triggers.isEmpty()) return;

        if (def != null && def.hasTag(PlantTags.ICE)) {
            // Iceberg Lettuce: freeze the triggering zombies (no damage).
            for (ZombieInstance zombie : triggers) {
                freezeZombie(zombie);
            }
        } else if (isTangleKelp(def)) {
            pullZombiesUnderwater(plant, context, 1);
        } else if (isSquash(def)) {
            int damage = def.getDamage();
            damage += (int) cumulativeSpecialValue(plant, PlantSpecialTag.EXPLODE_DAMAGE_BUFF);
            for (ZombieInstance zombie : triggers) {
                applyExplosionDamage(context, zombie, damage, false);
            }
        } else {
            detonate(plant, context);
        }
        if (destroy) {
            context.destroyPlant(plant);
        }
    }

    /**
     * @return true once the trap is armed (including after this call arms it)
     */
    private boolean armTrap(PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def == null) return false;
        AbilityState state = plant.getAbilityState(def.getAbilityType());
        if (state == null) return false;
        if (!state.isArmed()) {
            state.setArmed(true);
            if (plant.getState() == PlantState.ARMING || plant.getState() == PlantState.IDLE) {
                plant.setState(PlantState.ARMED);
            }
        }
        return true;
    }

    private boolean hasTrigger(PlantInstance plant, PlantAbilityContext context) {
        return !getTriggerZombies(plant, context).isEmpty();
    }

    /**
     * Returns the zombies that trigger this trap. Most traps trigger on
     * same-tile contact. Squash triggers on an adjacent zombie in its lane.
     */
    private List<ZombieInstance> getTriggerZombies(PlantInstance plant,
                                                   PlantAbilityContext context) {
        if (plant.getPosition() == null) {
            return List.of();
        }
        Plant def = plant.getDefinition();
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();

        // Squash: crush the first adjacent zombie (reach 1; BONUS_SMASH_CHARGES adds).
        if (isSquash(def)) {
            int reach = 1 + (int) cumulativeSpecialValue(plant, PlantSpecialTag.BONUS_SMASH_CHARGES);
            return grounded(context.getZombiesInArea(row, col, 0, reach));
        }

        // Default: same-tile trigger. Flying zombies pass over without setting them off.
        return grounded(context.getZombiesInArea(row, col, 0, 0));
    }

    private static List<ZombieInstance> grounded(List<ZombieInstance> zombies) {
        List<ZombieInstance> grounded = new ArrayList<>();
        if (zombies == null) {
            return grounded;
        }
        for (ZombieInstance zombie : zombies) {
            if (zombie != null && !zombie.isDead() && !zombie.isFlying()) {
                grounded.add(zombie);
            }
        }
        return grounded;
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
        radius += (int) cumulativeSpecialValue(plant, PlantSpecialTag.TILE_RANGE_EXT);
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
        // EXPLODE_DAMAGE_BUFF adds to the explosion damage.
        damage += (int) cumulativeSpecialValue(plant, PlantSpecialTag.EXPLODE_DAMAGE_BUFF);

        // Mapwide explosion (Doom-shroom).
        if (radius >= MAPWIDE_THRESHOLD) {
            for (int lane = 0; lane < context.getRowCount(); lane++) {
                for (ZombieInstance zombie : context.getZombiesInLane(lane)) {
                    applyExplosionDamage(context, zombie, damage, isFire);
                }
            }
            // Heat melts every ice block on the map.
            context.damageIceInArea(
                    context.getRowCount() / 2, context.getColumnCount() / 2,
                    context.getRowCount(), context.getColumnCount(), Math.max(damage, IceTerrainStrategy.MAX_HP));
            return;
        }
        // Lane-clearing explosion (Jalapeno).
        if (isFire && radius >= LARGE_RADIUS) {
            for (ZombieInstance zombie : context.getZombiesInLane(row)) {
                applyExplosionDamage(context, zombie, damage, isFire);
            }
            // Jalapeno scorches the entire lane - melt all ice in it.
            context.damageIceInArea(
                    row, context.getColumnCount() / 2,
                    0, context.getColumnCount(), Math.max(damage, IceTerrainStrategy.MAX_HP));
            return;
        }
        // 3x3 AoE (Cherry Bomb, Grapeshot, Primal Potato Mine).
        if (radius >= LARGE_RADIUS) {
            for (ZombieInstance zombie : context.getZombiesInArea(row, col, 1, 1)) {
                applyExplosionDamage(context, zombie, damage, isFire);
            }
            context.damageIceInArea(row, col, 1, 1, Math.max(damage, 1));
            return;
        }
        // Localised explosion (Potato Mine): abilityValue 1 is the contact tile.
        int localRadius = radius <= 1 ? 0 : radius;
        for (ZombieInstance zombie : context.getZombiesInArea(row, col, localRadius, localRadius)) {
            applyExplosionDamage(context, zombie, damage, isFire);
        }
        context.damageIceInArea(row, col, localRadius, localRadius, Math.max(damage, 1));
    }

    /** Applies explosion damage to a single zombie (attributed via context). */
    private void applyExplosionDamage(PlantAbilityContext context, ZombieInstance zombie, int damage, boolean isFire) {
        if (zombie == null || zombie.isDead() || damage <= 0) return;
        if (isFire) {
            context.damageZombieWithFire(zombie, damage);
        } else {
            context.damageZombie(zombie, damage);
        }
    }

    private void craterAt(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        context.createCraterAt(plant.getPosition().getY(), plant.getPosition().getX());
    }

    private void bustGrave(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        context.removeGraveAt(plant.getPosition().getY(), plant.getPosition().getX());
        if (cumulativeSpecialValue(plant, PlantSpecialTag.EXPLODE_ON_FINISH) > 0f) {
            detonate(plant, context);
        }
    }

    private void meltIceUnder(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        int radius = cumulativeSpecialValue(plant, PlantSpecialTag.MELT_AREA_3X3) > 0f ? 1 : 0;
        context.damageIceInArea(row, col, radius, radius, IceTerrainStrategy.MAX_HP);
        if (cumulativeSpecialValue(plant, PlantSpecialTag.EXPLODE_ON_FINISH) > 0f) {
            detonate(plant, context);
        }
    }

    // --- Grapeshot bouncing grapes ---

    /**
     * Fires a volley of grape projectiles down the plant's lane and
     * each adjacent lane. Each grape deals {@value #GRAPE_DAMAGE}
     * damage, bounces off lane edges, and despawns after {@value #GRAPE_LIFETIME}s.
     */
    private void spawnGrapes(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        int row = plant.getPosition().getY();
        float originX = plant.getPosition().getX() + 0.5f;

        int grapesPerLane = GRAPES_PER_LANE
                + (int) cumulativeSpecialValue(plant, PlantSpecialTag.GRAPE_BOUNCE_EXT);

        for (int laneOffset = -1; laneOffset <= 1; laneOffset++) {
            int lane = row + laneOffset;
            if (lane < 0 || lane >= context.getRowCount()) continue;

            for (int i = 0; i < grapesPerLane; i++) {
                int direction = (i < grapesPerLane / 2) ? -1 : +1;
                Pellet grape = new Pellet(
                        GRAPE_DAMAGE,
                        new FloatPoint(originX + i * 0.3f, lane),
                        lane,
                        GRAPE_VELOCITY,
                        Projectile.Element.NONE,
                        direction
                );
                grape.setPierce(true);
                grape.setBouncing(true);
                grape.setLifetime(GRAPE_LIFETIME);
                grape.setYVelocity(laneOffset * 0.75f);
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
                armTrap(plant);
                spawnClones(plant, context, (int) def.getPlantFoodValue());
                break;
            case MAP_WIDE_FREEZE:
                freezeAllZombies(context);
                break;
            case LOCAL_AOE_ATTACK:
                if (isSquash(def)) {
                    smashRandomGrounded(plant, context, (int) def.getPlantFoodValue());
                } else {
                    detonateAt(plant, context, (int) def.getPlantFoodValue());
                }
                break;
            case PULL_UNDERWATER:
                pullRandomWaterZombies(plant, context, pullCount(plant));
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

                    AbilityState cloneState = clone.getAbilityState(def.getAbilityType());
                    if (cloneState != null) {
                        cloneState.setArmed(true);
                        cloneState.setCooldownRemaining(0);
                    }
                    clone.setState(PlantState.ARMED);

                    if (context.placePlant(clone, targetRow, targetCol)) {
                        spawned++;
                    }
                }
            }
        }
    }

    // --- Plant-food: smash two random grounded zombies (Squash) ---

    private void smashRandomGrounded(PlantInstance plant, PlantAbilityContext context, int count) {
        if (count <= 0) return;
        int damage = plant.getDefinition().getDamage();
        List<ZombieInstance> candidates = listGroundedZombies(context);
        int remaining = Math.min(count, candidates.size());
        while (remaining > 0 && !candidates.isEmpty()) {
            ZombieInstance target = candidates.remove(RNG.nextInt(candidates.size()));
            applyExplosionDamage(context, target, damage, false);
            remaining--;
        }
    }

    // --- Plant-food: PULL_UNDERWATER (Tangle Kelp) ---

    private int pullCount(PlantInstance plant) {
        int count = (int) plant.getDefinition().getPlantFoodValue();
        if (count <= 0) {
            count = 1;
        }
        count += (int) cumulativeSpecialValue(plant, PlantSpecialTag.BONUS_GRAB_TARGETS);
        return Math.max(1, count);
    }

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

        List<ZombieInstance> zombiesInLane = context.getZombiesInLane(row);
        for (ZombieInstance zombie : zombiesInLane) {
            if (grabbed >= maxTargets) break;
            if (zombie == null || zombie.isDead()) continue;
            int zombieCol = zombie.getGridPosition() != null ? zombie.getGridPosition().getX() : 0;
            if (Math.abs(zombieCol - col) > 4) continue;
            context.damageZombie(zombie, damage);
            grabbed++;
        }
    }

    /** Plant-food: pull several random zombies that are standing in water. */
    private void pullRandomWaterZombies(PlantInstance plant, PlantAbilityContext context, int maxTargets) {
        if (maxTargets <= 0) return;
        int damage = plant.getDefinition().getDamage();
        List<ZombieInstance> inWater = new ArrayList<>();
        for (int lane = 0; lane < context.getRowCount(); lane++) {
            for (ZombieInstance zombie : context.getZombiesInLane(lane)) {
                if (zombie == null || zombie.isDead() || zombie.isFlying()) continue;
                int zRow = zombie.getGridPosition() != null ? zombie.getGridPosition().getY() : lane;
                int zCol = zombie.getGridPosition() != null ? zombie.getGridPosition().getX() : 0;
                if (context.isWaterTile(zRow, zCol)) {
                    inWater.add(zombie);
                }
            }
        }
        int remaining = Math.min(maxTargets, inWater.size());
        while (remaining > 0 && !inWater.isEmpty()) {
            ZombieInstance target = inWater.remove(RNG.nextInt(inWater.size()));
            context.damageZombie(target, damage);
            remaining--;
        }
    }

    private List<ZombieInstance> listGroundedZombies(PlantAbilityContext context) {
        List<ZombieInstance> all = new ArrayList<>();
        for (int lane = 0; lane < context.getRowCount(); lane++) {
            all.addAll(grounded(context.getZombiesInLane(lane)));
        }
        return all;
    }

    // --- Plant-type detection helpers ---

    private static boolean named(Plant def, String name) {
        return def != null && def.getName() != null && def.getName().equalsIgnoreCase(name);
    }

    private boolean isGrapeshot(Plant def) {
        return named(def, "Grapeshot")
                || (def != null && def.getName() != null && def.getName().toLowerCase().contains("grape"));
    }

    private boolean isSquash(Plant def) {
        return named(def, "Squash")
                || (def != null && def.getName() != null && def.getName().toLowerCase().contains("squash"));
    }

    private boolean isDoomShroom(Plant def) {
        return named(def, "Doom-shroom");
    }

    private boolean isHotPotato(Plant def) {
        return named(def, "Hot Potato");
    }

    private boolean isGraveBuster(Plant def) {
        return named(def, "Grave Buster");
    }

    private boolean isTangleKelp(Plant def) {
        return named(def, "Tangle Kelp");
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
            if (upgrade.isSpecialMechanic()
                    && upgrade.getSpecialTag() == tag) {
                total += upgrade.getValue();
            }
        }
        return total;
    }
}
