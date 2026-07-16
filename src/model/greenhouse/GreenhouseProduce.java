package model.greenhouse;

/**
 * Represents the produce/result of harvesting a plant from the greenhouse.
 * A harvest result can yield coins (for marigold) or a stored boost
 * (for other unlocked plants).
 */
public class GreenhouseProduce {
    private final boolean isCoinReward;
    private final int coinAmount;
    private final boolean isBoost;
    private final String boostPlantType;

    public GreenhouseProduce(boolean isCoinReward, int coinAmount,
                             boolean isBoost, String boostPlantType) {
        this.isCoinReward = isCoinReward;
        this.coinAmount = coinAmount;
        this.isBoost = isBoost;
        this.boostPlantType = boostPlantType;
    }

    /** Builds a coin-reward produce (e.g. marigold harvest). */
    public static GreenhouseProduce forCoins(int amount) {
        return new GreenhouseProduce(true, amount, false, null);
    }

    /** Builds a boost-reward produce for the given plant type. */
    public static GreenhouseProduce forBoost(String plantType) {
        return new GreenhouseProduce(false, 0, true, plantType);
    }

    /** Builds an empty produce (no coin, no boost) — used when a boost was already stored. */
    public static GreenhouseProduce empty() {
        return new GreenhouseProduce(false, 0, false, null);
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

    // --- Setters (kept for Jackson / reflection-based frameworks) ---

    public void setCoinReward(boolean coinReward) {
        // no-op: this object is intentionally immutable from the API surface;
        // setters exist only to satisfy frameworks that need them.
    }

    public void setCoinAmount(int coinAmount) {
        // no-op (see above)
    }

    public void setBoost(boolean boost) {
        // no-op (see above)
    }

    public void setBoostPlantType(String boostPlantType) {
        // no-op (see above)
    }
}
