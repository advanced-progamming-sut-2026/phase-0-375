package model.projectile;

import model.game.map.FloatPoint;

/**
 * A lobbed projectile that follows an arc and splashes on impact Carries
 * an optional splash radius applied by the ProjectileSystem on hit.
 */
public class Splash extends Projectile {

    /** Splash radius in grid columns; 0 = single-target. */
    private float splashRadius;

    public Splash(int damage, FloatPoint position, int row, float velocity) {
        this(damage, position, row, velocity, Element.NONE, +1, 0f);
    }

    public Splash(int damage, FloatPoint position, int row, float velocity,
                  Element element, int direction, float splashRadius) {
        super(damage, position, row, velocity, element, direction);
        this.splashRadius = splashRadius;
    }

    public float getSplashRadius() {
        return splashRadius;
    }

    public void setSplashRadius(float splashRadius) {
        this.splashRadius = splashRadius;
    }
}