package model.plant.ability;

import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Strategy for the {@link PlantCategory#MELEE} family.
 */
public class MeleeAbility implements PlantAbility {

    @Override
    public PlantCategory getCategory() { return PlantCategory.MELEE; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        Plant def = plant.getDefinition();
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();

        // AoE melee: hit everything in a small area
        int radius = (int) def.getAbilityValue();
        if (radius >= 9) {
            // Big swipe - 3x3 around the plant
            List<ZombieInstance> targets = context.getZombiesInArea(row, col, 1, 1);
            for (ZombieInstance zombie : targets) {
                context.damageZombie(zombie, def.getDamage());
            }
            return;
        }

        // Single-target melee - hit the first zombie in any of the 8 neighbors
        for (int rowDist = -1; rowDist <= 1; rowDist++) {
            for (int colDist = -1; colDist <= 1; colDist++) {
                if (rowDist == 0 && colDist == 0) continue;
                List<ZombieInstance> targets = context.getZombiesInArea(row + rowDist, col + colDist, 0, 0);
                if (!targets.isEmpty()) {
                    context.damageZombie(targets.getFirst(), def.getDamage());
                    return;
                }
            }
        }
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def.getPlantFoodType() != PlantFoodType.LOCAL_AOE_ATTACK) return;
        if (plant.getPosition() == null) return;

        int radius = (int) def.getPlantFoodValue();
        if (radius <= 0) radius = 1;
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        for (ZombieInstance zombie : context.getZombiesInArea(row, col, radius, radius)) {
            context.damageZombie(zombie, def.getDamage() * 3);
        }
    }
}