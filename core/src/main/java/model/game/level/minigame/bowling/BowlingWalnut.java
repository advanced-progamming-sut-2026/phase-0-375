package model.game.level.minigame.bowling;

import model.enums.BowlingWalnutType;
import model.game.map.FloatPoint;
import model.projectile.Projectile;

/**
 * A rolling walnut projectile used in the Wallnut Bowling mini-game.
 */
public class BowlingWalnut extends Projectile {
    private BowlingWalnutType type;

    private float horizontalVelocity; // tiles/second
    /** Vertical velocity component in tiles/second (0 = rolling straight). */
    private float verticalVelocity;
    private int hitCount;

    public BowlingWalnut(int damage, FloatPoint position, int row, float velocity) {
        super(damage, position, row, velocity);
    }

    public BowlingWalnut(int damage, FloatPoint position, int row, float velocity,
                         Element element, int direction) {
        super(damage, position, row, velocity, element, direction);
    }

    public BowlingWalnutType getType() {
        return type;
    }

    public void setType(BowlingWalnutType type) {
        this.type = type;
    }

    public float getHorizontalVelocity() {
        return horizontalVelocity;
    }

    public void setHorizontalVelocity(float horizontalVelocity) {
        this.horizontalVelocity = horizontalVelocity;
    }

    public float getVerticalVelocity() {
        return verticalVelocity;
    }

    public void setVerticalVelocity(float verticalVelocity) {
        this.verticalVelocity = verticalVelocity;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void incrementHitCount() {
        hitCount++;
    }
}
