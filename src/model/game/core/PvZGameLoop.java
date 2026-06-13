package model.core;

import model.enums.GameState;
import model.game.GameModel;

public class PvZGameLoop implements GameLoop {

    private final GameModel gameModel;
    private GameState gameState;

    public PvZGameLoop(GameModel gameModel) {
        this.gameModel = gameModel;
        this.gameState = GameState.RUNNING;
    }

    @Override
    public void update(float deltaTime) {

    }

    @Override
    public GameState getGameState() {
        return gameState;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    public void setGameState(GameState state) {
        this.gameState = state;
    }
}
