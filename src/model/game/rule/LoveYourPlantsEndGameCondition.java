package model.game.rule;

import model.game.core.GameModel;
import model.game.level.special.LoveYourPlantsLevel;

public class LoveYourPlantsEndGameCondition implements EndGameCondition {
    private LoveYourPlantsLevel loveYourPlantsLevel;

    public LoveYourPlantsEndGameCondition(LoveYourPlantsLevel loveYourPlantsLevel) {
        this.loveYourPlantsLevel = loveYourPlantsLevel;
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
