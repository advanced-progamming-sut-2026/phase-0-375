package view.gui.lawn;

import view.gui.anim.AnimScale;
import view.gui.anim.GraveAnim;

/** Fly-to-coin-HUD timing for lawn loot (same feel as sun collect). */
public final class LootCollect {
    public static final float HIT_RADIUS = AnimScale.SUN * 120f;
    public static final float FLY_SEC = 0.72f;
    public static final float POP_SEC = GraveAnim.EMERGE_DURATION;

    private LootCollect() {}

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

    public static float popU(float elapsed) {
        return clamp01((elapsed - FLY_SEC) / POP_SEC);
    }

    public static boolean flying(float elapsed) {
        return elapsed < FLY_SEC;
    }

    public static boolean done(float elapsed) {
        return elapsed >= FLY_SEC + POP_SEC;
    }

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
