package view.gui.anim.zombie;

import com.badlogic.gdx.math.Rectangle;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;

/**
 * How far the body has moved through a walk cycle, measured from the art itself.
 *
 * <p>A PAM keeps the body on the animation origin and slides {@code ground_swatch} with the planted
 * contact. Sampling that locator every frame gives {@link view.gui.lawn.LawnEntityRenderer} the
 * curve it needs to hold the swatch still.
 */
public final class ZombieFootfallCurve {
    /** Even progress across the cycle, i.e. no foot lock. Used when the feet cannot be measured. */
    public static final ZombieFootfallCurve LINEAR = new ZombieFootfallCurve(null);

    /** Cumulative progress per frame, normalised to end at 1; null means linear. */
    private final float[] progress;

    private ZombieFootfallCurve(float[] progress) {
        this.progress = progress;
    }

    /**
     * Measures {@code trackParts} across every frame of {@code clip}. Several names are merged into
     * one track (a split locator still yields a single curve).
     *
     * <p>Falls back to {@link #LINEAR} whenever the track cannot be followed — a missing part, a
     * frame where it draws nothing, or a clip whose swatch never moves.
     */
    public static ZombieFootfallCurve measure(PamPlayer player, ClipRef clip, String[] trackParts) {
        if (player == null || clip == null || trackParts == null || trackParts.length == 0) {
            return LINEAR;
        }
        Rectangle[][] byPart = new Rectangle[trackParts.length][];
        int frames = 0;
        for (int p = 0; p < trackParts.length; p++) {
            byPart[p] = player.partBoundsByFrame(clip, trackParts[p]);
            frames = Math.max(frames, byPart[p].length);
        }
        if (frames < 2) {
            return LINEAR;
        }

        float[] x = new float[frames];
        for (int i = 0; i < frames; i++) {
            float min = Float.MAX_VALUE;
            float max = -Float.MAX_VALUE;
            for (Rectangle[] part : byPart) {
                Rectangle r = i < part.length ? part[i] : null;
                if (r == null) {
                    continue;
                }
                min = Math.min(min, r.x);
                max = Math.max(max, r.x + r.width);
            }
            if (min > max) {
                return LINEAR;
            }
            x[i] = (min + max) * 0.5f;
        }

        // The swatch spends most of the cycle planted and snaps back through the air, so the
        // direction it moves in on more frames is the stance. Air frames go backwards and clamp
        // to zero, which also keeps progress from ever running backwards.
        float[] step = new float[frames];
        int forward = 0;
        int backward = 0;
        for (int i = 0; i < frames; i++) {
            step[i] = x[(i + 1) % frames] - x[i];
            if (step[i] > 0f) {
                forward++;
            } else if (step[i] < 0f) {
                backward++;
            }
        }
        float stance = forward >= backward ? 1f : -1f;

        float[] progress = new float[frames + 1];
        for (int i = 0; i < frames; i++) {
            progress[i + 1] = progress[i] + Math.max(0f, stance * step[i]);
        }
        float stride = progress[frames];
        if (stride <= 0f) {
            return LINEAR;
        }
        for (int i = 1; i < frames; i++) {
            progress[i] /= stride;
        }
        progress[frames] = 1f;
        return new ZombieFootfallCurve(progress);
    }

    /**
     * Fraction of the step the body has completed at {@code phase}. Always 0 at phase 0 and 1 at
     * phase 1, so travel per cycle matches the model and the sprite never drifts.
     */
    public float progressAt(float phase) {
        if (phase <= 0f) {
            return 0f;
        }
        if (phase >= 1f) {
            return 1f;
        }
        if (progress == null) {
            return phase;
        }
        int frames = progress.length - 1;
        float at = phase * frames;
        int frame = (int) at;
        if (frame >= frames) {
            return 1f;
        }
        return progress[frame] + (progress[frame + 1] - progress[frame]) * (at - frame);
    }
}
