package model.game.rule;

import model.game.core.GameModel;

/**
 * End-game condition that never wins or loses — used by the debug sandbox.
 */
public final class NeverEndCondition implements EndGameCondition {
    @Override
    public boolean isWin(GameModel model) {
        return false;
    }

    @Override
    public boolean isGameOver(GameModel model) {
        return false;
    }
}
