package model.game.rule;

import model.game.core.GameModel;
import model.game.level.RegularLevel;

public class RegularEndGameCondition implements EndGameCondition {
    private RegularLevel regularLevel;

    public RegularEndGameCondition(RegularLevel regularLevel) {
        this.regularLevel = regularLevel;
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
