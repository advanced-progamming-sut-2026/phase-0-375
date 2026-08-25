package view.gui.lawn;

import com.badlogic.gdx.graphics.OrthographicCamera;

import java.util.Random;

/**
 * Short decaying camera rumble. Pulse from Gargantuar walk stomps, plant/zombie
 * explosions, and anything else that should rattle the lawn;
 * {@link view.gui.screen.AbstractGameplayScreen} applies it after anchoring so the
 * house stays pinned.
 */
public final class ScreenShake {
    private static final float DURATION = 0.22f;
    /** World pixels at the start of a pulse. */
    private static final float AMPLITUDE = 14f;

    private final Random rng = new Random();
    private float remaining;
    private float ox;
    private float oy;

    public void pulse() {
        remaining = DURATION;
    }

    public void update(float delta) {
        if (remaining <= 0f) {
            ox = 0f;
            oy = 0f;
            return;
        }
        remaining -= Math.max(0f, delta);
        if (remaining <= 0f) {
            remaining = 0f;
            ox = 0f;
            oy = 0f;
            return;
        }
        float mag = AMPLITUDE * (remaining / DURATION);
        ox = (rng.nextFloat() * 2f - 1f) * mag;
        oy = (rng.nextFloat() * 2f - 1f) * mag * 0.65f;
    }

    public boolean active() {
        return remaining > 0f;
    }

    public float offsetX() {
        return ox;
    }

    public float offsetY() {
        return oy;
    }

    public void apply(OrthographicCamera camera) {
        if (ox == 0f && oy == 0f) {
            return;
        }
        camera.position.add(ox, oy, 0f);
        camera.update();
    }
}
