package model.game.systems;

import model.game.core.Tickable;
import model.event.EventBus;
import model.game.core.GameModel;

public class CombatSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

    public CombatSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {

    }

    /**
     * Applies eating damage from zombies to the plants they are eating.
     */
    private void handleZombieEating(float deltaTime) {

    }

    /**
     * Applies damage over time from status effects (poison, burning).
     */
    private void handleStatusEffectDamage(float deltaTime) {
    }
}