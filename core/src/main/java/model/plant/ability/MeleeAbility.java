package model.plant.ability;

import model.enums.*;
import model.game.map.terrain.IceTerrainStrategy;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.definition.PlantLevels;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Strategy for the {@link PlantCategory#MELEE} family.
 */
public class MeleeAbility implements PlantAbility {

    /** Maximum number of zombies that chomper can digest when it's on plant food. */
    private static final int DIGESTING_ZOMBIES = 3;

    /** Default digestion time (seconds) after the Chomper swallows a zombie. */
    private static final float CHOMPER_DIGEST_DURATION = 40.0f;
    /** Damage dealt to the swallowed zombie (intentionally huge to one-shot). */
    private static final int CHOMPER_SWALLOW_DAMAGE = 6767;

    /** Kiwibeast elapsed time to reach stage 2. */
    private static final float KIWIBEAST_STAGE2_SECONDS = 24f;
    /** Kiwibeast elapsed time to reach stage 3. */
    private static final float KIWIBEAST_STAGE3_SECONDS = 72f;

    private static final int AREA_3X3_VALUE = 9;
    private static final int AREA_5X5_VALUE = 25;

    private final List<ZombieInstance> targets = new ArrayList<>();
    private boolean consumedAction;

    @Override
    public PlantCategory getCategory() { return PlantCategory.MELEE; }

    @Override
    public void tick(PlantInstance plant, float deltaTime) {
        if (plant == null || deltaTime <= 0f) return;
        Plant def = plant.getDefinition();
        if (def == null || !named(def, "Kiwibeast")) return;
        AbilityState state = plant.getAbilityState(def.getAbilityType());
        if (state == null) return;
        state.setChargeProgress(state.getChargeProgress() + deltaTime);
        state.setGrowthStage(kiwibeastStageFromElapsed(state.getChargeProgress()));
    }

    @Override
    public PlantAction beginAction(PlantInstance plant, PlantAbilityContext context) {
        consumedAction = false;
        targets.clear();
        if (plant.getPosition() == null) return null;
        Plant def = plant.getDefinition();
        if (def == null) return null;

        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            consumedAction = true;
            context.triggerFamilyPlantFood(PlantCategory.MELEE);
            return null;
        }

        switch (def.getAbilityType()) {
            case MELEE_ATTACK:
                return beginMeleeAttack(plant, context);
            case DELAYED_EXPLOSIVE:
                return beginChomper(plant, context);
            default:
                return null;
        }
    }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        Plant def = plant.getDefinition();
        if (def == null) return;

        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.MELEE);
            return;
        }

        if (def.getAbilityType() == PlantAbilityType.DELAYED_EXPLOSIVE) {
            swallowStoredTargets(plant, context);
            return;
        }

        if (def.getAbilityType() != PlantAbilityType.MELEE_ATTACK) return;
        strikeTargets(plant, context, collectHitTargets(plant, context), false);
    }

    @Override
    public float getNextActionCooldown(PlantInstance plant) {
        return consumedAction ? -1f : 0f;
    }

    // --- animation windows ---

    private TimedPlantAction beginMeleeAttack(PlantInstance plant, PlantAbilityContext context) {
        List<ZombieInstance> hits = collectHitTargets(plant, context);
        if (hits.isEmpty()) return null;
        targets.clear();
        targets.addAll(hits);
        consumedAction = true;
        return TimedPlantAction.attackThen(plant, context, this::execute);
    }

    private TimedPlantAction beginChomper(PlantInstance plant, PlantAbilityContext context) {
        AbilityState state = plant.getAbilityState(PlantAbilityType.DELAYED_EXPLOSIVE);
        if (state == null || state.isDigesting()) return null;

        ZombieInstance target = firstSwallowCandidate(plant, context);
        if (target == null) return null;

        targets.clear();
        targets.add(target);
        consumedAction = true;
        return TimedPlantAction.attackThen(plant, context, this::execute);
    }

    // --- Melee attack ---

    private void strikeTargets(PlantInstance plant, PlantAbilityContext context,
                               List<ZombieInstance> hits, boolean plantFood) {
        targets.clear();
        Plant def = plant.getDefinition();
        if (def == null || hits == null || hits.isEmpty()) return;

        boolean fire = def.hasTag(PlantTags.FIRE);
        int damage = computePlantDamage(plant);
        if (plantFood) {
            damage *= plantFoodDamageMultiplier(def);
        }
        if (damage <= 0) return;

        for (ZombieInstance zombie : hits) {
            if (zombie == null || zombie.isDead()) continue;
            if (fire) {
                context.damageZombieWithFire(zombie, damage);
            } else {
                context.damageZombie(zombie, damage);
            }
            targets.add(zombie);
        }

        if (fire && plant.getPosition() != null) {
            int radius = plantFood ? plantFoodRadius(plant) : attackRadius(plant);
            context.damageIceInArea(
                    plant.getPosition().getY(), plant.getPosition().getX(),
                    radius, radius, IceTerrainStrategy.MAX_HP
            );
        }
    }

    // --- Chomper swallow + digest cycle ---

    private ZombieInstance firstSwallowCandidate(PlantInstance plant, PlantAbilityContext context) {
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        int reach = Math.max(1, (int) plant.getDefinition().getAbilityValue());
        List<ZombieInstance> candidates = context.getZombiesInArea(row, col, 0, reach);
        for (ZombieInstance zombie : candidates) {
            if (zombie != null && !zombie.isDead()) {
                return zombie;
            }
        }
        return null;
    }

    private void swallowStoredTargets(PlantInstance plant, PlantAbilityContext context) {
        AbilityState state = plant.getAbilityState(PlantAbilityType.DELAYED_EXPLOSIVE);
        if (state == null) return;

        List<ZombieInstance> toSwallow = new ArrayList<>();
        for (ZombieInstance zombie : targets) {
            if (zombie != null && !zombie.isDead()) {
                toSwallow.add(zombie);
            }
        }
        if (toSwallow.isEmpty()) {
            ZombieInstance fallback = firstSwallowCandidate(plant, context);
            if (fallback != null) {
                toSwallow.add(fallback);
            }
        }
        if (toSwallow.isEmpty()) return;

        for (ZombieInstance target : toSwallow) {
            context.damageZombie(target, CHOMPER_SWALLOW_DAMAGE);
        }

        float digestDuration = Math.max(5f, CHOMPER_DIGEST_DURATION - cumulativeDigestReduction(plant));
        state.setDigesting(true);
        state.setDigestRemaining(digestDuration);
        state.setCooldownRemaining(digestDuration);
    }

    /**
     * Sums up every {@link PlantSpecialTag#DIGEST_REDUCTION} upgrade
     * value the plant has accumulated via its level upgrades.
     */
    private float cumulativeDigestReduction(PlantInstance plant) {
        return cumulativeSpecialValue(plant, PlantSpecialTag.DIGEST_REDUCTION);
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null || plant.getPosition() == null) return;

        if (def.getPlantFoodType() == PlantFoodType.LOCAL_AOE_ATTACK) {
            int radius = plantFoodRadius(plant);
            int row = plant.getPosition().getY();
            int col = plant.getPosition().getX();
            strikeTargets(plant, context, context.getZombiesInArea(row, col, radius, radius), true);
        } else if (def.getPlantFoodType() == PlantFoodType.PULL_UNDERWATER) {
            AbilityState state = plant.getAbilityState(PlantAbilityType.DELAYED_EXPLOSIVE);
            if (state == null) return;

            int row = plant.getPosition().getY();
            int col = plant.getPosition().getX();
            List<ZombieInstance> candidates = new ArrayList<>(context.getZombiesInLane(row));
            candidates.removeIf(z -> z == null || z.isDead() || z.getGridPosition() == null);
            candidates.sort(Comparator.comparingInt(z -> Math.abs(z.getGridX() - col)));
            targets.clear();
            for (ZombieInstance zombie : candidates) {
                if (targets.size() >= DIGESTING_ZOMBIES) break;
                targets.add(zombie);
            }
            swallowStoredTargets(plant, context);
        }
    }

    /** Computes the damage that the given {@code plant} applies to zombies in this tick. */
    public int computePlantDamage(PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def == null) return 0;

        int base = def.getDamage();
        if (!def.hasTag(PlantTags.WARM_UP)) {
            return base;
        }

        AbilityState state = plant.getAbilityState(PlantAbilityType.MELEE_ATTACK);
        if (state == null) return base;
        int stage = Math.min(2, Math.max(0, state.getGrowthStage()));
        return base * (1 + stage);
    }

    /**
     * radius of this plant's current melee hit (0 = own tile).
     * Used by the view for pulse / tile-hit placement.
     */
    public int attackRadius(PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def == null) return 0;
        if (named(def, "Kiwibeast")) {
            AbilityState state = plant.getAbilityState(def.getAbilityType());
            int stage = state == null ? 0 : Math.max(0, state.getGrowthStage());
            return 1 + Math.min(1, stage);
        }
        return radiusFromAbilityValue(def.getAbilityValue());
    }

    /** radius of the plant-food smash / pulse. */
    public int plantFoodRadius(PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def == null) return 1;
        if (named(def, "Phat Beet")) {
            return 2;
        }
        return radiusFromFoodValue(def.getPlantFoodValue());
    }

    // --- Helpers ---

    private List<ZombieInstance> collectHitTargets(PlantInstance plant, PlantAbilityContext context) {
        if (plant == null || context == null || plant.getPosition() == null) {
            return Collections.emptyList();
        }
        Plant def = plant.getDefinition();
        if (def == null) return Collections.emptyList();

        int col = plant.getPosition().getX();
        int row = plant.getPosition().getY();

        if (def.hasTag(PlantTags.AOE) || named(def, "Phat Beet") || named(def, "Kiwibeast")) {
            int radius = attackRadius(plant);
            return liveCopy(context.getZombiesInArea(row, col, radius, radius));
        }

        int reach = Math.max(1, (int) def.getAbilityValue());
        reach += (int) cumulativeSpecialValue(plant, PlantSpecialTag.TILE_RANGE_EXT);
        return liveCopy(context.getZombiesInArea(row, col, 0, reach));
    }

    private static List<ZombieInstance> liveCopy(List<ZombieInstance> source) {
        List<ZombieInstance> live = new ArrayList<>();
        if (source == null) return live;
        for (ZombieInstance zombie : source) {
            if (zombie != null && !zombie.isDead()) {
                live.add(zombie);
            }
        }
        return live;
    }

    private static int radiusFromAbilityValue(float value) {
        int encoded = (int) value;
        if (encoded >= AREA_5X5_VALUE) return 2;
        if (encoded >= AREA_3X3_VALUE) return 1;
        return Math.max(0, encoded);
    }

    private static int radiusFromFoodValue(float value) {
        int encoded = (int) value;
        if (encoded >= AREA_5X5_VALUE) return 2;
        if (encoded >= AREA_3X3_VALUE) return 1;
        if (encoded <= 0) return 1;
        return encoded;
    }

    private static int plantFoodDamageMultiplier(Plant def) {
        if (named(def, "Phat Beet")) return 5;
        if (named(def, "Kiwibeast")) return 4;
        return 3;
    }

    private static int kiwibeastStageFromElapsed(float elapsed) {
        if (elapsed >= KIWIBEAST_STAGE3_SECONDS) {
            return 2;
        }
        if (elapsed >= KIWIBEAST_STAGE2_SECONDS) {
            return 1;
        }
        return 0;
    }

    private float cumulativeSpecialValue(PlantInstance plant, PlantSpecialTag tag) {
        Plant def = plant.getDefinition();
        if (def == null || def.getLevels() == null || tag == null) return 0f;
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

    private static boolean named(Plant def, String name) {
        return def != null && def.getName() != null && def.getName().equalsIgnoreCase(name);
    }

    public List<ZombieInstance> getTargets() {
        return Collections.unmodifiableList(targets);
    }
}
