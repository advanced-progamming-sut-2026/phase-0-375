package view.gui.anim.zombie;

import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;

/**
 * How a zombie's walk cycle maps onto board travel: {@link view.gui.lawn.LawnEntityRenderer}
 * drives the clip by distance so one cycle covers exactly {@link #stepTiles()}, then offsets the
 * sprite by the measured {@link ZombieFootfallCurve} to keep {@link #GROUND_SWATCH} still. Per-zombie
 * values live in {@link ZombieGaitProfiles}; owned by the zombie team.
 */
public final class ZombieGait {
    /**
     * Hidden PAM locator at the ground-contact point. PvZ2 names it in {@code GroundTrackName};
     * libPVZ keeps it undrawn. Measuring it is how the walk stays planted.
     */
    public static final String GROUND_SWATCH = "ground_swatch";

    private static final String[] GROUND_TRACK = {GROUND_SWATCH};

    /**
     * Two steps per tile. The art's own stride is a little shorter, so the swatch creeps to make
     * cycles line up with the board — worth the trade.
     */
    public static final ZombieGait DEFAULT = new ZombieGait(0.5f, GROUND_TRACK, true);

    private final float stepTiles;
    private final String[] trackParts;
    private final boolean enabled;

    private ZombieGait(float stepTiles, String[] trackParts, boolean enabled) {
        this.stepTiles = stepTiles > 0f ? stepTiles : 0.5f;
        this.trackParts = trackParts;
        this.enabled = enabled;
    }

    /**
     * A zombie that covers {@code stepTiles} per walk cycle, measuring {@code ground_swatch}.
     */
    public static ZombieGait of(float stepTiles) {
        return new ZombieGait(stepTiles, GROUND_TRACK, true);
    }

    /** Same as {@link #of(float)} but samples a different PAM part (rare; sheets almost always use the swatch). */
    public static ZombieGait of(float stepTiles, String... trackParts) {
        return new ZombieGait(stepTiles, trackParts, true);
    }

    /** Opts a zombie out entirely, back onto wall-clock playback ({@code GroundTrackName: none}). */
    public static ZombieGait disabled() {
        return new ZombieGait(0.5f, null, false);
    }

    /** Board distance covered by one full walk cycle, in tiles. */
    public float stepTiles() {
        return stepTiles;
    }

    public boolean enabled() {
        return enabled;
    }

    /** Reads the ground track out of the walk clip. Cache the result; it walks every frame. */
    public ZombieFootfallCurve measureFootfall(PamPlayer player, ClipRef walkClip) {
        return ZombieFootfallCurve.measure(player, walkClip, trackParts);
    }

    /**
     * Walk-cycle phase in {@code [0, 1)} after covering {@code travelledTiles}, measured from
     * column 0 so cycles start on tile borders and centres. {@code travelledTiles}
     * is signed board travel (negative toward the house, positive away).
     */
    public float phaseAt(float travelledTiles) {
        float steps = travelledTiles / stepTiles;
        return steps - (float) Math.floor(steps);
    }

    /**
     * How far behind its linear model position the body should be drawn, in tiles. Zero at both
     * ends of a cycle, so this only reshapes motion within a step.
     */
    public float footLockOffsetTiles(float phase, ZombieFootfallCurve footfall) {
        return (phase - footfall.progressAt(phase)) * stepTiles;
    }
}
