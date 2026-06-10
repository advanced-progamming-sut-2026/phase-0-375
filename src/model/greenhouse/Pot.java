package model.greenhouse;

import model.enums.PotState;

import java.time.LocalDateTime;

/**
 * Represents a single pot/slot in the greenhouse.
 * A pot can be locked, empty, growing a plant, or ready for harvest.
 * Each pot has a fixed position in the 4x5 greenhouse grid.
 */
public class Pot {
    private int x;
    private int y;
    private PotState state;
    private String plantType;
    private LocalDateTime plantingTime;
    private float growthDurationHours;
    private LocalDateTime lastHarvestTime;
    private boolean isMarigold;

    public Pot(int x, int y) {
        this.x = x;
        this.y = y;
        this.state = (y >= 2) ? PotState.LOCKED : PotState.EMPTY; // in the beginning
        this.plantType = null;
        this.plantingTime = null;
        this.growthDurationHours = 0;
        this.lastHarvestTime = null;
        this.isMarigold = false;
    }

    /**
     * Unlocks this pot by changing its state from LOCKED to EMPTY.
     *
     * @return true if the pot was successfully unlocked
     */
    public boolean unlock() {
        return false;
    }

    /**
     * Plants a seed in this pot. The pot must be EMPTY.
     *
     * @param plantType the type of plant to grow
     * @param isMarigold whether the plant is a marigold
     * @param growthDurationHours hours until the plant is fully grown
     */
    public void plant(String plantType, boolean isMarigold,
                      float growthDurationHours) {}

    /**
     * Checks if the plant in this pot has finished growing.
     *
     * @return true if the plant is fully grown and ready for harvest
     */
    public boolean isReady() {
        return false;
    }

    /**
     * Calculates the remaining hours until the plant is fully grown.
     *
     * @return remaining growth hours, or 0 if already ready
     */
    public float getRemainingGrowthHours() {
        return 0;
    }

    /**
     * Harvests the plant from this pot, resetting it to EMPTY.
     * Records the harvest time and returns the plant type.
     *
     * @return the type of plant that was harvested, or null if not ready
     */
    public String harvest() {
        return null;
    }

    /**
     * Accelerates the growth of the plant by setting planting time
     * back so the plant becomes immediately ready.
     *
     * @return the number of gems cost for acceleration
     */
    public int accelerateGrowth() {
        return 0;
    }

    // --- Getters ---

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public PotState getState() {
        return state;
    }

    public String getPlantType() {
        return plantType;
    }

    public LocalDateTime getPlantingTime() {
        return plantingTime;
    }

    public float getGrowthDurationHours() {
        return growthDurationHours;
    }

    public LocalDateTime getLastHarvestTime() {
        return lastHarvestTime;
    }

    public boolean isMarigold() {
        return isMarigold;
    }

    // --- Setters ---

    public void setState(PotState state) {
        this.state = state;
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;
    }

    public void setPlantingTime(LocalDateTime plantingTime) {
        this.plantingTime = plantingTime;
    }

    public void setGrowthDurationHours(float growthDurationHours) {
        this.growthDurationHours = growthDurationHours;
    }

    public void setLastHarvestTime(LocalDateTime lastHarvestTime) {
        this.lastHarvestTime = lastHarvestTime;
    }

    public void setMarigold(boolean marigold) {
        isMarigold = marigold;
    }
}
