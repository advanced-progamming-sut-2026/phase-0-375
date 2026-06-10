package model.game.rule;

import model.game.GameModel;
import model.game.level.special.SaveOurSeedsLevel;

public class SaveOurSeedsEndGameCondition implements EndGameCondition {
    private SaveOurSeedsLevel saveOurSeedsLevel;

    public SaveOurSeedsEndGameCondition(SaveOurSeedsLevel saveOurSeedsLevel) {
        this.saveOurSeedsLevel = saveOurSeedsLevel;
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
