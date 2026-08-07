package model.game.rule;

import model.game.core.GameModel;
import model.game.level.special.TimedWarLevel;

/**
 * Timed War levels are won by killing the target number of zombies
 * ({@code timedWarTargetKills}) before the time limit
 * ({@code timedWarLimit}, in seconds) runs out; they are lost when the
 * time runs out first (or a zombie reaches the house).
 */
public class TimedWarEndGameCondition extends AbstractEndGameCondition {
    private final TimedWarLevel timedWarLevel;

    public TimedWarEndGameCondition(TimedWarLevel timedWarLevel) {
        this.timedWarLevel = timedWarLevel;
    }

    @Override
    public boolean isWin(GameModel model) {
        int targetKills = rules().getTimedWarTargetKills();
        if (targetKills <= 0) return super.isWin(model);
        return model.getZombiesKilled() >= targetKills;
    }

    @Override
    public boolean isGameOver(GameModel model) {
        if (super.isGameOver(model)) return true;

        float timeLimit = rules().getTimedWarLimit();
        return timeLimit > 0
                && model.getElapsedSeconds() >= timeLimit
                && !isWin(model);
    }

    private GameRules rules() {
        return timedWarLevel.getConfig().getRules();
    }
}
