package model.plant.ability;

import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.PlantTags;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Strategy for the {@link PlantCategory#WALL_NUT} family.
 */
public class WallAbility implements PlantAbility {

    @Override
    public PlantCategory getCategory() { return PlantCategory.WALL_NUT; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        if (def.getDamage() > 0) {
            redirectOrReflect(plant, context);
        } else if (def.hasTag(PlantTags.MOVE_ZOMBIE)) {
            redirectOrReflect(plant, context);
        }
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        switch (def.getPlantFoodType()) {
            case GRANT_PERMANENT_ARMOR:
                int bonus = (int) def.getPlantFoodValue();
                plant.setCurrentHP(plant.getCurrentHP() + bonus);
                break;
            case KNOCKBACK_BLAST:
                // Garlic plant-food: shove every zombie in the lane
                // to an adjacent lane.
                knockbackBlast(plant, context);
                break;
            default:
                break;
        }
    }

    private void redirectOrReflect(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        int row = plant.getPosition().getY();
        int col = plant.getPosition().getX();
        Plant def = plant.getDefinition();

        // Damage reflect (Endurian)
        if (def.getDamage() > 0) {
            for (ZombieInstance zombie : context.getZombiesInArea(row, col, 0, 0)) {
                if (zombie.isEating() && zombie.getEatingTarget() == plant) {
                    context.damageZombie(zombie, def.getDamage());
                }
            }
        }

        // Lane redirect (Garlic, Sweet Potato)
        if (def.hasTag(PlantTags.MOVE_ZOMBIE)) {
            for (ZombieInstance zombie : context.getZombiesInArea(row, col, 0, 0)) {
                if (zombie.isEating() && zombie.getEatingTarget() == plant) {
                    int targetLane = pickAdjacentLane(row, context);
                    if (targetLane != row) {
                        context.moveZombieToLane(zombie, targetLane);
                    }
                }
            }
        }
    }

    private void knockbackBlast(PlantInstance plant, PlantAbilityContext context) {
        if (plant.getPosition() == null) return;
        int row = plant.getPosition().getY();

        List<ZombieInstance> zombiesInLane = context.getZombiesInLane(row);
        boolean toUpper = row > 0;
        boolean toLower = row < context.getRowCount() - 1;

        // If both adjacent lanes are available, alternate between them
        // to distribute the load. If only one is available, send all
        // zombies there.
        boolean sendUpNext = true;
        for (ZombieInstance zombie : zombiesInLane) {
            if (zombie == null || zombie.isDead()) continue;

            int targetLane;
            if (toUpper && toLower) {
                targetLane = sendUpNext ? row - 1 : row + 1;
                sendUpNext = !sendUpNext;
            } else if (toUpper) {
                targetLane = row - 1;
            } else if (toLower) {
                targetLane = row + 1;
            } else {
                // nowhere to move.
                continue;
            }

            context.moveZombieToLane(zombie, targetLane);
        }
    }

    /** Picks an adjacent lane for the regular redirect. */
    private int pickAdjacentLane(int row, PlantAbilityContext context) {
        boolean toUpper = row > 0;
        boolean toLower = row < context.getRowCount() - 1;
        if (toUpper && toLower) {
            return row % 2 == 0 ? row - 1 : row + 1;
        }
        if (toUpper) return row - 1;
        if (toLower) return row + 1;
        return row;
    }
}