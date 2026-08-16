package model.projectile;

import model.game.map.FloatPoint;
import model.zombie.instance.ZombieInstance;

/**
 * A lobbed projectile that follows a parabola to its target and splashes
 * on impact. Carries an optional splash radius applied by the
 * ProjectileSystem on hit.
 */
public class Splash extends Projectile {

    private static final float MIN_FLIGHT_SECONDS = 0.4f;
    private static final float MIN_PEAK_CELLS = 0.55f;
    private static final float PEAK_PER_TILE = 0.12f;
    private static final float MAX_PEAK_CELLS = 1.35f;
    private static final float MIN_DISTANCE = 0.25f;

    /** Splash radius in grid columns; 0 = single-target. */
    private float splashRadius;

    private float originX;
    private float originY;
    private float landingX;
    private float landingY;
    private float flightDuration;
    private float flightElapsed;
    private float peakHeight;
    private boolean lobbing;
    private boolean landed;

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

    /**
     * Locks a throw from the current position to {@code (landingX, landingY)}.
     * Duration follows {@link #getVelocity()} so travel time stays close to
     * the old linear lob.
     */
    public void beginLob(float originX, float originY, float landingX, float landingY) {
        this.originX = originX;
        this.originY = originY;
        this.landingX = landingX;
        this.landingY = landingY;
        float dx = landingX - originX;
        float dy = landingY - originY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < MIN_DISTANCE) {
            dist = MIN_DISTANCE;
        }
        float speed = getVelocity() > 0f ? getVelocity() : 0.8f;
        this.flightDuration = Math.max(MIN_FLIGHT_SECONDS, dist / speed);
        this.flightElapsed = 0f;
        this.peakHeight = Math.min(MAX_PEAK_CELLS, MIN_PEAK_CELLS + dist * PEAK_PER_TILE);
        this.lobbing = true;
        this.landed = false;
        setDirection(dx >= 0f ? +1 : -1);
        setX(originX);
        setY(originY);
        setRow(Math.round(originY));
    }

    /** @return true if this splash is flying a parabola. */
    public boolean isLobbing() {
        return lobbing;
    }

    /** @return true after the lob has reached its landing point. */
    public boolean hasLanded() {
        return landed;
    }

    /**
     * Advances along the parabola. While the homing target is alive the
     * landing point tracks it so the shot still hits.
     *
     * @return true once the projectile has landed (this tick or earlier)
     */
    public boolean advanceLob(float deltaTime) {
        if (!lobbing) {
            return landed;
        }
        if (landed) {
            return true;
        }
        trackLanding();
        flightElapsed += Math.max(0f, deltaTime);
        float t = flightDuration <= 0f ? 1f : flightElapsed / flightDuration;
        if (t >= 1f) {
            t = 1f;
            landed = true;
        }
        setX(originX + (landingX - originX) * t);
        setY(originY + (landingY - originY) * t);
        int newRow = Math.round(getY());
        if (newRow != getRow()) {
            setRow(newRow);
        }
        return landed;
    }

    /**
     * Extra height above the lawn, in cell units. Peaks at mid-flight:
     * {@code 4h t (1-t)}.
     */
    public float getVisualHeight() {
        if (!lobbing || landed || flightDuration <= 0f) {
            return 0f;
        }
        float t = flightElapsed / flightDuration;
        if (t < 0f) {
            t = 0f;
        } else if (t > 1f) {
            t = 1f;
        }
        return 4f * peakHeight * t * (1f - t);
    }

    private void trackLanding() {
        ZombieInstance target = getHomingTarget();
        if (target == null || target.isDead() || target.getContinuousPosition() == null) {
            return;
        }
        landingX = target.getContinuousX();
        landingY = target.getContinuousY();
    }
}
