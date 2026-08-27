package view.gui.anim;

/**
 * Ancient Egypt sandstorm ("sandstorm_top") that carries tornado zombies in
 * from off-screen right: intro plays as the storm starts moving, loop repeats
 * until touchdown, then outro plays once and fades the storm away over the
 * freshly landed (still hidden) zombie.
 */
public final class SandstormAnim {
    private SandstormAnim() {}

    /** Effect PAM carrying the intro / loop / outro clips. */
    public static final String PAM_PATH =
            "768/INITIAL/EFFECTS/SANDSTORM_TOP/SANDSTORM_TOP.PAM";
    public static final String INTRO_CLIP = "intro";
    public static final String LOOP_CLIP = "loop";
    public static final String OUTRO_CLIP = "outro";

    /** Storm height expressed in lawn cells so the dust covers a zombie. */
    public static final float HEIGHT_CELLS = 2.6f;

    /** Pixels past the zombie entry edge where the storm materialises. */
    public static final float START_MARGIN_PX = 260f;
}
