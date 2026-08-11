package view.gui.lawn;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import pvz.libpvz.textures.TextureBank;

/**
 * Draws FrontLawn background layers via libPVZ {@link TextureBank}.
 */
public final class LawnBackgroundRenderer {
    public static final String ATLAS_GROUP = "DelayLoad_Background_FrontLawn";
    public static final String TEXTURE_ID = "IMAGE_BACKGROUNDS_FRONTLAWN_TEXTURE";
    public static final String TEXTURE_LEFT_ID = "IMAGE_BACKGROUNDS_FRONTLAWN_TEXTURE_LEFT";
    public static final String TEXTURE_RIGHT_ID = "IMAGE_BACKGROUNDS_FRONTLAWN_TEXTURE_RIGHT";
    public static final String ROW05_ID = "IMAGE_BACKGROUNDS_FRONTLAWN_ROW_05";

    private final TextureBank textures;
    private boolean atlasRequested;

    public LawnBackgroundRenderer(TextureBank textures) {
        this.textures = textures;
    }

    public void ensureLoaded() {
        if (!atlasRequested) {
            textures.loadSync(ATLAS_GROUP);
            atlasRequested = true;
        }
    }

    public void draw(Batch batch) {
        ensureLoaded();

        TextureRegion left = textures.region(TEXTURE_LEFT_ID);
        if (left != null) {
            batch.draw(
                    left,
                    LawnLayout.TEXTURE_LEFT_X,
                    0f,
                    LawnLayout.TEXTURE_LEFT_WIDTH,
                    LawnLayout.TEXTURE_HEIGHT);
        }

        TextureRegion base = textures.region(TEXTURE_ID);
        if (base != null) {
            batch.draw(
                    base,
                    LawnLayout.TEXTURE_ORIGIN_X,
                    0f,
                    LawnLayout.TEXTURE_WIDTH,
                    LawnLayout.TEXTURE_HEIGHT);
        }

        TextureRegion right = textures.region(TEXTURE_RIGHT_ID);
        if (right != null) {
            batch.draw(
                    right,
                    LawnLayout.TEXTURE_RIGHT_X,
                    0f,
                    LawnLayout.TEXTURE_RIGHT_WIDTH,
                    LawnLayout.TEXTURE_HEIGHT);
        }

        TextureRegion row05 = textures.region(ROW05_ID);
        if (row05 != null) {
            batch.draw(
                    row05,
                    LawnLayout.ROW05_DRAW_X,
                    LawnLayout.ROW05_DRAW_Y,
                    LawnLayout.ROW05_WIDTH,
                    LawnLayout.ROW05_HEIGHT);
        }
    }
}
