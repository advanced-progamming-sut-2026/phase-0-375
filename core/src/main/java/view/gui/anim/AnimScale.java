package view.gui.anim;

/**
 * Draw scale for PAM art on the lawn.
 *
 * <p>PAM clips are authored against a larger board than {@link view.gui.lawn.LawnLayout}
 * cells, so drawing them 1:1 makes plants and zombies overflow their cell.
 * {@link view.gui.lawn.LawnEntityRenderer} passes these factors to {@code PamPlayer.draw},
 * which scales the clip around the entity's cell center.
 *
 * <p>Tune here rather than at call sites; per-entity tweaks (Gargantuar, Imp, …) belong on
 * {@link AnimPose#scale()} via a profile in {@code view.gui.anim.plant} /
 * {@code view.gui.anim.zombie}.
 */
public final class AnimScale {
    private AnimScale() {}

    /** Base lawn art scale shared by both teams. */
    public static final float LAWN = 0.6f;

    public static final float PLANT = LAWN;

    public static final float ZOMBIE = LAWN;

    /** Lawn collectible. {@code SUN.PAM} canvas is 200, smaller than plant/zombie 390. */
    public static final float SUN = 0.25f;
}
