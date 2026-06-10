package model.greenhouse;

/**
 * Represents the produce/result of harvesting a plant from the greenhouse.
 * A harvest result can yield coins (for marigold) or a stored boost
 * (for other unlocked plants).
 */
public class GreenhouseProduce {
    private boolean isCoinReward;
    private int coinAmount;
    private boolean isBoost;
    private String boostPlantType;

    public GreenhouseProduce(boolean isCoinReward, int coinAmount,
                             boolean isBoost, String boostPlantType) {
        this.isCoinReward = isCoinReward;
        this.coinAmount = coinAmount;
        this.isBoost = isBoost;
        this.boostPlantType = boostPlantType;
    }

    // --- Getters ---

    public boolean isCoinReward() {
        return isCoinReward;
    }

    public int getCoinAmount() {
        return coinAmount;
    }

    public boolean isBoost() {
        return isBoost;
    }

    public String getBoostPlantType() {
        return boostPlantType;
    }

    // --- Setters ---

    public void setCoinReward(boolean coinReward) {
        isCoinReward = coinReward;
    }

    public void setCoinAmount(int coinAmount) {
        this.coinAmount = coinAmount;
    }

    public void setBoost(boolean boost) {
        isBoost = boost;
    }

    public void setBoostPlantType(String boostPlantType) {
        this.boostPlantType = boostPlantType;
    }
}
