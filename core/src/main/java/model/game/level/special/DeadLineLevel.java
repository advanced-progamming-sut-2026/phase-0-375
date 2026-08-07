package model.game.level.special;

import model.game.level.LevelConfig;
import model.game.level.RegularLevel;
import model.game.rule.DeadLineEndGameCondition;

/**
 * Dead Line: a regular level with a red line on the lawn. The level is lost
 * the moment any zombie crosses the line, even if the house is still safe.
 *
 * <p>The loss rule itself lives in {@link DeadLineEndGameCondition}; this
 * class wires it in and validates that the level data actually defines a
 * line within the grid.
 */
public class DeadLineLevel extends RegularLevel {

    public DeadLineLevel(LevelConfig config) {
        super(config);
        config.setEndGameCondition(new DeadLineEndGameCondition(this));
    }

    @Override
    public boolean canStart() {
        int line = getConfig().getDeadLineColumn();
        return super.canStart() && line >= 0 && line < getConfig().getColumns();
    }
}
