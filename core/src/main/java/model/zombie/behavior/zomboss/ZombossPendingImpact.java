package model.zombie.behavior.zomboss;

/**
 * A fireball (or similar lobbed attack) currently in flight toward a tile.
 */
public final class ZombossPendingImpact {
    private final int row;
    private final int col;
    private final float travelSeconds;
    private float elapsed;
    private boolean resolved;

    public ZombossPendingImpact(int row, int col, float travelSeconds) {
        this.row = row;
        this.col = col;
        this.travelSeconds = Math.max(0.05f, travelSeconds);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public float getTravelSeconds() {
        return travelSeconds;
    }

    public float getElapsed() {
        return elapsed;
    }

    /** 0 = just spawned, 1 = about to impact. */
    public float progress01() {
        return Math.min(1f, elapsed / travelSeconds);
    }

    public boolean isResolved() {
        return resolved;
    }

    /** @return true once the impact should resolve this tick. */
    boolean tick(float deltaTime) {
        if (resolved) {
            return false;
        }
        elapsed += deltaTime;
        if (elapsed >= travelSeconds) {
            resolved = true;
            return true;
        }
        return false;
    }
}
