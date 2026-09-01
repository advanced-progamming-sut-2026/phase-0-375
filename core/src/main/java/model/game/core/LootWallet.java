package model.game.core;

import model.app.App;
import model.user.User;

/** Diamonds / coins / flower pots earned during a level. */
final class LootWallet {

    private int diamondCount;
    private int coinCount;
    private int flowerPotCount;

    void restore(int diamonds, int coins, int pots) {
        diamondCount = Math.max(0, diamonds);
        coinCount = Math.max(0, coins);
        flowerPotCount = Math.max(0, pots);
    }

    void addDiamonds(int amount) {
        if (amount > 0) {
            diamondCount += amount;
            model.user.persistance.UserSync.addGems(amount);
        }
    }

    void addCoins(int amount) {
        if (amount > 0) {
            coinCount += amount;
            model.user.persistance.UserSync.addCoins(amount);
        }
    }

    void addFlowerPots(int amount) {
        if (amount <= 0) {
            return;
        }
        flowerPotCount += amount;
        User user = App.getInstance().getCurrentUser();
        if (user == null || App.getInstance().getUserRepository() == null) {
            return;
        }
        for (int i = 0; i < amount; i++) {
            int potIndex = user.getUnlockedPots() + i;
            int x = potIndex % 4;
            int y = potIndex / 4;
            App.getInstance().getUserRepository()
                    .unlockGreenhousePot(user.getUsername(), x, y);
        }
    }

    int diamonds() {
        return diamondCount;
    }

    int coins() {
        return coinCount;
    }

    int flowerPots() {
        return flowerPotCount;
    }
}
