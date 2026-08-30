package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import view.gui.anim.PamClipCache;

/** Reusable PAM overlay for store promos, quest badges, and one-shot FX. */
public final class PamEffectActor extends Actor {
    private final PamPlayer player;
    private final PamClipCache clips;
    private final String pamPath;
    private final String clipName;
    private float time;
    private float scale = 1f;
    private boolean looping = true;
    private boolean autoRemove;
    private Runnable onComplete;
    private float duration = -1f;
    private boolean finished;

    public PamEffectActor(PamPlayer player, PamClipCache clips, String pamPath, String clipName) {
        this.player = player;
        this.clips = clips;
        this.pamPath = pamPath;
        this.clipName = clipName;
        setTouchable(Touchable.disabled);
    }

    public PamEffectActor setEffectScale(float scale) {
        this.scale = scale;
        return this;
    }

    public PamEffectActor setLooping(boolean looping) {
        this.looping = looping;
        return this;
    }

    public PamEffectActor setAutoRemove(boolean autoRemove) {
        this.autoRemove = autoRemove;
        return this;
    }

    public PamEffectActor onComplete(Runnable onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    /** Caps playback length (seconds). Useful for brief one-shot flashes. */
    public PamEffectActor setPlayTime(float seconds) {
        if (seconds > 0f) {
            this.duration = seconds;
        }
        return this;
    }

    /** Skips into the clip before the first draw (seconds). */
    public PamEffectActor setStartTime(float seconds) {
        this.time = Math.max(0f, seconds);
        return this;
    }

    @Override public void act(float delta) {
        super.act(delta);
        time += Math.max(0f, delta);
        if (looping || finished) {
            return;
        }
        float d = resolveDuration();
        if (d > 0f && time >= d) {
            finished = true;
            if (onComplete != null) {
                onComplete.run();
            }
            if (autoRemove) {
                remove();
            }
        }
    }

    @Override public void draw(Batch batch, float parentAlpha) {
        ClipRef ref = clips.getOrLoad(pamPath, clipName);
        if (ref == null) {
            return;
        }
        if (duration < 0f) {
            duration = resolveDuration(ref);
        }
        Color old = batch.getColor();
        float oldR = old.r;
        float oldG = old.g;
        float oldB = old.b;
        float oldA = old.a;
        Color actor = getColor();
        batch.setColor(actor.r, actor.g, actor.b, oldA * parentAlpha * actor.a);
        player.draw(batch, ref, time, getX() + getWidth() * .5f,
            getY() + getHeight() * .5f, scale, scale, looping);
        batch.setColor(oldR, oldG, oldB, oldA);
    }

    private float resolveDuration() {
        if (duration >= 0f) {
            return duration;
        }
        ClipRef ref = clips.getOrLoad(pamPath, clipName);
        duration = resolveDuration(ref);
        return duration;
    }

    private float resolveDuration(ClipRef ref) {
        if (ref != null && ref.duration > 0f) {
            return ref.duration;
        }
        try {
            float seconds = player.clipDurationSeconds(pamPath, clipName);
            if (seconds > 0f) {
                return seconds;
            }
        } catch (IllegalArgumentException ignored) {
            // Requested clip name may not exist; PamClipCache already fell back.
        }
        return 1.2f;
    }
}
