package model.game.map;

import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LawnMower {
    /** Travel speed of a triggered mower, in grid-columns per second. */
    public static final float MOWER_SPEED = 12.0f;

    private boolean active;
    private boolean isTriggered;
    private float xPosition;

    private final List<ZombieInstance> sweepKills = new ArrayList<>();

    public LawnMower() {
        this.active = true;
        this.isTriggered = false;
        this.xPosition = 0f;
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
        xPosition = 0f;
        sweepKills.clear();
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
        xPosition += MOWER_SPEED * deltaTime;
        return xPosition >= columnCount;
    }
}
