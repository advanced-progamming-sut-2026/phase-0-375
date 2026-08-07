package model.game.level.special;

import model.game.level.LevelConfig;
import model.game.level.RegularLevel;
import model.game.rule.LoveYourPlantsEndGameCondition;

/**
 * Love Your Plants: a regular level that is additionally lost when more
 * than {@code maxPlantDeaths} plants have died.
 *
 * <p>The loss rule itself lives in {@link LoveYourPlantsEndGameCondition},
 * fed by the plant-death bookkeeping in {@code GameModel}; this class wires
 * it in and validates that the level data actually defines a death budget.
 */
public class LoveYourPlantsLevel extends RegularLevel {

    public LoveYourPlantsLevel(LevelConfig config) {
        super(config);
        config.setEndGameCondition(new LoveYourPlantsEndGameCondition(this));
    }

    @Override
    public boolean canStart() {
        return super.canStart() && getConfig().getRules().getMaxPlantDeaths() >= 0;
    }
}
