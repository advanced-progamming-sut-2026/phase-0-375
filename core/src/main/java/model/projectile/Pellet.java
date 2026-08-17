package model.projectile;

import model.game.map.FloatPoint;

/**
 * A standard straight-line pea (or similar) projectile fired by shooter
 * plants such as Peashooter.
 */
public class Pellet extends Projectile {

    /** {@code < 0} means this pellet never times out. */
    private float lifetimeRemaining = -1f;
    /** True when this pellet ricochets off lane edges (Grapeshot grapes). */
    private boolean bouncing;

    public Pellet(int damage, FloatPoint position, int row, float velocity) {
        super(damage, position, row, velocity);
    }

    public Pellet(int damage, FloatPoint position, int row, float velocity,
                  Element element, int direction) {
        super(damage, position, row, velocity, element, direction);
    }

    public void setLifetime(float seconds) {
        this.lifetimeRemaining = seconds;
    }

    public boolean isBouncing() {
        return bouncing;
    }

    public void setBouncing(boolean bouncing) {
        this.bouncing = bouncing;
    }

    /**
     * Counts down a finite lifetime.
     *
     * @return true once the pellet should despawn
     */
    public boolean tickLifetime(float deltaTime) {
        if (lifetimeRemaining < 0f) {
            return false;
        }
        lifetimeRemaining -= deltaTime;
        return lifetimeRemaining <= 0f;
    }
}
