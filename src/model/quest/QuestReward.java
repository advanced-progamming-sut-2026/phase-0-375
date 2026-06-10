package model.quest;

import model.enums.QuestRewardType;

/**
 * Represents the reward given upon completing a quest.
 * A reward can be currency (coins/gems), an unlockable (plant/level),
 * or inventory items (seed packets).
 */
public class QuestReward {
    private QuestRewardType type;
    private int coinAmount;
    private int gemAmount;
    private String unlockableName;
    private String inventoryItem;
    private int inventoryItemAmount;

    public QuestReward(QuestRewardType type, int coinAmount, int gemAmount,
                       String unlockableName, String inventoryItem, int inventoryItemAmount) {
        this.type = type;
        this.coinAmount = coinAmount;
        this.gemAmount = gemAmount;
        this.unlockableName = unlockableName;
        this.inventoryItem = inventoryItem;
        this.inventoryItemAmount = inventoryItemAmount;
    }
    /**
     * Grants the reward to the player.
     */
    public void grant() {}

    // --- Getters ---

    public QuestRewardType getType() {
        return type;
    }

    public int getCoinAmount() {
        return coinAmount;
    }

    public int getGemAmount() {
        return gemAmount;
    }

    public String getUnlockableName() {
        return unlockableName;
    }

    public String getInventoryItem() {
        return inventoryItem;
    }

    public int getInventoryItemAmount() {
        return inventoryItemAmount;
    }

    // --- Setters ---

    public void setType(QuestRewardType type) {
        this.type = type;
    }

    public void setCoinAmount(int coinAmount) {
        this.coinAmount = coinAmount;
    }

    public void setGemAmount(int gemAmount) {
        this.gemAmount = gemAmount;
    }

    public void setUnlockableName(String unlockableName) {
        this.unlockableName = unlockableName;
    }

    public void setInventoryItem(String inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public void setInventoryItemAmount(int inventoryItemAmount) {
        this.inventoryItemAmount = inventoryItemAmount;
    }
}
