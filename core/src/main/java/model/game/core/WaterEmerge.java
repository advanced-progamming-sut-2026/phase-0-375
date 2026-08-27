package model.game.core;

import model.zombie.instance.ZombieInstance;

/**
 * Low-tide ambush emergence: the zombie was spawned beneath the water on a
 * submerged shallow tile and visually surfaces with the Snorkel-style water
 * mask + ripple — same pace as the Snorkeler rise.
 */
public class WaterEmerge {

    /** Seconds to surface; matches the Snorkeler rise (~0.5s at {@code RISE_SPEED 2/s}). */
    public static final float DURATION_SECONDS = 0.5f;

    private final ZombieInstance zombie;
    private float age;

    WaterEmerge(ZombieInstance zombie) {
        this.zombie = zombie;
    }

    /**
     * Advances the emergence.
     *
     * @return true once surfaced or the zombie is gone
     */
    boolean tick(float deltaTime) {
        if (zombie == null || zombie.isDead()) {
            return true;
        }
        age += deltaTime;
        return age >= DURATION_SECONDS;
    }

    /** The emerging zombie. */
    public ZombieInstance getZombie() {
        return zombie;
    }

    /** {@code 0..1} surfacing progress; clamped at 1. */
    public float progress() {
        return Math.max(0f, Math.min(1f, age / DURATION_SECONDS));
    }

    /**
     * Draw origin so the sprite's art top sits on {@code waterY} at progress 0
     * and on the normal stand origin at progress 1 (sinks under the water
     * mask, then rises — same direction as the snorkeler).
     */
    public static float drawOriginY(float standY, float waterY, float artTop,
                                    float progress) {
        return drawOriginY(standY, waterY, artTop, progress, 0f);
    }

    /**
     * Same as {@link #drawOriginY(float, float, float, float)} with extra downward
     * sink for tall art whose bounds sit below protruding limbs.
     */
    public static float drawOriginY(float standY, float waterY, float artTop,
                                    float progress, float extraSink) {
        float sink = Math.max(0f, artTop - waterY) + Math.max(0f, extraSink);
        float t = Math.max(0f, Math.min(1f, progress));
        return standY - sink * (1f - t);
    }
}
