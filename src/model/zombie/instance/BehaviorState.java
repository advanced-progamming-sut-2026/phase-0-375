package model.zombie.instance;

import model.enums.ZombieBehaviorType;

/**
 * Runtime state tracker for a single behavior on a zombie instance
 */
public class BehaviorState {
    private ZombieBehaviorType behaviorType;
    private float cooldownRemaining;    // seconds until the behavior can fire again
    private boolean isActive;           // whether the behavior is currently performing its action
    private int ammoRemaining;          // for limited-use behaviors (e.g. Tomb Raiser bones)
    private float actionProgress;       // 0..1 progress for actions with a cast time
    private float currentSpeedModifier; // speed buff/debuff applied by this behavior (1.0 = normal)

    public BehaviorState(ZombieBehaviorType behaviorType) {
        this.behaviorType = behaviorType;
        this.isActive = true;
        this.currentSpeedModifier = 1.0f;
    }

    // --- Getters ---

    public ZombieBehaviorType getBehaviorType() {
        return behaviorType;
    }

    public float getCooldownRemaining() {
        return cooldownRemaining;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getAmmoRemaining() {
        return ammoRemaining;
    }

    public float getActionProgress() {
        return actionProgress;
    }

    public float getCurrentSpeedModifier() {
        return currentSpeedModifier;
    }

    // --- Setters ---

    public void setBehaviorType(ZombieBehaviorType behaviorType) {
        this.behaviorType = behaviorType;
    }

    public void setCooldownRemaining(float cooldownRemaining) {
        this.cooldownRemaining = cooldownRemaining;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setAmmoRemaining(int ammoRemaining) {
        this.ammoRemaining = ammoRemaining;
    }

    public void setActionProgress(float actionProgress) {
        this.actionProgress = actionProgress;
    }

    public void setCurrentSpeedModifier(float currentSpeedModifier) {
        this.currentSpeedModifier = currentSpeedModifier;
    }
}
