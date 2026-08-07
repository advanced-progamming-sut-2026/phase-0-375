package view.gui.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * PvzSkin FreeType fonts ship with Nearest filtering; that looks blocky when the
 * FitViewport scales up to fullscreen. Switch fonts + atlas to Linear.
 */
public final class SkinSmoothing {
    private SkinSmoothing() {}

    public static void applyLinearFiltering(Skin skin) {
        if (skin == null) {
            return;
        }
        TextureAtlas atlas = skin.getAtlas();
        if (atlas != null) {
            for (Texture texture : atlas.getTextures()) {
                texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
            }
        }
        ObjectMap<String, BitmapFont> fonts = skin.getAll(BitmapFont.class);
        if (fonts == null) {
            return;
        }
        for (BitmapFont font : fonts.values()) {
            for (TextureRegion region : font.getRegions()) {
                region.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
            }
            font.setUseIntegerPositions(false);
        }
    }
}
