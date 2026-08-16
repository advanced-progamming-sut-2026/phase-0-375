package model.projectile;

import model.game.map.FloatPoint;

/**
 * Stationary fume burst spawned on a tile in front of Fume-shroom.
 * Damages zombies on that tile once, then plays out and despawns.
 */
public class FumeCloud extends Projectile {

    private float remaining;
    private boolean burstDone;

    public FumeCloud(int damage, FloatPoint position, int row, float lifetime) {
        super(damage, position, row, 0f);
        setPierce(true);
        this.remaining = Math.max(0f, lifetime);
        this.burstDone = false;
    }

    public boolean isBurstDone() {
        return burstDone;
    }

    public void markBurstDone() {
        this.burstDone = true;
    }

    /** @return true once the bubble clip has finished. */
    public boolean tickLifetime(float deltaTime) {
        remaining -= deltaTime;
        return remaining <= 0f;
    }
}
