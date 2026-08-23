package view.gui.anim;

/**
 * Draw scale for PAM / spritesheet art on the lawn.
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

    /**
     * Custom PNG plant spritesheets. Frames are full-bleed (no empty PAM canvas),
     * so {@link #PLANT} makes them look oversized next to PAM plants.
     */
    public static final float PLANT_SHEET = 0.35f;

    public static final float ZOMBIE = LAWN;

    public static final float PROJECTILE = LAWN;

    /**
     * Custom PNG projectile spritesheets (Cat-tail digits, …). Full-bleed like
     * {@link #PLANT_SHEET}, so smaller than PAM projectile scale.
     */
    public static final float PROJECTILE_SHEET = 0.35f;

    /** Lawn collectible. {@code SUN.PAM} canvas is 200, smaller than plant/zombie 390. */
    public static final float SUN = 0.25f;

    /** {@link #PLANT_SHEET} for spritesheet poses, otherwise {@link #PLANT}. */
    public static float forPlant(AnimPose pose) {
        return pose != null && pose.isSpritesheet() ? PLANT_SHEET : PLANT;
    }

    /** {@link #PROJECTILE_SHEET} for spritesheet poses, otherwise {@link #PROJECTILE}. */
    public static float forProjectile(AnimPose pose) {
        return pose != null && pose.isSpritesheet() ? PROJECTILE_SHEET : PROJECTILE;
    }
}
