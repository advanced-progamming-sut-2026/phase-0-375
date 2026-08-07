package model.game.rule;

import model.game.core.GameModel;

public interface EndGameCondition {
    boolean isWin(GameModel model);
    boolean isGameOver(GameModel model);
}
