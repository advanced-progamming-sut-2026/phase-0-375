package model.game.rule;

import model.game.GameModel;

public interface EndGameCondition {
    boolean isWin(GameModel model);
    boolean isGameOver(GameModel model);
}
