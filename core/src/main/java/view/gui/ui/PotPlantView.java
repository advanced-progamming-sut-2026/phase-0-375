package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import view.gui.anim.AnimScale;
import view.gui.anim.PamClipCache;
import view.gui.assets.PamCatalog;
import view.gui.assets.PvzAssets;

/**
 * Idle PAM plant drawn inside a greenhouse pot (Scene2D).
 */
public final class PotPlantView extends Actor {
    /** Ready plant scale relative to lawn {@link AnimScale#PLANT}. */
    public static final float SCALE_READY = AnimScale.PLANT * 1.4f;
    /** Growing plant — a bit smaller. */
    public static final float SCALE_GROWING = AnimScale.PLANT * 1.1f;

    private final PamPlayer player;
    private final PamCatalog catalog;
    private final PamClipCache clips;

    private String pamPath;
    private String clipName;
    private float drawScale = SCALE_READY;
    private float time;
    private boolean hasPlant;

    public PotPlantView(PvzAssets assets, PamClipCache clips) {
        this.player = assets.player;
        this.catalog = assets.pamCatalog;
        this.clips = clips;
        setTouchable(Touchable.disabled);
    }

    public void setPlant(String plantType, boolean ready) {
        pamPath = null;
        clipName = null;
        hasPlant = false;
        time = 0f;
        drawScale = ready ? SCALE_READY : SCALE_GROWING;
        if (plantType == null || plantType.isBlank()) {
            return;
        }
        PamCatalog.PamEntry entry = catalog.forPlant(plantType);
        if (entry == null) {
            return;
        }
        pamPath = entry.path();
        clipName = catalog.resolveClip(entry, "idle", "idle2", "idle1", "loop", "animation");
        hasPlant = pamPath != null && clipName != null;
    }

    public void clearPlant() {
        pamPath = null;
        clipName = null;
        hasPlant = false;
        time = 0f;
    }

    /** {@code true} once a PAM path resolved (may still be loading frames). */
    public boolean hasPlant() {
        return hasPlant;
    }

    /** {@code true} when the idle clip is ready to draw this frame. */
    public boolean isClipReady() {
        if (!hasPlant) {
            return false;
        }
        return clips.getOrLoad(pamPath, clipName) != null;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (hasPlant) {
            time += delta;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!hasPlant) {
            return;
        }
        ClipRef ref = clips.getOrLoad(pamPath, clipName);
        if (ref == null) {
            return;
        }
        float oldA = batch.getColor().a;
        batch.setColor(batch.getColor().r, batch.getColor().g, batch.getColor().b, oldA * parentAlpha);
        // Feet sit in the soil: center X, low in the pot cell.
        float cx = getX() + getWidth() * 0.5f;
        float cy = getY() + getHeight() * 0.38f;
        player.draw(batch, ref, time, cx, cy, drawScale, drawScale, true);
        batch.setColor(batch.getColor().r, batch.getColor().g, batch.getColor().b, oldA);
    }
}
