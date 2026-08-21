package view.gui.lawn;

import view.gui.anim.AnimScale;
import view.gui.anim.GraveAnim;

/**
 * Screen-space sun pickup + fly-to-HUD timing. Hit uses the drawn sprite,
 * not the spawn tile.
 */
public final class SunCollect {
    /** Half of the PAM canvas, plus a little click slack. */
    public static final float HIT_RADIUS = AnimScale.SUN * 120f;
    /** Semi-fast ease-in toward the HUD logo. */
    public static final float FLY_SEC = 0.4f;
    /** Grave-style squash/stretch vanish at the logo. */
    public static final float POP_SEC = GraveAnim.EMERGE_DURATION;

    private SunCollect() {}

    public static boolean hits(float sunX, float sunY, float worldX, float worldY) {
        float dx = worldX - sunX;
        float dy = worldY - sunY;
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
