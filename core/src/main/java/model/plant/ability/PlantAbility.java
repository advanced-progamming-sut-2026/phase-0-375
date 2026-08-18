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
     * Start the next ability cycle. Return a {@link PlantAction} to run across
     * subsequent ticks, or {@code null} if the cycle finished in this call.
     *
     * <p>Default: {@link #execute} once and return {@code null}.
     */
    default PlantAction beginAction(PlantInstance plant, PlantAbilityContext context) {
        execute(plant, context);
        return null;
    }

    /**
     * Perform the plant-food-enhanced action. Called once when the player
     * activates plant food on this plant.
     */
    void onPlantFood(PlantInstance plant, PlantAbilityContext context);

    /**
     * Optional per-shot cooldown override. Called by
     * {@code PlantInstance} after {@link #beginAction} returns.
     */
    default float getNextActionCooldown(PlantInstance plant) {
        return -1f;
    }

    /**
     * Optional per-tick bookkeeping (growth, digestion visuals, …).
     * Default: no-op.
     */
    default void tick(PlantInstance plant, float deltaTime) {
    }
}
