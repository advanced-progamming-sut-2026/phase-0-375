package model.game.core;

/**
 * Frostbite Caves ice-wind gust sweeping one lane after a wave start.
 * Presentation-only record: the view reads {@link #getLane()} and
 * {@link #progress()} to play {@code frostbite_chill_wind} across the lane.
 */
public class IceWindGust {

    /** Seconds the gust takes to sweep across its lane. */
    public static final float SWEEP_SECONDS = 1.6f;

    private final int lane;
    private float age;

    IceWindGust(int lane) {
        this.lane = lane;
    }

    /**
     * Advances the gust.
     *
     * @return true once the sweep is over and the record can be dropped
     */
    boolean tick(float deltaTime) {
        age += deltaTime;
        return age >= SWEEP_SECONDS;
    }

    /** Lane row the wind blows through. */
    public int getLane() {
        return lane;
    }

    /** {@code 0..1} sweep progress; clamped at 1. */
    public float progress() {
        return Math.max(0f, Math.min(1f, age / SWEEP_SECONDS));
    }
}
