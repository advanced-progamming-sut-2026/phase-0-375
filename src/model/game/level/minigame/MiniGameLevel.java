package model.game.level.minigame;

import model.app.App;
import model.enums.MiniGameType;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.user.User;

/**
 * Base class for all mini-game levels
 */
public abstract class MiniGameLevel extends Level {
    private final MiniGameType miniGameType;
    private int difficultyTier;
    private boolean completed;
    private int coinReward;

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

    public int getCoinReward() {
        return coinReward;
    }

    public void setCoinReward(int coinReward) {
        this.coinReward = coinReward;
    }

    /**
     * Shared completion behaviour for all mini-games: marks the game as
     * completed, counts it on the user profile and pays the coin reward.
     * Subclasses needing extra behaviour should override and call super.
     */
    @Override
    public void onComplete() {
        if (completed) {
            return;
        }
        completed = true;
        User user = App.getInstance().getCurrentUser();
        if (user != null) {
            if (coinReward > 0) {
                user.setCoins(user.getCoins() + coinReward);
            }
            user.setCompletedMiniGames(user.getCompletedMiniGames() + 1);
            App.getInstance().getUserRepository().flush();
        }
    }
}
