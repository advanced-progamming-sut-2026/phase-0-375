package view.gui.anim;

/**
 * Frostbite Caves ice wind ("frostbite_chill_wind") that sweeps a hit lane
 * when an icy wind blows at wave start: the clip plays looped while the gust
 * travels right-to-left across the lane.
 */
public final class IceWindAnim {
    private IceWindAnim() {}

    /** Effect PAM carrying the {@code animation} clip. */
    public static final String PAM_PATH =
            "768/FULL/EFFECTS/FROSTBITE_CHILL_WIND/FROSTBITE_CHILL_WIND.PAM";
    public static final String CLIP = "animation";

    /** Gust art height expressed in lawn cells so frost puffs fill the lane. */
    public static final float HEIGHT_CELLS = 1.25f;

    /** Pixels past each lawn edge where the gust materialises / dies out. */
    public static final float START_MARGIN_PX = 160f;
}
