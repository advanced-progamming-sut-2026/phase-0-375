package model.game.rule;

import model.game.core.GameModel;
import model.game.level.special.TimedWarLevel;

public class TimedWarEndGameCondition implements EndGameCondition {
    private TimedWarLevel timedWarLevel;

    public TimedWarEndGameCondition(TimedWarLevel timedWarLevel) {
        this.timedWarLevel = timedWarLevel;
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
