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

import java.util.Objects;

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

    /**
     * Bind an idle PAM. Same plant + clip keeps {@code time} so the loop does not
     * restart when the greenhouse refreshes the pot timer each second.
     */
    public void setPlant(String plantType, boolean ready) {
        drawScale = ready ? SCALE_READY : SCALE_GROWING;
        if (plantType == null || plantType.isBlank()) {
            clearPlant();
            return;
        }
        PamCatalog.PamEntry entry = catalog.forPlant(plantType);
        if (entry == null) {
            clearPlant();
            return;
        }
        String nextPath = entry.path();
        String nextClip = catalog.resolveClip(entry, "idle", "idle2", "idle1", "loop", "animation");
        boolean sameClip = hasPlant
            && Objects.equals(pamPath, nextPath)
            && Objects.equals(clipName, nextClip);
        pamPath = nextPath;
        clipName = nextClip;
        hasPlant = pamPath != null && clipName != null;
        if (!sameClip) {
            time = 0f;
        }
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
        float cy = getY() + getHeight() * 0.7f;
        player.draw(batch, ref, time, cx, cy, drawScale, drawScale, true);
        batch.setColor(batch.getColor().r, batch.getColor().g, batch.getColor().b, oldA);
    }
}
