package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import model.game.core.GameModel;
import model.game.map.WaterBand;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
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

    /** Tide-limit marker ("water_tide_line"), not indexed by PamCatalog. */
    public static final String TIDE_LINE_PAM_PATH =
            "768/FULL/BACKGROUNDS/WATER_TIDE_LINE/WATER_TIDE_LINE.PAM";
    public static final String TIDE_LINE_CLIP = "idle";
    /** animations.json canvas fallback for the tide line strip. */
    public static final float TIDE_LINE_CANVAS_HEIGHT = 397f;
    /**
     * The tide line art's (0,0) axis sits ~45 canvas units right of the drawn
     * line itself, so the anchor is parked that far right of the limit.
     */
    public static final float TIDE_LINE_ORIGIN_OFFSET_X = 45f;

    /** Beach water-sign decal (raw atlas image, drawn over the water layer). */
    public static final String WATER_SIGN_IMAGE_ID = "IMAGE_BACKGROUNDS_BEACH_WATERSIGN";
    /** Draw size relative to the 146x263 atlas image. */
    public static final float WATER_SIGN_SCALE = 0.4f;
    /** Image point (from the image's bottom-left) pinned to the tide top. */
    public static final float WATER_SIGN_ANCHOR_X = 80f;
    public static final float WATER_SIGN_ANCHOR_Y = 77f;
    /** Plain pixel nudge on top of the anchor (world px, + right). */
    public static final float WATER_SIGN_OFFSET_X = 0f;
    /** Plain pixel nudge on top of the anchor (world px, + up). */
    public static final float WATER_SIGN_OFFSET_Y = 0f;
    /** Manual vertical nudge for the tide line strip (world px, Y-up). */
    public static final float TIDE_LINE_Y_OFFSET = 25f;

    private final PamPlayer player;
    private final PamClipCache clips;
    private final TextureBank textures;
    private final LawnLayout layout;
    private final String pamPath;
    private float animTime;
    private float tideLineTime;
    private float drawX = Float.NaN;
    private Rectangle clipBox;
    private Rectangle tideLineBox;

    public WaterUnderlayerRenderer(PvzAssets assets, LawnLayout layout) {
        this.player = assets.player;
        this.clips = new PamClipCache(assets.player);
        this.textures = assets.textures;
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
        drawTideLine(batch, model, Math.max(0f, delta));
        draw(batch, WaterBand.columnsFromRight(model.getMap()), delta);
        // Above the wave wall so the surf never paints over the sign.
        drawWaterSign(batch, model);
    }

    public void draw(Batch batch, int columnsFromRight, float delta) {
        refreshClipBox();
        int n = Math.max(0, Math.min(columnsFromRight, layout.cols()));
        float scale = scaleForHeight(clipHeight());
        float target = drawCenterX(layout, n, localLeft(), clipWidth(), scale);
        if (Float.isNaN(drawX)) {
            drawX = drawCenterX(layout, n, localLeft(), clipWidth(), scale);
        }
        float dt = Math.max(0f, delta);
        drawX = moveToward(drawX, target, layout.cellWidth() / SLIDE_SECONDS_PER_TILE * dt);
        if (n <= 0 && drawX == target) {
            return;
        }
        animTime += dt;
        paint(batch);
    }

    /** Current tide pose, no time/slide advance. Used to cover a drowning Fisherman. */
    public void paint(Batch batch) {
        if (Float.isNaN(drawX)) {
            return;
        }
        ClipRef ref = clips.getOrLoad(pamPath, CLIP);
        if (ref == null) {
            return;
        }
        float scale = scaleForHeight(clipHeight());
        player.draw(batch, ref, animTime, drawX, drawCenterY(scale), scale, scale, true);
    }

    /**
     * Draws the tide-limit marker on the left edge of the dynamic tide band —
     * the line the water can never cross. Visible even when the tide is fully
     * out; drawn beneath the wave underlayer so surf overlaps it.
     */
    private void drawTideLine(Batch batch, GameModel model, float dt) {
        int limit = Math.min(model.getTideLimitColumns(), layout.cols());
        if (limit <= 0) {
            return;
        }
        refreshTideLineBox();
        float clipHeight = tideLineBox != null && tideLineBox.height > 1f
                ? tideLineBox.height : TIDE_LINE_CANVAS_HEIGHT;
        // Span the strip over the full playable grid height.
        float scale = LawnLayout.GRID_HEIGHT / clipHeight;
        ClipRef ref = clips.getOrLoad(TIDE_LINE_PAM_PATH, TIDE_LINE_CLIP);
        if (ref == null) {
            return;
        }
        tideLineTime += dt;
        // The art's (0,0) point sits right of the drawn line itself, so park
        // the anchor offset-right of the limit for the line to land on it.
        float lineX = layout.cellLeft(layout.cols() - limit);
        float x = lineX + TIDE_LINE_ORIGIN_OFFSET_X * scale;
        float y = LawnLayout.LAWN_ORIGIN_Y + LawnLayout.GRID_HEIGHT * 0.5f
                + TIDE_LINE_Y_OFFSET;
        player.draw(batch, ref, tideLineTime, x, y, scale, scale, true);
    }

    /** World Y of the tide line strip's top (grid top + manual nudge). */
    private static float tideTopY() {
        return LawnLayout.LAWN_ORIGIN_Y + LawnLayout.GRID_HEIGHT + TIDE_LINE_Y_OFFSET;
    }

    /** @return the tide limit's x, or NaN when this level has no tide band. */
    private float tideLineX(GameModel model) {
        int limit = Math.min(model.getTideLimitColumns(), layout.cols());
        return limit <= 0 ? Float.NaN : layout.cellLeft(layout.cols() - limit);
    }

    /**
     * Beach water-sign decal planted at the top end of the tide line, drawn
     * at {@link #WATER_SIGN_SCALE} of its atlas size. Image point (28, 77),
     * counted from the image's bottom-left corner, lands exactly on the
     * line's top; {@code batch.draw} also anchors the image's bottom-left in
     * this Y-up world, so the draw position is simply the anchor target
     * minus the (scaled) anchor offset.
     */
    private void drawWaterSign(Batch batch, GameModel model) {
        float lineX = tideLineX(model);
        if (Float.isNaN(lineX)) {
            return;
        }
        TextureRegion sign = textures.region(WATER_SIGN_IMAGE_ID);
        if (sign == null) {
            return;
        }
        float s = WATER_SIGN_SCALE;
        batch.draw(sign,
                lineX - WATER_SIGN_ANCHOR_X * s + WATER_SIGN_OFFSET_X,
                tideTopY() - WATER_SIGN_ANCHOR_Y * s + WATER_SIGN_OFFSET_Y,
                sign.getRegionWidth() * s,
                sign.getRegionHeight() * s);
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

    private void refreshTideLineBox() {
        if (tideLineBox != null) {
            return;
        }
        try {
            tideLineBox = player.bounds(TIDE_LINE_PAM_PATH, TIDE_LINE_CLIP);
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
