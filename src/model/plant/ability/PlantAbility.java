package model.plant.ability;

import model.Ability;
import model.enums.PlantCategory;
import model.plant.instance.PlantInstance;

/**
 * Strategy interface for plant abilities, keyed by {@link PlantCategory}.
 */
public interface PlantAbility extends Ability {

    /** @return the plant category this strategy handles. */
    PlantCategory getCategory();

    /**
     * Perform the ability's regular action. Called by {@code PlantSystem}
     * once the per-plant cooldown has expired.
     */
    void execute(PlantInstance plant, PlantAbilityContext context);

    /**
     * Perform the plant-food-enhanced action. Called once when the player
     * activates plant food on this plant.
     */
    void onPlantFood(PlantInstance plant, PlantAbilityContext context);

    /**
     * Optional per-shot cooldown override. Called by
     * {@code PlantInstance.executeAbility} after {@link #execute} returns.
     */
    default float getNextActionCooldown(PlantInstance plant) {
        return -1f;
    }
}