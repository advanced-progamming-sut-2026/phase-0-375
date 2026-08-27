package view.gui.anim;

import model.enums.Chapter;
import model.item.Grave;

/**
 * Gravestone clip + a short squash-and-stretch pop out of the ground.
 *
 * <p>Egypt uses {@link #PAM_EGYPT}; Dark Ages picks {@link #PAM_DARK_PLAIN},
 * {@link #PAM_DARK_SUN}, or {@link #PAM_DARK_PLANTFOOD} from {@link Grave.GraveType}.
 * Damage tiers and emerge timing match Egypt tombs.
 *
 * <p>Classic emerge (anticipation squash → stretch → settle), not a PAM clip.
 * Progress {@code 0} is a wide pancake; {@code 1} is rest scale.
 */
public final class GraveAnim {
    public static final String PAM_EGYPT = "EGYPT_HIEROGLYPH";
    public static final String PAM_DARK_PLAIN = "DARK_NOOP";
    public static final String PAM_DARK_SUN = "DARK_SUN";
    public static final String PAM_DARK_PLANTFOOD = "DARK_PLANTFOOD";

    /** Egypt default; prefer {@link #pamFor(Chapter, Grave)}. */
    public static final String PAM = PAM_EGYPT;

    /** Egypt / Dark Ages tomb PAM canvas (animations.json); scale is about this centre. */
    public static final float CANVAS = 390f;

    /** Whole pop is a few tenths of a second. */
    public static final float EMERGE_DURATION = 0.28f;

    private GraveAnim() {}

    /** Catalog PAM name for the active chapter and grave loot type. */
    public static String pamFor(Chapter chapter, Grave grave) {
        if (chapter == Chapter.DARK_AGES) {
            if (grave == null) {
                return PAM_DARK_PLAIN;
            }
            return switch (grave.getType()) {
                case SUN -> PAM_DARK_SUN;
                case PLANT_FOOD -> PAM_DARK_PLANTFOOD;
                default -> PAM_DARK_PLAIN;
            };
        }
        return PAM_EGYPT;
    }

    public static String clipForHp(int hp, int maxHp) {
        if (maxHp <= 0) {
            return "undamaged";
        }
        float f = hp / (float) maxHp;
        if (f > 0.8f) {
            return "undamaged";
        }
        if (f > 0.6f) {
            return "damage1";
        }
        if (f > 0.4f) {
            return "damage2";
        }
        if (f > 0.2f) {
            return "damage3";
        }
        return "damage4";
    }

    public static String clipFor(Grave grave) {
        if (grave == null) {
            return "undamaged";
        }
        return clipForHp(grave.getHp(), Grave.DEFAULT_HP);
    }

    /** Horizontal multiplier at emerge progress {@code u} in {@code [0, 1]}. */
    public static float scaleX(float u) {
        return lerpStops(u, 1.55f, 0.62f, 1.08f, 1f);
    }

    /** Vertical multiplier at emerge progress {@code u} in {@code [0, 1]}. */
    public static float scaleY(float u) {
        return lerpStops(u, 0.22f, 1.42f, 0.92f, 1f);
    }

    /**
     * Squash → stretch ({@code 0..0.4}) → overshoot ({@code 0.4..0.72}) → rest.
     */
    private static float lerpStops(float u, float squash, float stretch, float settle, float rest) {
        if (u <= 0f) {
            return squash;
        }
        if (u >= 1f) {
            return rest;
        }
        if (u < 0.4f) {
            return mix(squash, stretch, smooth(u / 0.4f));
        }
        if (u < 0.72f) {
            return mix(stretch, settle, smooth((u - 0.4f) / 0.32f));
        }
        return mix(settle, rest, smooth((u - 0.72f) / 0.28f));
    }

    private static float mix(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}
