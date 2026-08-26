package model.game.core;

import model.zombie.instance.ZombieInstance;

/**
 * Frostbite Caves slide in progress: a zombie has already been relocated to
 * {@code toRow} by gameplay, but the view glides it from {@code fromRow} over
 * {@link #GLIDE_SECONDS} instead of teleporting.
 */
public class LaneSlide {

    /** Seconds the visual glide from one lane to the next takes. */
    public static final float GLIDE_SECONDS = 0.5f;

    private final ZombieInstance zombie;
    private final int fromRow;
    private final int toRow;
    private float age;

    LaneSlide(ZombieInstance zombie, int fromRow, int toRow) {
        this.zombie = zombie;
        this.fromRow = fromRow;
        this.toRow = toRow;
    }

    /**
     * Advances the glide.
     *
     * @return true once the glide is over or the zombie is gone
     */
    boolean tick(float deltaTime) {
        if (zombie == null || zombie.isDead()) {
            return true;
        }
        age += deltaTime;
        return age >= GLIDE_SECONDS;
    }

    /** The sliding zombie. */
    public ZombieInstance getZombie() {
        return zombie;
    }

    /** Lane the zombie slid from. */
    public int getFromRow() {
        return fromRow;
    }

    /** Lane the zombie slid into (its logical lane already). */
    public int getToRow() {
        return toRow;
    }

    /** {@code 0..1} glide progress; clamped at 1. */
    public float progress() {
        return Math.max(0f, Math.min(1f, age / GLIDE_SECONDS));
    }
}
