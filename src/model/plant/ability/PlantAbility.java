package model.plant.ability;

import model.Ability;
import model.enums.PlantAbilityType;
import model.plant.instance.PlantInstance;

/**
 * Strategy interface for plant abilities
 */
public interface PlantAbility extends Ability {
    /**
     * Perform the ability's regular action during game flow.
     * Called by the plant instance on each relevant tick/event.
     *
     * @param plant the runtime plant instance providing context
     */
    void execute(PlantInstance plant);

    /**
     * Perform the ability's plant-food-enhanced action.
     * Called when the player activates plant food on this plant.
     *
     * @param plant the runtime plant instance providing context
     */
    void onPlantFood(PlantInstance plant);

    /**
     * @return the category type of this ability
     */
    PlantAbilityType getType();
}
