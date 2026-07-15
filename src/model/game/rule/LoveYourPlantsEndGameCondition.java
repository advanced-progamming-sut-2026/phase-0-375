package model.game.rule;

import model.game.core.GameModel;
import model.game.level.special.LoveYourPlantsLevel;

/**
 * Love Your Plants levels are additionally lost when more than
 * {@code maxPlantDeaths} plants have died (-1 = unlimited).
 */
public class LoveYourPlantsEndGameCondition extends AbstractEndGameCondition {
    private final LoveYourPlantsLevel loveYourPlantsLevel;

    public LoveYourPlantsEndGameCondition(LoveYourPlantsLevel loveYourPlantsLevel) {
        this.loveYourPlantsLevel = loveYourPlantsLevel;
    }

    @Override
    public boolean isGameOver(GameModel model) {
        if (super.isGameOver(model)) return true;

        int maxPlantDeaths = loveYourPlantsLevel.getConfig().getRules().getMaxPlantDeaths();
        return maxPlantDeaths >= 0 && model.getPlantsLost() > maxPlantDeaths;
    }
}
