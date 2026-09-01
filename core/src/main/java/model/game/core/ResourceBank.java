package model.game.core;

import model.app.App;
import model.user.User;

/**
 * The sun / plant-food economy for one level, extracted from
 * {@link GameModel} by composition: the model owns one instance and
 * delegates all balance changes to it.
 */
final class ResourceBank {

    private int sunAmount;
    private int plantFoodCount;
    private int persistentPlantFood; // portion of plantFoodCount backed by the user profile

    ResourceBank(int initialSun, int purchasedPlantFood) {
        this.sunAmount = initialSun;
        this.plantFoodCount = Math.max(0, purchasedPlantFood);
        this.persistentPlantFood = this.plantFoodCount;
    }

    int getSunAmount() { return sunAmount; }

    int getPlantFoodCount() { return plantFoodCount; }

    int getPersistentPlantFood() { return persistentPlantFood; }

    void restore(int sun, int plantFood, int persistent) {
        this.sunAmount = Math.max(0, sun);
        this.plantFoodCount = Math.max(0, plantFood);
        this.persistentPlantFood = Math.max(0, Math.min(persistent, this.plantFoodCount));
    }

    void addSun(int amount) {
        sunAmount += amount;
    }

    void setSunAmount(int amount) {
        sunAmount = Math.max(0, amount);
    }

    boolean spendSun(int amount) {
        if (sunAmount < amount) return false;
        sunAmount -= amount;
        return true;
    }

    void addPlantFood() {
        plantFoodCount++;
    }

    /** Consumes one plant food; purchased plant food is consumed from the user profile too. */
    boolean usePlantFood() {
        if (plantFoodCount < 1) return false;
        plantFoodCount--;
        if (persistentPlantFood > 0) {
            persistentPlantFood--;
            User owner = App.getInstance().getCurrentUser();
            if (owner != null && owner.getPlantFoodCount() > 0) {
                App.getInstance().getUserRepository().usePlantFood(owner.getUsername());
            }
        }
        return true;
    }
}
