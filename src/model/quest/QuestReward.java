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
     * Grants this reward to the currently logged-in user
     * (coins/gems, an inventory item, or an unlockable).
     */
    public void grant() {
        model.user.User user = model.app.App.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        if (coinAmount > 0) {
            user.setCoins(user.getCoins() + coinAmount);
        }
        if (gemAmount > 0) {
            user.setGems(user.getGems() + gemAmount);
        }
        if (inventoryItem != null && !inventoryItem.isBlank()
                && inventoryItemAmount > 0 && user.getSeedPackets() != null) {
            int current = user.getSeedPackets().getOrDefault(inventoryItem, 0);
            user.getSeedPackets().put(inventoryItem, current + inventoryItemAmount);
        }
        if (unlockableName != null && !unlockableName.isBlank() && user.getUnlockedPlants() != null) {
            user.getUnlockedPlants().add(unlockableName);
        }
    }

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
