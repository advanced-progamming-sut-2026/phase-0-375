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
    private float armedElapsed;       // seconds since the trap finished arming
    private int growthStage;          // current growth stage for staged plants
    private boolean isDigesting;      // true for Chomper after swallowing a zombie
    private float digestRemaining;    // seconds left in digestion
    private ArmorType heldMetal;      // Metal last pulled by Magnet-shroom; null if none
    private int shotOrdinal;

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

    /** Seconds since {@link #setArmed(boolean) setArmed(true)}; {@code 0} while disarmed. */
    public float getArmedElapsed() {
        return armedElapsed;
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
        if (armed && !isArmed) {
            armedElapsed = 0f;
        } else if (!armed) {
            armedElapsed = 0f;
        }
        isArmed = armed;
    }

    public void addArmedElapsed(float deltaTime) {
        if (isArmed && deltaTime > 0f) {
            armedElapsed += deltaTime;
        }
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

    public int nextShotOrdinal() {
        return shotOrdinal++;
    }

    public int getShotOrdinal() {
        return shotOrdinal;
    }

    public void setShotOrdinal(int shotOrdinal) {
        this.shotOrdinal = Math.max(0, shotOrdinal);
    }

    public void restoreArmed(boolean armed, float elapsed) {
        this.isArmed = armed;
        this.armedElapsed = Math.max(0f, elapsed);
    }
}
