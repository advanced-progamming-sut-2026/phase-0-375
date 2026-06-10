package model.game.level.minigame.zombotany;

import model.enums.MiniGameType;
import model.game.GameModel;
import model.game.level.LevelConfig;
import model.game.level.minigame.MiniGameLevel;

/**
 * Zombotany mini-game.
 */
public class ZombotanyLevel extends MiniGameLevel {
    public ZombotanyLevel(LevelConfig config, MiniGameType miniGameType, int difficultyTier) {
        super(config, miniGameType, difficultyTier);
    }

    @Override
    public boolean canStart() {
        return false;
    }

    @Override
    public void onStart() {

    }

    @Override
    public void tick(float deltaTime) {

    }

    @Override
    public void onWaveCleared(int waveNumber) {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public void onFail() {

    }

    @Override
    public boolean checkWinCondition(GameModel model) {
        return false;
    }

    @Override
    public boolean checkLossCondition(GameModel model) {
        return false;
    }
}
