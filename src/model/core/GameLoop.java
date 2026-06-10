package model.core;

import model.enums.GameState;

public interface GameLoop {
    void update(float deltaTime);

    GameState getGameState();

    void pause();

    void resume();
}
