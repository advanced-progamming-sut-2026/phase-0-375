package view.gui.lawn;

import view.gui.anim.AnimScale;
import view.gui.anim.GraveAnim;

/**
 * Screen-space plant-food pickup + fly-to-HUD timing. Mirrors {@link SunCollect}
 * but uses a slightly slower fly (plant food feels heavier) and a longer pop
 * vanish so the orb visibly stretches into the bank's logo circle.
 *
 * <p>Hit-test radius, easing helpers and vanish curve are intentionally the
 * same shape as the sun pickup so the two pickups share a feel; only the
 * timing constants differ.
 */
public final class PlantFoodCollect {
    /** Half of the PAM canvas, plus a little click slack (same as Sun). */
    public static final float HIT_RADIUS = AnimScale.SUN * 120f;
    /** Slower ease-in toward the HUD logo (plant-food is heavier than sun). */
    public static final float FLY_SEC = 0.5f;
    /** Grave-style squash/stretch vanish at the logo (slightly longer than sun). */
    public static final float POP_SEC = GraveAnim.EMERGE_DURATION + 0.04f;

    private PlantFoodCollect() {}

    public static boolean hits(float foodX, float foodY, float worldX, float worldY) {
        float dx = worldX - foodX;
        float dy = worldY - foodY;
        return dx * dx + dy * dy <= HIT_RADIUS * HIT_RADIUS;
    }

    /** Ease-in 0..1 while flying; 1 after. */
    public static float flyU(float elapsed) {
        float t = elapsed / FLY_SEC;
        if (t <= 0f) {
            return 0f;
        }
        if (t >= 1f) {
            return 1f;
        }
        return t * t;
    }

    /** 0 at logo arrival, 1 when the pop finishes. */
    public static float popU(float elapsed) {
        return clamp01((elapsed - FLY_SEC) / POP_SEC);
    }

    public static boolean flying(float elapsed) {
        return elapsed < FLY_SEC;
    }

    public static boolean done(float elapsed) {
        return elapsed >= FLY_SEC + POP_SEC;
    }

    /**
     * Vanish plays grave emerge backwards: rest → stretch (big) → pancake (small).
     */
    public static float vanishU(float elapsed) {
        return 1f - popU(elapsed);
    }

    static float clamp01(float t) {
        if (t <= 0f) {
            return 0f;
        }
        if (t >= 1f) {
            return 1f;
        }
        return t;
    }
}
