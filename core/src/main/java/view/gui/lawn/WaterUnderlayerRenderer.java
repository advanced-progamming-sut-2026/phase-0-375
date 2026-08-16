package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import model.game.core.GameModel;
import model.game.map.WaterBand;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import view.gui.anim.PamClipCache;
import view.gui.assets.PamCatalog;
import view.gui.assets.PvzAssets;

/**
 * Loops {@code WAVE_UPPERLAYER.PAM} as a fixed-size tide wall.
 *
 * <p>Scale is constant (clip height → world height). Left/right only slides X
 * so the <em>drawn</em> left edge sits on the water line — not the 390 canvas,
 * which this PAM overflows. Big Wave Beach should draw this after the lawn
 * background and before plants/zombies.
 */
public final class WaterUnderlayerRenderer {
    public static final String CATALOG_NAME = "WAVE_UPPERLAYER";
    public static final String PAM_PATH =
            "768/FULL/BACKGROUNDS/WAVE_UPPERLAYER/WAVE_UPPERLAYER.PAM";
    public static final String CLIP = "water";
    /** animations.json canvas; used only until clip bounds load. */
    public static final float CANVAS = 390f;
    /** Tall wave strip in the 768 atlas; the 390 canvas does not contain it. */
    public static final float WAVE_STRIP_HEIGHT = 1390f;
    /** One tile of water-line travel takes this many seconds. */
    public static final float SLIDE_SECONDS_PER_TILE = 0.28f;
    /** Foam sits on the tile centre in the PAM; pull left to the column edge. */
    public static final float LINE_INSET_TILES = 0.5f;

    private final PamPlayer player;
    private final PamClipCache clips;
    private final LawnLayout layout;
    private final String pamPath;
    private float animTime;
    private float drawX = Float.NaN;
    private Rectangle clipBox;

    public WaterUnderlayerRenderer(PvzAssets assets, LawnLayout layout) {
        this.player = assets.player;
        this.clips = new PamClipCache(assets.player);
        this.layout = layout;
        PamCatalog.PamEntry entry = assets.pamCatalog == null
                ? null
                : assets.pamCatalog.byName(CATALOG_NAME);
        this.pamPath = entry != null ? entry.path() : PAM_PATH;
        clips.preloadSync(pamPath, CLIP);
        refreshClipBox();
    }

    public void draw(Batch batch, GameModel model, float delta) {
        if (model == null || model.getMap() == null) {
            return;
        }
        draw(batch, WaterBand.columnsFromRight(model.getMap()), delta);
    }

    public void draw(Batch batch, int columnsFromRight, float delta) {
        refreshClipBox();
        int n = Math.max(0, Math.min(columnsFromRight, layout.cols()));
        float scale = scaleForHeight(clipHeight());
        float target = drawCenterX(layout, n, localLeft(), clipWidth(), scale);
        if (Float.isNaN(drawX)) {
            drawX = drawCenterX(layout, 0, localLeft(), clipWidth(), scale);
        }
        float dt = Math.max(0f, delta);
        drawX = moveToward(drawX, target, layout.cellWidth() / SLIDE_SECONDS_PER_TILE * dt);
        if (n <= 0 && drawX == target) {
            return;
        }
        animTime += dt;
        ClipRef ref = clips.getOrLoad(pamPath, CLIP);
        if (ref == null) {
            return;
        }
        player.draw(batch, ref, animTime, drawX, drawCenterY(scale), scale, scale, true);
    }

    /** Left edge of the flooded band (or past the lawn when {@code n == 0}). */
    public static float waterLineX(LawnLayout layout, int columnsFromRight) {
        int cols = layout.cols();
        int n = Math.max(0, Math.min(columnsFromRight, cols));
        if (n == 0) {
            return layout.cellLeft(cols);
        }
        return layout.cellLeft(cols - n) - LINE_INSET_TILES * layout.cellWidth();
    }

    /**
     * PamPlayer centre X so {@code localLeft} (clip bounds, canvas-centre space)
     * lands on the water line. {@code n == 0} parks the whole clip past the lawn.
     */
    public static float drawCenterX(LawnLayout layout, int columnsFromRight,
                                    float localLeft, float clipWidth, float scale) {
        float line = waterLineX(layout, columnsFromRight);
        if (columnsFromRight <= 0) {
            line += clipWidth * scale;
        }
        return line - localLeft * scale;
    }

    public static float scaleForHeight(float clipHeight) {
        float h = clipHeight > 1f ? clipHeight : CANVAS;
        return LawnLayout.WORLD_HEIGHT / h;
    }

    static float moveToward(float current, float target, float maxDelta) {
        float d = target - current;
        if (Math.abs(d) <= maxDelta) {
            return target;
        }
        return current + Math.signum(d) * maxDelta;
    }

    private void refreshClipBox() {
        if (clipBox != null) {
            return;
        }
        try {
            clipBox = player.bounds(pamPath, CLIP);
        } catch (RuntimeException ignored) {
            // still loading
        }
    }

    private float localLeft() {
        return clipBox != null ? clipBox.x : -CANVAS * 0.5f;
    }

    private float clipWidth() {
        return clipBox != null && clipBox.width > 1f ? clipBox.width : CANVAS;
    }

    private float clipHeight() {
        if (clipBox != null && clipBox.height > CANVAS + 8f) {
            return clipBox.height;
        }
        return WAVE_STRIP_HEIGHT;
    }

    /** World Y so the clip is centred on the 768 screen (PAM Y is down). */
    private float drawCenterY(float scale) {
        if (clipBox == null) {
            return LawnLayout.WORLD_HEIGHT * 0.5f;
        }
        float localCy = clipBox.y + clipBox.height * 0.5f;
        return LawnLayout.WORLD_HEIGHT * 0.5f + localCy * scale;
    }
}
