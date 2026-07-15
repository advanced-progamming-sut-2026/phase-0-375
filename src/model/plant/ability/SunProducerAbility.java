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
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;

import java.util.Random;

/**
 * Strategy for the {@link PlantCategory#SUN_PRODUCER} family.
 */
public class SunProducerAbility implements PlantAbility {

    private static final Random RNG = new Random();

    @Override
    public PlantCategory getCategory() { return PlantCategory.SUN_PRODUCER; }

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
            context.addSun((int) def.getAbilityValue());
            context.destroyPlant(plant);
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

        int count = (int) def.getPlantFoodValue() / Math.max(1, computeSunAmount(plant));
        if (count <= 0) count = 1;
        for (int i = 0; i < count; i++) {
            float dx = (RNG.nextFloat() - 0.5f) * 2.0f;  // -1.0 .. +1.0
            float dy = (RNG.nextFloat() - 0.5f) * 2.0f;  // -1.0 .. +1.0
            int col = Math.round(plant.getPosition().getX() + dx);
            int row = Math.round(plant.getPosition().getY() + dy);
            Sun sun = new Sun(
                    SunType.NORMAL,
                    computeSunAmount(plant),
                    col,
                    row
            );
            context.spawnSun(sun);
        }
    }

    // --- Helpers ---

    /** Computes the actual sun amount this plant produces this tick. */
    private int computeSunAmount(PlantInstance plant) {
        Plant def = plant.getDefinition();
        int base = (int) def.getAbilityValue();

        // Gold Bloom (INSTANT_SUN_BURST) reads the SUN_AMOUNT_BUFF
        // upgrade to increase the burst amount.
        if (def.getAbilityType() == PlantAbilityType.INSTANT_SUN_BURST) {
            base += (int) cumulativeSpecialValue(plant, PlantSpecialTag.SUN_AMOUNT_BUFF);
        }

        if (def.hasTag(PlantTags.WARM_UP)) {
            AbilityState state = plant.getAbilityState(PlantAbilityType.PRODUCE_SUN);
            if (state != null) {
                int stage = Math.min(2, state.getGrowthStage());
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

    private void dropSun(PlantInstance plant, PlantAbilityContext context, int amount) {
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        Sun sun = new Sun(SunType.NORMAL, amount, col, row);
        context.spawnSun(sun);
    }
}