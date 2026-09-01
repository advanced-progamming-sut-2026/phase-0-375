package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import model.enums.Chapter;
import pvz.libpvz.textures.TextureBank;

/**
 * Draws chapter lawn backgrounds via libPVZ {@link TextureBank}.
 *
 * <p>Left + center fill the camera; {@code TEXTURE_RIGHT} is drawn past the
 * right edge and only peeks in on wide windows — same layout as debug FrontLawn.
 */
public final class LawnBackgroundRenderer {
    public record Style(
            String atlasGroup,
            String centerId,
            String leftId,
            String rightId,
            String row05Id) {

        public static final Style FRONT_LAWN = new Style(
                "DelayLoad_Background_FrontLawn",
                "IMAGE_BACKGROUNDS_FRONTLAWN_TEXTURE",
                "IMAGE_BACKGROUNDS_FRONTLAWN_TEXTURE_LEFT",
                "IMAGE_BACKGROUNDS_FRONTLAWN_TEXTURE_RIGHT",
                "IMAGE_BACKGROUNDS_FRONTLAWN_ROW_05");

        public static final Style EGYPT = new Style(
                "DelayLoad_Background_Egypt_Compressed",
                "IMAGE_BACKGROUNDS_EGYPT_TEXTURE",
                "IMAGE_BACKGROUNDS_EGYPT_TEXTURE_LEFT",
                "IMAGE_BACKGROUNDS_EGYPT_TEXTURE_RIGHT",
                null);

        public static final Style ICE_AGE = new Style(
                "DelayLoad_Background_Iceage_Compressed",
                "IMAGE_BACKGROUNDS_ICEAGE_TEXTURE",
                "IMAGE_BACKGROUNDS_ICEAGE_TEXTURE_LEFT",
                "IMAGE_BACKGROUNDS_ICEAGE_TEXTURE_RIGHT",
                null);

        public static final Style BEACH = new Style(
                "DelayLoad_Background_Beach_Compressed",
                "IMAGE_BACKGROUNDS_BEACH_TEXTURE",
                "IMAGE_BACKGROUNDS_BEACH_TEXTURE_LEFT",
                "IMAGE_BACKGROUNDS_BEACH_TEXTURE_RIGHT",
                null);

        public static final Style DARK = new Style(
                "DelayLoad_Background_Dark_Compressed",
                "IMAGE_BACKGROUNDS_DARK_TEXTURE",
                "IMAGE_BACKGROUNDS_DARK_TEXTURE_LEFT",
                "IMAGE_BACKGROUNDS_DARK_TEXTURE_RIGHT",
                null);

        public static Style forChapter(Chapter chapter) {
            if (chapter == null) {
                return FRONT_LAWN;
            }
            return switch (chapter) {
                case ANCIENT_EGYPT -> EGYPT;
                case FROSTBITE_CAVES -> ICE_AGE;
                case BIG_WAVE_BEACH -> BEACH;
                case DARK_AGES -> DARK;
            };
        }
    }

    /** FrontLawn ids kept for callers that still name them directly. */
    public static final String ATLAS_GROUP = Style.FRONT_LAWN.atlasGroup();
    public static final String TEXTURE_ID = Style.FRONT_LAWN.centerId();
    public static final String TEXTURE_LEFT_ID = Style.FRONT_LAWN.leftId();
    public static final String TEXTURE_RIGHT_ID = Style.FRONT_LAWN.rightId();
    public static final String ROW05_ID = Style.FRONT_LAWN.row05Id();

    private final TextureBank textures;
    private final Style style;
    private boolean atlasRequested;

    public LawnBackgroundRenderer(TextureBank textures) {
        this(textures, Style.FRONT_LAWN);
    }

    public LawnBackgroundRenderer(TextureBank textures, Style style) {
        this.textures = textures;
        this.style = style;
    }

    public void ensureLoaded() {
        if (!atlasRequested) {
            textures.loadSync(style.atlasGroup());
            atlasRequested = true;
        }
    }

    public void draw(Batch batch) {
        ensureLoaded();
        drawRegion(batch, style.leftId(), LawnLayout.TEXTURE_LEFT_X, 0f,
                LawnLayout.TEXTURE_LEFT_WIDTH, LawnLayout.TEXTURE_HEIGHT);
        drawCenter(batch);
        drawRegion(batch, style.rightId(), LawnLayout.TEXTURE_RIGHT_X, 0f,
                LawnLayout.TEXTURE_RIGHT_WIDTH, LawnLayout.TEXTURE_HEIGHT);
        if (style.row05Id() != null) {
            drawRegion(batch, style.row05Id(), LawnLayout.ROW05_DRAW_X, LawnLayout.ROW05_DRAW_Y,
                    LawnLayout.ROW05_WIDTH, LawnLayout.ROW05_HEIGHT);
        }
    }

    private void drawCenter(Batch batch) {
        TextureRegion base = textures.region(style.centerId());
        if (base == null) {
            return;
        }
        float h = LawnLayout.TEXTURE_HEIGHT;
        float y = 0f;
        if (Style.ICE_AGE.equals(style)) {
            h = base.getRegionHeight();
            y = LawnLayout.WORLD_HEIGHT - h;
        }
        batch.draw(base, LawnLayout.TEXTURE_ORIGIN_X, y, LawnLayout.TEXTURE_WIDTH, h);
    }

    private void drawRegion(Batch batch, String id, float x, float y, float w, float h) {
        TextureRegion region = textures.region(id);
        if (region != null) {
            batch.draw(region, x, y, w, h);
        }
    }
}
