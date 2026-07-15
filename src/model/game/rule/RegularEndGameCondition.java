package model.game.rule;

import model.game.level.RegularLevel;

/**
 * Regular levels use the default rules: win when all waves are cleared,
 * lose when a zombie reaches the house.
 */
public class RegularEndGameCondition extends AbstractEndGameCondition {
    private final RegularLevel regularLevel;

    public RegularEndGameCondition(RegularLevel regularLevel) {
        this.regularLevel = regularLevel;
    }
}
