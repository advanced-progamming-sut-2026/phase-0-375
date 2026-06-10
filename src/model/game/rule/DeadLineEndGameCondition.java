package model.game.rule;

import model.game.GameModel;
import model.game.level.special.DeadLineLevel;

public class DeadLineEndGameCondition implements EndGameCondition {
    private DeadLineLevel deadLineLevel;

    public DeadLineEndGameCondition(DeadLineLevel deadLineLevel) {
        this.deadLineLevel = deadLineLevel;
    }

    @Override
    public boolean isWin(GameModel model) {
        return false;
    }

    @Override
    public boolean isGameOver(GameModel model) {
        return false;
    }
}
