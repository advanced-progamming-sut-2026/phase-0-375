package model.game.save;

import model.enums.PlantAbilityType;

/** Snapshot of one plant ability's runtime state. */
public class AbilitySave {
    private PlantAbilityType abilityType;
    private float cooldownRemaining;
    private float chargeProgress;
    private boolean active = true;
    private boolean armed;
    private float armedElapsed;
    private int growthStage;
    private boolean digesting;
    private float digestRemaining;
    private int shotOrdinal;

    public PlantAbilityType getAbilityType() { return abilityType; }
    public void setAbilityType(PlantAbilityType abilityType) { this.abilityType = abilityType; }
    public float getCooldownRemaining() { return cooldownRemaining; }
    public void setCooldownRemaining(float cooldownRemaining) { this.cooldownRemaining = cooldownRemaining; }
    public float getChargeProgress() { return chargeProgress; }
    public void setChargeProgress(float chargeProgress) { this.chargeProgress = chargeProgress; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isArmed() { return armed; }
    public void setArmed(boolean armed) { this.armed = armed; }
    public float getArmedElapsed() { return armedElapsed; }
    public void setArmedElapsed(float armedElapsed) { this.armedElapsed = armedElapsed; }
    public int getGrowthStage() { return growthStage; }
    public void setGrowthStage(int growthStage) { this.growthStage = growthStage; }
    public boolean isDigesting() { return digesting; }
    public void setDigesting(boolean digesting) { this.digesting = digesting; }
    public float getDigestRemaining() { return digestRemaining; }
    public void setDigestRemaining(float digestRemaining) { this.digestRemaining = digestRemaining; }
    public int getShotOrdinal() { return shotOrdinal; }
    public void setShotOrdinal(int shotOrdinal) { this.shotOrdinal = shotOrdinal; }
}
