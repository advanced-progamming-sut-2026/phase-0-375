package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;

/**
 * Skin font accessors: Linear-filtered, and a thin dark-outlined wrapper.
 */
public final class SkinFonts {
    private static final Color OUTLINE_COLOR = new Color(0.08f, 0.06f, 0.04f, 1f);
    private static final float OUTLINE = 1f;

    private SkinFonts() {}

    public static BitmapFont linear(Skin skin, String name) {
        return linear(resolve(skin, name));
    }

    public static BitmapFont linear(BitmapFont font) {
        if (font == null) {
            return null;
        }
        for (TextureRegion region : font.getRegions()) {
            region.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
        }
        font.setUseIntegerPositions(false);
        return font;
    }

    public static BitmapFont outlined(Skin skin, String name) {
        return outlined(linear(skin, name));
    }

    public static BitmapFont outlined(BitmapFont font) {
        if (font == null) {
            return null;
        }
        if (font instanceof OutlinedFont) {
            return linear(font);
        }
        return new OutlinedFont(linear(font));
    }

    static BitmapFont resolve(Skin skin, String name) {
        if (skin.has(name, BitmapFont.class)) {
            return skin.get(name, BitmapFont.class);
        }
        if (skin.has(name, Label.LabelStyle.class)) {
            return skin.get(name, Label.LabelStyle.class).font;
        }
        return skin.get(name, BitmapFont.class);
    }

    static final class OutlinedFont extends BitmapFont {
        OutlinedFont(BitmapFont src) {
            super(src.getData(), new Array<>(src.getRegions()), false);
            setUseIntegerPositions(false);
        }

        @Override
        public BitmapFontCache newFontCache() {
            return new OutlinedCache(this);
        }
    }

    private static final class OutlinedCache extends BitmapFontCache {
        private final Color pendingTint = new Color(1f, 1f, 1f, 1f);

        OutlinedCache(OutlinedFont font) {
            super(font, font.usesIntegerPositions());
        }

        @Override
        public void tint(Color tint) {
            pendingTint.set(tint);
        }

        @Override
        public void draw(Batch batch) {
            float x = getX();
            float y = getY();
            Color outline = OUTLINE_COLOR;
            setColors(outline.r, outline.g, outline.b, outline.a * pendingTint.a);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    setPosition(x + dx * OUTLINE, y + dy * OUTLINE);
                    super.draw(batch);
                }
            }
            setPosition(x, y);
            setColors(pendingTint);
            super.draw(batch);
        }
    }
}
