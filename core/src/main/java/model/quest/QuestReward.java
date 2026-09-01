package model.quest;

import model.app.App;
import model.enums.QuestRewardType;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.news.NewsFactory;
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
        var repo = App.getInstance().getUserRepository();
        String username = user.getUsername();
        if (coinAmount > 0 && repo != null) {
            repo.addCoins(username, coinAmount);
        }
        if (gemAmount > 0 && repo != null) {
            repo.addGems(username, gemAmount);
        }
        if (inventoryItem != null && !inventoryItem.isBlank() && inventoryItemAmount > 0 && repo != null) {
            String packetPlant = inventoryItem;
            if ("seed_packet".equalsIgnoreCase(inventoryItem)) {
                packetPlant = pickRandomUnlockedPlant(App.getInstance().getCurrentUser());
            }
            if (packetPlant != null) {
                repo.addSeedPackets(username, packetPlant, inventoryItemAmount);
                lastSeedPacketPlant = packetPlant;
            }
        }
        if (unlockableName != null && !unlockableName.isBlank() && repo != null) {
            String resolved = resolveUnlockable(unlockableName, App.getInstance().getCurrentUser());
            if (resolved != null) {
                repo.unlockPlant(username, resolved);
                lastUnlockedPlant = resolved;
            }
        }
    }

    /** Last plant that received generic seed packets (for UI messages). */
    private String lastSeedPacketPlant;
    /** Last plant unlocked by this reward (for UI messages). */
    private String lastUnlockedPlant;

    public String getLastSeedPacketPlant() {
        return lastSeedPacketPlant;
    }

    public String getLastUnlockedPlant() {
        return lastUnlockedPlant;
    }

    /** Picks a random plant the user has already unlocked. */
    private String pickRandomUnlockedPlant(User user) {
        if (user.getUnlockedPlants() == null || user.getUnlockedPlants().isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>(user.getUnlockedPlants());
        return names.get(new Random().nextInt(names.size()));
    }

    /** Resolves "random_*" placeholders to a random plant not unlocked yet. */
    private String resolveUnlockable(String name, User user) {
        if (!name.toLowerCase().startsWith("random")) {
            return name;
        }
        try {
            List<Plant> candidates = new ArrayList<>();
            for (Plant p : PlantFactory.getAllDefinitions()) {
                boolean unlocked = user.getUnlockedPlants() != null
                        && user.getUnlockedPlants().contains(p.getName());
                if (!unlocked) {
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
