package model.data.minigame;

import model.data.level.LevelDataEntry;

/**
 * JSON DTO for one mini-game stage in minigames.json.
 *
 * Inherits every regular level field (rows, columns, rules, waves, ...)
 * from LevelDataEntry so mini-game stages are described with the exact
 * same vocabulary as normal levels, and adds the mini-game specific keys.
 */
public class MiniGameDataEntry extends LevelDataEntry {

    private String miniGameType;
    private int stage = 1;
    private int difficultyTier = 3;
    private int coinReward = 100;

    public String getMiniGameType() { return miniGameType; }
    public void setMiniGameType(String miniGameType) { this.miniGameType = miniGameType; }

    public int getStage() { return stage; }
    public void setStage(int stage) { this.stage = stage; }

    public int getDifficultyTier() { return difficultyTier; }
    public void setDifficultyTier(int difficultyTier) { this.difficultyTier = difficultyTier; }

    public int getCoinReward() { return coinReward; }
    public void setCoinReward(int coinReward) { this.coinReward = coinReward; }
}
