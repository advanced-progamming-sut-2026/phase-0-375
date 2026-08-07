package model.game.rule;

import model.game.core.GameModel;
import model.game.wave.WaveManager;

/**
 * Shared logic for end-game conditions.
 *
 * <p>Defaults every level to the regular rules: the level is lost when a
 * zombie walks into the house, and won when every wave has been cleared and
 * no zombie is left on the lawn. Subclasses override to add their own
 * win/loss rules on top.
 */
abstract class AbstractEndGameCondition implements EndGameCondition {

    @Override
    public boolean isWin(GameModel model) {
        return allWavesCleared(model);
    }

    @Override
    public boolean isGameOver(GameModel model) {
        return model.isHouseBreached();
    }

    /** True when every wave has been cleared and the lawn is empty. */
    protected final boolean allWavesCleared(GameModel model) {
        WaveManager waves = model.getWaveManager();
        return waves != null && waves.isLevelDone() && model.getZombieCount() == 0;
    }
}
