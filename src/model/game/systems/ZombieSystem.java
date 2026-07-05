package model.game.systems;

import model.game.core.Tickable;
import model.event.EventBus;
import model.game.core.GameModel;
import model.zombie.instance.ZombieInstance;

import java.util.List;

public class ZombieSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

    public ZombieSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {
        List<ZombieInstance> zombies = gameModel.getZombies();

        for (ZombieInstance zombie : zombies) {
            zombie.tick(deltaTime);
            zombie.tickBehaviors(deltaTime, gameModel);

            moveZombie(zombie, deltaTime);
        }
    }

    /**
     * Moves a zombie based on its current speed, status modifiers, and direction.
     * Zombies in EATING or SPECIAL_ACTION state do not move.
     */
    private void moveZombie(ZombieInstance zombie, float deltaTime) {
        if (zombie.getState() == model.enums.ZombieState.EATING ||
                zombie.getState() == model.enums.ZombieState.SPECIAL_ACTION) {
            return;
        }

        float effectiveSpeed = zombie.getCurrentSpeed();

        float deltaX = effectiveSpeed * deltaTime;
        if (zombie.isMovingBackward()) {
            deltaX = -deltaX;
        }

        zombie.setContinuousX(zombie.getContinuousX() - deltaX);

        int newGridX = (int) Math.floor(zombie.getContinuousX());
        if (newGridX != zombie.getGridX()) {
            zombie.setGridX(newGridX);
            onZombieEnteredCell(zombie);
        }
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