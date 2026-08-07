package model.game.level.minigame.zombotany;

import model.enums.MiniGameType;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.level.minigame.MiniGameLevel;

/**
 * Zombotany mini-game
 */
public class ZombotanyLevel extends MiniGameLevel {

    public ZombotanyLevel(LevelConfig config, MiniGameType miniGameType, int difficultyTier) {
        super(config, miniGameType, difficultyTier);
    }

    @Override
    public boolean canStart() {
        LevelConfig config = getConfig();
        return config != null
                && config.getRows() > 0
                && config.getColumns() > 0
                && config.getRules() != null
                && config.getWaves() != null
                && !config.getWaves().isEmpty();
    }

    @Override
    public void onStart() {
        // Nothing to pre-place: waves and zombie behaviors do all the work.
    }

    @Override
    public void tick(float deltaTime) {
        // No per-tick logic: Zombotany abilities run as ZombieBehaviors.
    }

    @Override
    public void onWaveCleared(int waveNumber) {
        // Nothing special happens on wave clear.
    }

    @Override
    public void onFail() {
        // Nothing to roll back on failure.
    }

    /** Won when every wave has been sent and the lawn is clear of zombies. */
    @Override
    public boolean checkWinCondition(GameModel model) {
        return model != null
                && model.getWaveManager() != null
                && model.getWaveManager().isLevelDone()
                && model.getZombieCount() == 0;
    }

    /** Lost when a zombie walks into the house. */
    @Override
    public boolean checkLossCondition(GameModel model) {
        return model != null && model.isHouseBreached();
    }
}
