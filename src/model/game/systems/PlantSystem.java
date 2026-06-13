package model.game.systems;

import model.game.core.Tickable;
import model.event.EventBus;
import model.game.core.GameModel;
import model.plant.instance.PlantInstance;
import model.enums.PlantAbilityType;

public class PlantSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

    public PlantSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {

    }

    /**
     * Reduces cooldown timers for all abilities on a plant.
     */
    private void tickAbilityCooldowns(PlantInstance plant, float deltaTime) {

    }

    /**
     * Handles plant food effect ticking.
     */
    private void tickPlantFood(PlantInstance plant, float deltaTime) {

    }

    /**
     * Executes abilities whose cooldowns have expired.
     */
    private void executeAbilities(PlantInstance plant, float deltaTime) {

    }

    /**
     * Checks if an ability can execute based on game conditions.
     * For example, shooters need a zombie in their lane.
     */
    private boolean canExecuteAbility(PlantInstance plant, PlantAbilityType type) {
        return false;
    }

    /**
     * Checks if there is at least one zombie in the given lane.
     */
    private boolean hasZombieInLane(int row) {
        return false;
    }

    /**
     * Checks if there is a zombie adjacent to the plant for melee attacks.
     */
    private boolean hasAdjacentZombie(PlantInstance plant) {
        return false;
    }
}