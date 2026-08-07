package model.game.level.special;

import model.game.level.LevelConfig;
import model.game.level.RegularLevel;
import model.game.rule.TimedWarEndGameCondition;

/**
 * Timed War: kill the target number of zombies before the time limit runs
 * out. Losing happens when time expires first (or a zombie reaches the
 * house, as always).
 *
 * <p>The win/loss rule itself lives in {@link TimedWarEndGameCondition},
 * driven by {@code timedWarTargetKills} and {@code timedWarLimit} in the
 * level rules; this class wires it in and validates those two fields.
 */
public class TimedWarLevel extends RegularLevel {

    public TimedWarLevel(LevelConfig config) {
        super(config);
        config.setEndGameCondition(new TimedWarEndGameCondition(this));
    }

    @Override
    public boolean canStart() {
        return super.canStart()
                && getConfig().getRules().getTimedWarTargetKills() > 0
                && getConfig().getRules().getTimedWarLimit() > 0;
    }
}
