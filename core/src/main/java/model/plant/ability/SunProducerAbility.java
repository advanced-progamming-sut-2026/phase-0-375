package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.PlantSpecialTag;
import model.enums.PlantState;
import model.enums.PlantTags;
import model.enums.SunType;
import model.item.Sun;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.definition.PlantLevels;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;

import java.util.Random;

/**
 * Strategy for the {@link PlantCategory#SUN_PRODUCER} family.
 */
public class SunProducerAbility implements PlantAbility {

    private static final Random RNG = new Random();

    /** Sun-shroom elapsed time to reach stage 2. */
    private static final float SUNSHROOM_STAGE2_SECONDS = 24f;
    /** Sun-shroom elapsed time to reach stage 3. */
    private static final float SUNSHROOM_STAGE3_SECONDS = 72f;
    /** Sun-shroom max growth stage index (0-based). */
    private static final int SUNSHROOM_MAX_STAGE = 2;

    @Override
    public PlantCategory getCategory() { return PlantCategory.SUN_PRODUCER; }

    @Override
    public void tick(PlantInstance plant, float deltaTime) {
        if (plant == null || deltaTime <= 0f) return;
        Plant def = plant.getDefinition();
        if (def == null || !def.hasTag(PlantTags.WARM_UP)) return;

        AbilityState state = plant.getAbilityState(PlantAbilityType.PRODUCE_SUN);
        if (state == null) return;

        state.setChargeProgress(state.getChargeProgress() + deltaTime);
        float stage2At = sunshroomStage2Threshold(plant);
        float stage3At = sunshroomStage3Threshold(plant);
        state.setGrowthStage(sunshroomStageFromElapsed(state.getChargeProgress(), stage2At, stage3At));
    }

    @Override
    public PlantAction beginAction(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return null;

        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            execute(plant, context);
            return null;
        }

        if (def.getAbilityType() == PlantAbilityType.INSTANT_SUN_BURST) {
            return beginGoldBloomBurst(plant, context);
        }

        if (def.getAbilityType() == PlantAbilityType.PRODUCE_SUN) {
            return TimedPlantAction.produceAt(plant, context, this::execute);
        }

