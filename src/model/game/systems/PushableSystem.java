package model.game.systems;

import model.game.core.Tickable;
import model.event.EventBus;
import model.game.core.GameModel;

public class PushableSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

    public PushableSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {

    }
}