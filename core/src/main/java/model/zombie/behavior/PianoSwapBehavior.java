package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.zombie.instance.ZombieInstance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Piano-swap behavior.
 */
public class PianoSwapBehavior implements ZombieBehavior {

    // --- Constants ---

    /** Seconds between two consecutive lane-swap pulses. */
    public static final float SWAP_INTERVAL = 5.0f;

    // --- State ---

    /** Seconds elapsed since the last swap pulse. */
    private float swapTimer = 0f;

    /** Total swap pulses emitted so far. */
    private int swapCount = 0;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        ZombieState state = zombie.getState();
        if (state == ZombieState.SPAWNING || state == ZombieState.STUNNED) {
            return;
        }

        swapTimer += deltaTime;
        if (swapTimer < SWAP_INTERVAL) {
            return;
        }
        swapTimer -= SWAP_INTERVAL;

        swapLanesOfNearbyZombies(zombie, context);
        swapCount++;
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.PIANO_SWAP;
    }

    // --- Core logic ---

    private void swapLanesOfNearbyZombies(ZombieInstance pianist, BehaviorContext context) {
        int rows = context.getRowCount();
        if (rows <= 1) {
            return; // nothing to swap with
        }

        for (int lane = 0;  lane < rows; lane++) {
            List<ZombieInstance> zombiesInLane = context.getZombiesInLane(lane);
            for (ZombieInstance other : zombiesInLane) {
                if (other == null || other == pianist || other.isDead()) {
                    continue;
                }
                int newRow = pickAdjacentRow(lane, rows);
                if (newRow != lane) {
                    context.moveZombieToLane(other, newRow);
                }
            }
        }
    }

    /**
     * Picks one of the two rows adjacent to {@code row} (or the single
     * adjacent row when {@code row} is on the field edge).
     */
    private int pickAdjacentRow(int row, int totalRows) {
        boolean canUp = row < totalRows - 1;
        boolean canDown = row > 0;
        if (canUp && canDown) {
            return ThreadLocalRandom.current().nextBoolean() ? row - 1 : row + 1;
        }
        if (canUp) {
            return row + 1;
        }
        if (canDown) {
            return row - 1;
        }
        return row;
    }

    // --- Getters / setters ---

    public float getSwapTimer() {
        return swapTimer;
    }

    public int getSwapCount() {
        return swapCount;
    }

    public void setSwapTimer(float swapTimer) {
        this.swapTimer = swapTimer;
    }

    public void setSwapCount(int swapCount) {
        this.swapCount = swapCount;
    }
}
