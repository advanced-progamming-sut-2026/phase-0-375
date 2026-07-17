package model.game.rule;

import model.game.core.GameModel;
import model.game.level.minigame.MiniGameLevel;

/**
 * Mini-game levels define their own win/loss rules, so this adapter routes
 * the game loop's end-game evaluation to the level's
 * {@code checkWinCondition} / {@code checkLossCondition} overrides.
 */
public class MiniGameEndGameCondition implements EndGameCondition {

    private final MiniGameLevel level;

    public MiniGameEndGameCondition(MiniGameLevel level) {
        this.level = level;
    }

    @Override
    public boolean isWin(GameModel model) {
        return level != null && level.checkWinCondition(model);
    }

    @Override
    public boolean isGameOver(GameModel model) {
        return level != null && level.checkLossCondition(model);
    }
}
