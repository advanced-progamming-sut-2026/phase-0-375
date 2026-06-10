package model.game.systems;

import model.core.Tickable;
import model.event.EventBus;
import model.game.GameModel;
import model.zombie.instance.ZombieInstance;

public class ZombieSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

    public ZombieSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {

    }

    /**
     * Moves a zombie leftward based on its current speed and status modifiers.
     * Zombies in EATING state do not move.
     */
    private void moveZombie(ZombieInstance zombie, float deltaTime) {

    }

    /**
     * Called when a zombie enters a new grid cell.
     * Checks for plant collision, terrain effects, etc.
     */
    private void onZombieEnteredCell(ZombieInstance zombie) {

    }

    /**
     * Handles a zombie reaching the end of a lane.
     * Checks for lawn mower; if none, triggers game over.
     */
    private void handleZombieReachedEnd(ZombieInstance zombie) {

    }
}