package model.plant.ability;

import model.plant.instance.PlantInstance;

/**
 * Multi-tick ability sequence. {@link PlantInstance} holds at most one active
 * action and advances it each tick — abilities own timing/phases here instead of
 * adding per-animation timers on the plant.
 */
public interface PlantAction {

    /** Called once when the action becomes the plant's active action. */
    void start(PlantInstance plant, PlantAbilityContext context);

    /**
     * Advance the action by {@code deltaTime} seconds.
     *
     * @return {@code true} when the action is finished and should be cleared
     */
    boolean tick(PlantInstance plant, PlantAbilityContext context, float deltaTime);

    /** Called when the plant interrupts this action (freeze, plant-food, death, …). */
    default void cancel(PlantInstance plant) {}
}
