package model.game.systems;

import model.game.core.Tickable;
import model.event.EventBus;
import model.game.core.GameModel;
import model.game.map.Lane;


public class LawnMowerSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

    public LawnMowerSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {

    }

    /**
     * Kills all non-boss zombies in the lawn mower's row
     * as it sweeps across.
     */
    private void killZombiesInPath(Lane lane, float deltaTime) {

    }
}