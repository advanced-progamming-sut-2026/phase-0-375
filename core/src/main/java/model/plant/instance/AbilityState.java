package model.plant.instance;

import model.enums.ArmorType;
import model.enums.PlantAbilityType;

/**
 * Runtime state tracker for a single ability on a plant instance.
 */
public class AbilityState {
    private PlantAbilityType abilityType;
    private float cooldownRemaining;  // seconds until the ability can fire again
    private float chargeProgress;     // 0..1 for charge-type abilities
    private boolean isActive;         // whether the ability is currently performing its action
    private boolean isArmed;          // true for traps that have finished arming
    private int growthStage;          // current growth stage for staged plants
    private boolean isDigesting;      // true for Chomper after swallowing a zombie
    private float digestRemaining;    // seconds left in digestion
    private ArmorType heldMetal;      // Metal last pulled by Magnet-shroom; null if none

    public AbilityState(PlantAbilityType abilityType) {
        this.abilityType = abilityType;
        this.isActive = true;
    }

    // --- Getters ---

    public PlantAbilityType getAbilityType() {
        return abilityType;
    }

    public float getCooldownRemaining() {
        return cooldownRemaining;
    }

    public float getChargeProgress() {
        return chargeProgress;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isArmed() {
        return isArmed;
    }

    public int getGrowthStage() {
        return growthStage;
    }

    public boolean isDigesting() {
        return isDigesting;
    }

    public float getDigestRemaining() {
        return digestRemaining;
    }

    public ArmorType getHeldMetal() {
        return heldMetal;
    }

    // --- Setters ---

    public void setAbilityType(PlantAbilityType abilityType) {
        this.abilityType = abilityType;
    }

    public void setCooldownRemaining(float cooldownRemaining) {
        this.cooldownRemaining = cooldownRemaining;
    }

    public void setChargeProgress(float chargeProgress) {
        this.chargeProgress = chargeProgress;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setArmed(boolean armed) {
        isArmed = armed;
    }

    public void setGrowthStage(int growthStage) {
        this.growthStage = growthStage;
    }

    public void setDigesting(boolean digesting) {
        isDigesting = digesting;
    }

    public void setDigestRemaining(float digestRemaining) {
        this.digestRemaining = digestRemaining;
    }

    public void setHeldMetal(ArmorType heldMetal) {
        this.heldMetal = heldMetal;
    }
}
