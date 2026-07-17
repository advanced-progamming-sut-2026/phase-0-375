package model.quest;

import model.app.App;
import model.enums.QuestRewardType;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

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
        User user = App.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        if (coinAmount > 0) {
            user.setCoins(user.getCoins() + coinAmount);
        }
        if (gemAmount > 0) {
            user.setGems(user.getGems() + gemAmount);
        }
        if (inventoryItem != null && !inventoryItem.isBlank() && inventoryItemAmount > 0) {
            if (user.getSeedPackets() == null) {
                user.setSeedPackets(new HashMap<>());
            }
            int current = user.getSeedPackets().getOrDefault(inventoryItem, 0);
            user.getSeedPackets().put(inventoryItem, current + inventoryItemAmount);
        }
        if (unlockableName != null && !unlockableName.isBlank()) {
            String resolved = resolveUnlockable(unlockableName, user);
            if (resolved != null) {
                if (user.getUnlockedPlants() == null) {
                    user.setUnlockedPlants(new HashSet<>());
                }
                user.getUnlockedPlants().add(resolved);
            }
        }
    }

    /** Resolves "random_*" placeholders to a real locked, kill-capable plant. */
    private String resolveUnlockable(String name, User user) {
        if (!name.toLowerCase().startsWith("random")) {
            return name;
        }
        try {
            List<Plant> candidates = new ArrayList<>();
            for (Plant p : PlantFactory.getAllDefinitions()) {
                boolean unlocked = user.getUnlockedPlants() != null
                        && user.getUnlockedPlants().contains(p.getName());
                if (!unlocked && p.getDamage() > 0) {
                    candidates.add(p);
                }
            }
            if (candidates.isEmpty()) {
                return null;
            }
            return candidates.get(new Random().nextInt(candidates.size())).getName();
        } catch (IllegalStateException e) {
            // PlantFactory not initialized yet; try once
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
                return resolveUnlockable(name, user);
            } catch (Exception ex) {
                return null;
            }
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
