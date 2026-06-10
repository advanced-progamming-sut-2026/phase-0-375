package model.game.level.minigame;

import model.enums.MiniGameType;
import model.game.level.Level;
import model.game.level.LevelConfig;

/**
 * Base class for all mini-game levels
 */
public abstract class MiniGameLevel extends Level {
    private final MiniGameType miniGameType;
    private int difficultyTier;
    private boolean completed;

    public MiniGameLevel(LevelConfig config, MiniGameType miniGameType, int difficultyTier) {
        super(config);
        this.miniGameType = miniGameType;
        this.difficultyTier = difficultyTier;
        this.completed = false;
    }

    // --- Getters ---

    public MiniGameType getMiniGameType() {
        return miniGameType;
    }

    public int getDifficultyTier() {
        return difficultyTier;
    }

    public boolean isCompleted() {
        return completed;
    }

    // --- Setters ---

    public void setDifficultyTier(int difficultyTier) {
        this.difficultyTier = difficultyTier;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
