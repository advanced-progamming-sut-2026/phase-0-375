package model.game.map;

import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LawnMower {
    /** Travel speed of a triggered mower, in grid-columns per second. */
    public static final float MOWER_SPEED = 10.8f;
    /** Rest column: fully left of column 0 so the blade sits in the margin. */
    public static final float REST_COL = -1.1f;
    /**
     * Fallback delay before the blade starts moving when no GUI clip is driving
     * {@link #beginSweep()}. GUI typically fires sooner when {@code transition} ends.
     */
    public static final float TRANSITION_SEC = 1.0f;

    private boolean active;
    private boolean isTriggered;
    private boolean sweeping;
    private float xPosition;
    private float transitionElapsed;

    private final List<ZombieInstance> sweepKills = new ArrayList<>();

    public LawnMower() {
        this.active = true;
        this.isTriggered = false;
        this.xPosition = REST_COL;
    }

    /** @return true if this mower is still sitting in the lane waiting to fire. */
    public boolean isActive() {
        return active;
    }

    /** @return true once a zombie has triggered this mower and it is sweeping the lane. */
    public boolean isTriggered() {
        return isTriggered;
    }

    /** @return the mower's current X (column) position while sweeping. */
    public float getXPosition() {
        return xPosition;
    }

    /**
     * Triggers the mower: it becomes inactive (cannot be retriggered)
     * and starts sweeping the lane. The triggering zombie is recorded
     * so it shows up in the sweep-kill notification.
     */
    public void trigger() {
        if (!active) return;
        isTriggered = true;
        active = false;
        sweeping = false;
        xPosition = REST_COL;
        transitionElapsed = 0f;
        sweepKills.clear();
    }

    /** Called when the GUI {@code transition} clip finishes; mower starts moving. */
    public void beginSweep() {
        if (isTriggered && !sweeping) {
            sweeping = true;
            xPosition = REST_COL;
        }
    }

    public boolean isSweeping() {
        return isTriggered && sweeping;
    }

    public void recordSweepKill(ZombieInstance zombie) {
        if (zombie != null && !sweepKills.contains(zombie)) {
            sweepKills.add(zombie);
        }
    }

    public List<ZombieInstance> getSweepKills() {
        return Collections.unmodifiableList(sweepKills);
    }

    /**
     * Advances the mower by {@code deltaTime} seconds. Returns true once
     * the mower has crossed the rightmost column and can be removed.
     *
     * @param columnCount total number of columns on the map
     */
    public boolean tick(float deltaTime, int columnCount) {
        if (!isTriggered) return false;
        if (!sweeping) {
            transitionElapsed += deltaTime;
            if (transitionElapsed < TRANSITION_SEC) {
                return false;
            }
            deltaTime = transitionElapsed - TRANSITION_SEC;
            beginSweep();
            if (deltaTime <= 0f) {
                return false;
            }
        }
        xPosition += MOWER_SPEED * deltaTime;
        return xPosition >= columnCount + 1.5f;
    }
}
