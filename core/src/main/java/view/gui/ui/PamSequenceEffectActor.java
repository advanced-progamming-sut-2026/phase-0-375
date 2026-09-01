package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import view.gui.anim.PamClipCache;

/** Plays an ordered list of PAM clips from one file; optional full-sequence loop. */
public final class PamSequenceEffectActor extends Actor {
    private final PamPlayer player;
    private final PamClipCache clips;
    private final String pamPath;
    private final String[] clipNames;
    private int clipIndex;
    private float clipTime;
    private float scale = 1f;
    private float offsetY;
    /** 0 = bottom of actor, 0.5 = center, 1 = top. */
    private float drawAnchorY = 0.5f;
    private boolean loopSequence;
    private Runnable onComplete;
    private boolean finished;

    public PamSequenceEffectActor(PamPlayer player, PamClipCache clips, String pamPath, String... clipNames) {
        this.player = player;
        this.clips = clips;
        this.pamPath = pamPath;
        this.clipNames = clipNames != null && clipNames.length > 0 ? clipNames : new String[] {"animation"};
        setTouchable(Touchable.disabled);
    }

    public PamSequenceEffectActor setEffectScale(float scale) {
        this.scale = scale;
        return this;
    }

    /** Negative values shift the draw anchor downward. */
    public PamSequenceEffectActor setOffsetY(float offsetY) {
        this.offsetY = offsetY;
        return this;
    }

    public PamSequenceEffectActor setDrawAnchorY(float fractionFromBottom) {
        this.drawAnchorY = Math.clamp(fractionFromBottom, 0f, 1f);
        return this;
    }

    public PamSequenceEffectActor setLoopSequence(boolean loopSequence) {
        this.loopSequence = loopSequence;
        return this;
    }

    public PamSequenceEffectActor onComplete(Runnable onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (finished) {
            return;
        }
        clipTime += Math.max(0f, delta);
        float duration = clipDuration(clipNames[clipIndex]);
        if (duration <= 0f || clipTime < duration) {
            return;
        }
        clipIndex++;
        clipTime = 0f;
        if (clipIndex < clipNames.length) {
            return;
        }
        if (loopSequence) {
            clipIndex = 0;
            return;
        }
        finished = true;
        if (onComplete != null) {
            onComplete.run();
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        String clip = clipNames[Math.min(clipIndex, clipNames.length - 1)];
        ClipRef ref = clips.getOrLoad(pamPath, clip);
        if (ref == null) {
            return;
        }
        float oldA = batch.getColor().a;
        float a = oldA * parentAlpha * getColor().a;
        batch.setColor(batch.getColor().r, batch.getColor().g, batch.getColor().b, a);
        player.draw(batch, ref, clipTime, getX() + getWidth() * 0.5f,
                getY() + getHeight() * drawAnchorY + offsetY, scale, scale, false);
        batch.setColor(batch.getColor().r, batch.getColor().g, batch.getColor().b, oldA);
    }

    private float clipDuration(String clip) {
        ClipRef ref = clips.getOrLoad(pamPath, clip);
        if (ref != null && ref.duration > 0f) {
            return ref.duration;
        }
        try {
            float seconds = player.clipDurationSeconds(pamPath, clip);
            if (seconds > 0f) {
                return seconds;
            }
        } catch (IllegalArgumentException ignored) {
            // PamClipCache may have already fallen back.
        }
        return 1.2f;
    }
}
