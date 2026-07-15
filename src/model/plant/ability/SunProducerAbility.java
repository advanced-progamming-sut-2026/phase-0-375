package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.PlantTags;
import model.enums.SunType;
import model.game.map.FloatPoint;
import model.item.Sun;
import model.plant.definition.Plant;
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
            float dx = (RNG.nextFloat() - 0.5f) * 0.8f;
            float dy = (RNG.nextFloat() - 0.5f) * 0.8f;
            Sun sun = new Sun(
                    SunType.NORMAL,
                    computeSunAmount(plant),
                    plant.getPosition().getX() + (int) dx,
                    plant.getPosition().getY() + (int) dy
            );
            context.spawnSun(sun);
        }
    }

    // --- Helpers ---

    /** Computes the actual sun amount this plant produces this tick. */
    private int computeSunAmount(PlantInstance plant) {
        Plant def = plant.getDefinition();
        int base = (int) def.getAbilityValue();
        if (def.hasTag(PlantTags.WARM_UP)) {
            AbilityState state = plant.getAbilityState(PlantAbilityType.PRODUCE_SUN);
            if (state != null) {
                int stage = Math.min(2, state.getGrowthStage());
                return base * (1 + stage);
            }
        }
        return base;
    }

    private void dropSun(PlantInstance plant, PlantAbilityContext context, int amount) {
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        Sun sun = new Sun(SunType.NORMAL, amount, col, row);
        context.spawnSun(sun);
    }
}