        return null;
    }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;

        // Enlighten-mint: trigger plant-food on every SUN_PRODUCER plant.
        if (def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST) {
            context.triggerFamilyPlantFood(PlantCategory.SUN_PRODUCER);
            return;
        }

        if (def.getAbilityType() == PlantAbilityType.INSTANT_SUN_BURST) {
            dropGoldBloomBurst(plant, context);
            return;
        }

        if (def.getAbilityType() != PlantAbilityType.PRODUCE_SUN) return;
        if (plant.getPosition() == null) return;

        int amount = computeSunAmount(plant);
        dropSun(plant, context, amount);
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def.getPlantFoodType() == PlantFoodType.NONE) return;
        if (def.getPlantFoodType() != PlantFoodType.SPAWN_SUN_ITEMS) return;
        if (plant.getPosition() == null) return;

        if (def.hasTag(PlantTags.WARM_UP)) {
            AbilityState state = plant.getAbilityState(PlantAbilityType.PRODUCE_SUN);
            if (state != null) {
                state.setGrowthStage(SUNSHROOM_MAX_STAGE);
                state.setChargeProgress(sunshroomStage3Threshold(plant));
            }
        }

        dropScatteredSuns(plant, context, (int) def.getPlantFoodValue());
    }

    // --- Helpers ---

    /** Computes the actual sun amount this plant produces this tick. */
    private int computeSunAmount(PlantInstance plant) {
        Plant def = plant.getDefinition();
        int base = (int) def.getAbilityValue();

        if (def.hasTag(PlantTags.WARM_UP)) {
            AbilityState state = plant.getAbilityState(PlantAbilityType.PRODUCE_SUN);
            if (state != null) {
                int stage = Math.min(SUNSHROOM_MAX_STAGE, state.getGrowthStage());
                return base * (1 + stage);
            }
        }

        // DOUBLE_SUN_CHANCE: a chance to drop double sun.
        if (hasSpecialTag(plant, PlantSpecialTag.DOUBLE_SUN_CHANCE)
                && RNG.nextFloat() < 0.25f) {
            return base * 2;
        }
        return base;
    }

    /** @return true if the plant has any upgrade with the given special tag. */
    private boolean hasSpecialTag(PlantInstance plant, PlantSpecialTag tag) {
        Plant def = plant.getDefinition();
        if (def == null || def.getLevels() == null) return false;
        for (int lvl = 2; lvl <= 4; lvl++) {
            if (lvl > plant.getLevel()) break;
            LevelUpgrade upgrade = def.getLevels().getUpgrade(lvl);
            if (upgrade == null) continue;
            if (upgrade.isSpecialMechanic() && upgrade.getSpecialTag() == tag) {
                return true;
            }
        }
        return false;
    }

    /** Sums up every upgrade value with the given special tag. */
    private float cumulativeSpecialValue(PlantInstance plant, PlantSpecialTag tag) {
        Plant def = plant.getDefinition();
        if (def == null || def.getLevels() == null) return 0f;
        float total = 0f;
        for (int lvl = 2; lvl <= 4; lvl++) {
            if (lvl > plant.getLevel()) break;
            LevelUpgrade upgrade = def.getLevels().getUpgrade(lvl);
            if (upgrade == null) continue;
            if (upgrade.isSpecialMechanic() && upgrade.getSpecialTag() == tag) {
                total += upgrade.getValue();
            }
        }
        return total;
    }

    private float growTimeReduction(PlantInstance plant) {
        float reduction = cumulativeSpecialValue(plant, PlantSpecialTag.GROW_TIME_REDUCTION);
        return reduction < 0f ? -reduction : reduction;
    }

    private float sunshroomStage2Threshold(PlantInstance plant) {
        return Math.max(0f, SUNSHROOM_STAGE2_SECONDS - growTimeReduction(plant));
    }

    private float sunshroomStage3Threshold(PlantInstance plant) {
        return Math.max(0f, SUNSHROOM_STAGE3_SECONDS - growTimeReduction(plant));
    }

    private PlantAction beginGoldBloomBurst(PlantInstance plant, PlantAbilityContext context) {
        float duration = TimedPlantAction.presentationDurationFor(
                plant, context, PlantState.PRODUCING, TimedPlantAction.DEFAULT_PRODUCING_DURATION);
        return new TimedPlantAction(
                PlantState.PRODUCING,
                duration,
                null,
                (p, ctx) -> dropGoldBloomBurst(p, ctx),
                TimedPlantAction.DEFAULT_PRODUCING_FRACTION,
                (p, ctx) -> ctx.destroyPlant(p));
    }

    private void dropGoldBloomBurst(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def == null) return;
        int burst = (int) def.getAbilityValue()
                + (int) cumulativeSpecialValue(plant, PlantSpecialTag.SUN_AMOUNT_BUFF);
        dropMultipleSuns(plant, context, burst);
    }

    private static int sunshroomStageFromElapsed(float elapsed, float stage2At, float stage3At) {
        if (elapsed >= stage3At) {
            return 2;
        }
        if (elapsed >= stage2At) {
            return 1;
        }
        return 0;
    }

    private SunType sunTypeFor(PlantInstance plant) {
        Plant def = plant.getDefinition();
        if (def != null && "Primal Sunflower".equals(def.getName())) {
            return SunType.SPECIAL;
        }
        return SunType.NORMAL;
    }

    private void dropSun(PlantInstance plant, PlantAbilityContext context, int amount) {
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        Sun sun = new Sun(sunTypeFor(plant), amount, col, row);
        context.spawnSun(sun);
    }

    private void dropMultipleSuns(PlantInstance plant, PlantAbilityContext context, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int chunk = Math.min(50, remaining);
            dropSun(plant, context, chunk);
            remaining -= chunk;
        }
    }

    /** Drops sun tokens totalling {@code total}, scattered near the plant. */
    private void dropScatteredSuns(PlantInstance plant, PlantAbilityContext context, int total) {
        if (total <= 0) return;

        int remaining = total;
        while (remaining > 0) {
            int chunk = Math.min(50, remaining);
            float dx = (RNG.nextFloat() - 0.5f) * 2.0f;
            float dy = (RNG.nextFloat() - 0.5f) * 2.0f;
            float x = Math.max(0f, Math.min(context.getColumnCount() - 1,
                    plant.getPosition().getX() + dx));
            float y = Math.max(0f, Math.min(context.getRowCount() - 1,
                    plant.getPosition().getY() + dy));
            Sun sun = new Sun(
                    sunTypeFor(plant),
                    chunk,
                    Math.round(x),
                    Math.round(y)
            );
            context.spawnSun(sun);
            remaining -= chunk;
        }
    }
}
