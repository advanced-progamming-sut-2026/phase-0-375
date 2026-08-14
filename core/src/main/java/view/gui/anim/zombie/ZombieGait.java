package view.gui.anim.zombie;

import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;

/**
 * How a zombie's walk cycle maps onto board travel: {@link view.gui.lawn.LawnEntityRenderer}
 * drives the clip by distance so one cycle covers exactly {@link #stepTiles()}, then offsets the
 * sprite by the measured {@link ZombieFootfallCurve} to keep the planted foot still. Per-zombie
 * values live in {@link ZombieGaitProfiles}; owned by the zombie team.
 */
public final class ZombieGait {
    /** The basic Egyptian rig plants its inner foot, split into heel and toe parts. */
    private static final String[] EGYPT_INNER_FOOT = {
            "zombie_egypt_foot_inner_heel",
            "zombie_egypt_foot_inner_toe_01",
    };

    /**
     * Basic Egyptian zombie: two steps per tile. The art's own stride is a little shorter, so the
     * foot creeps to make cycles line up with the board — worth the trade.
     */
    public static final ZombieGait DEFAULT = new ZombieGait(0.5f, EGYPT_INNER_FOOT, true);

    private final float stepTiles;
    private final String[] footParts;
    private final boolean enabled;

    private ZombieGait(float stepTiles, String[] footParts, boolean enabled) {
        this.stepTiles = stepTiles > 0f ? stepTiles : 0.5f;
        this.footParts = footParts;
        this.enabled = enabled;
    }

    /**
     * A zombie that covers {@code stepTiles} per walk cycle and plants {@code footParts}. Name
     * every part of the one foot that stays on the ground; other rigs split it differently.
     */
    public static ZombieGait of(float stepTiles, String... footParts) {
        return new ZombieGait(stepTiles, footParts, true);
    }

    /** Opts a zombie out entirely, back onto wall-clock playback (swimmers, fliers). */
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

    /** Reads the planted foot out of the walk clip. Cache the result; it walks every frame. */
    public ZombieFootfallCurve measureFootfall(PamPlayer player, ClipRef walkClip) {
        return ZombieFootfallCurve.measure(player, walkClip, footParts);
    }

    /**
     * Walk-cycle phase in {@code [0, 1)} after covering {@code travelledTiles}, measured from
     * column 0 so cycles start on tile borders and centres. The caller measures travel along
     * the direction the zombie faces, so hypnotized zombies still walk forwards.
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
