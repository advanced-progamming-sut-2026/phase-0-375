package view.gui.anim;

/**
 * Frostbite Caves slide tiles ("tileslider_iceage_up" / "_down") drawn on
 * slide terrain. Each PAM's origin sits exactly on its middle, so the draw
 * point is the tile centre. {@code idle} loops while nothing slides; when a
 * zombie starts to slide {@code active_start} plays once, then
 * {@code active_end} plays once.
 */
public final class SlideTileAnim {
    private SlideTileAnim() {}

    public static final String DOWN_PAM_PATH =
            "768/FULL/EFFECTS/TILESLIDER_ICEAGE_DOWN/TILESLIDER_ICEAGE_DOWN.PAM";
    public static final String UP_PAM_PATH =
            "768/FULL/EFFECTS/TILESLIDER_ICEAGE_UP/TILESLIDER_ICEAGE_UP.PAM";
    public static final String IDLE_CLIP = "idle";
    public static final String START_CLIP = "active_start";
    public static final String END_CLIP = "active_end";
}
