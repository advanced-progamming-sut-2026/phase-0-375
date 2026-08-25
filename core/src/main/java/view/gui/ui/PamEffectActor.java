package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import view.gui.anim.PamClipCache;

/** Reusable looping PAM overlay, suitable for store promos and greenhouse boosts. */
public final class PamEffectActor extends Actor {
    private final PamPlayer player;
    private final PamClipCache clips;
    private final String pamPath;
    private final String clipName;
    private float time;
    private float scale = 1f;

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

    @Override public void act(float delta) {
        super.act(delta);
        time += Math.max(0f, delta);
    }

    @Override public void draw(Batch batch, float parentAlpha) {
        ClipRef ref = clips.getOrLoad(pamPath, clipName);
        if (ref == null) return;
        float oldA = batch.getColor().a;
        float a = oldA * parentAlpha * getColor().a;
        batch.setColor(batch.getColor().r, batch.getColor().g, batch.getColor().b, a);
        player.draw(batch, ref, time, getX() + getWidth() * .5f,
            getY() + getHeight() * .5f, scale, scale, true);
        batch.setColor(batch.getColor().r, batch.getColor().g, batch.getColor().b, oldA);
    }
}
