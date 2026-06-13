package model.game.level;

import model.game.core.GameModel;

/**
 * Represents a complete level in the game.
 */
public abstract class Level {
    private LevelConfig config;

    public Level(LevelConfig config) {
        this.config = config;
    }

    public LevelConfig getConfig() {
        return config;
    }

    public void setConfig(LevelConfig config) {
        this.config = config;
    }

    /** Checks if the level can be started */
    public abstract boolean canStart();

    /** Called when the level starts */
    public abstract void onStart();

    /** Called every tick */
    public abstract void tick(float deltaTime);

    /** Called when a wave is cleared */
    public abstract void onWaveCleared(int waveNumber);

    /** Called when level completes */
    public abstract void onComplete();

    /** Called when player loses the level */
    public abstract void onFail();

    /** Check if the level's win condition is met */
    public abstract boolean checkWinCondition(GameModel model);

    /** Check if the level's loss condition is met */
    public abstract boolean checkLossCondition(GameModel model);
}
