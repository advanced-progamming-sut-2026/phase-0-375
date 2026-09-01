package view.gui.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Skin font accessors: Linear-filtered, custom-sized dynamic FreeType fonts, and thin dark-outlined wrappers.
 */
public final class SkinFonts {
    private static final Color OUTLINE_COLOR = new Color(0.08f, 0.06f, 0.04f, 1f);
    private static final float OUTLINE = 1f;

    private static final ObjectMap<String, BitmapFont> DYNAMIC_FONTS = new ObjectMap<>();

    private SkinFonts() {}

    public static final class FontSpec {
        public final String ttfPath;
        public final int baseSize;
        public final float borderWidth;
        public final Color borderColor;

        public FontSpec(String ttfPath, int baseSize, float borderWidth, Color borderColor) {
            this.ttfPath = ttfPath;
            this.baseSize = baseSize;
            this.borderWidth = borderWidth;
            this.borderColor = borderColor;
        }
    }

    public static FontSpec resolveFontSpec(String name) {
        if (name == null || name.isEmpty()) {
            return new FontSpec("skin/FBUSV8C5EI.TTF", 16, 0f, null);
        }
        return switch (name) {
            case "big", "FBUSV8C5EI_1" -> new FontSpec("skin/FBUSV8C5EI.TTF", 40, 0f, null);
            case "big_outline", "FBUSV8C5EI_1_outline" -> new FontSpec("skin/FBUSV8C5EI.TTF", 40, 3f, Color.BLACK);
            case "medium", "FBUSV8C5EI_2", "bundle_reward_multiplier" ->
                    new FontSpec("skin/FBUSV8C5EI.TTF", 24, 0f, null);
            case "medium_outline", "FBUSV8C5EI_2_outline" ->
                    new FontSpec("skin/FBUSV8C5EI.TTF", 30, 2f, Color.BLACK);
            case "secondary", "default", "FBUSV8C6EI_3" ->
                    new FontSpec("skin/FBUSV8C5EI.TTF", 16, 0f, null);
            case "HOUSE_OF_TERROR", "brown", "purple", "green", "green_small" ->
                    new FontSpec("skin/HOUSE OF TERROR.TTF", 22, 0f, null);
            case "BRIANNETOD" -> new FontSpec("skin/BRIANNETOD.TTF", 16, 0f, null);
            case "ASHLEYSCRIPTMTSTD" -> new FontSpec("skin/ASHLEYSCRIPTMTSTD.TTF", 16, 0f, null);
            case "AVENIRNEXTLTPRO-DEMICN" -> new FontSpec("skin/AVENIRNEXTLTPRO-DEMICN.TTF", 16, 0f, null);
            case "PICO12" -> new FontSpec("skin/PICO12__.TTF", 16, 0f, null);
            default -> new FontSpec("skin/FBUSV8C5EI.TTF", 24, 0f, null);
        };
    }

    public static BitmapFont get(String ttfPath, int size, float borderWidth, Color borderColor) {
        if (size <= 0) {
            size = 16;
        }
        String key = ttfPath + "_" + size + "_" + borderWidth + "_"
                + (borderColor != null ? borderColor.toIntBits() : 0);
        synchronized (DYNAMIC_FONTS) {
            BitmapFont cached = DYNAMIC_FONTS.get(key);
            if (cached != null) {
                return cached;
            }
            try {
                FileHandle handle = Gdx.files.classpath(ttfPath);
                if (handle.exists()) {
                    FreeTypeFontGenerator gen = new FreeTypeFontGenerator(handle);
                    FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
                    p.size = size;
                    p.color = Color.WHITE;
                    p.minFilter = TextureFilter.Linear;
                    p.magFilter = TextureFilter.Linear;
                    p.kerning = true;
                    if (borderWidth > 0f) {
                        p.borderWidth = borderWidth;
                        p.borderColor = borderColor != null ? borderColor : Color.BLACK;
                        p.borderStraight = false;
                    }
                    p.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                            + "¡¢£¤¥¦§¨©ª«¬®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞß"
                            + "àáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ•⌘‘’“”–—…™";
                    BitmapFont font = gen.generateFont(p);
                    gen.dispose();
                    font.setUseIntegerPositions(false);
                    DYNAMIC_FONTS.put(key, font);
                    return font;
                }
            } catch (Exception e) {
                Gdx.app.error("SkinFonts", "Failed to generate dynamic font for " + ttfPath + " @" + size, e);
            }
            return null;
        }
    }

    public static BitmapFont get(String fontOrStyleName, int size) {
        FontSpec spec = resolveFontSpec(fontOrStyleName);
        return get(spec.ttfPath, size, spec.borderWidth, spec.borderColor);
    }

    public static BitmapFont getScaled(Skin skin, String name, float scale) {
        FontSpec spec = resolveFontSpec(name);
        int targetSize = Math.max(1, Math.round(spec.baseSize * scale));
        BitmapFont font = get(spec.ttfPath, targetSize, spec.borderWidth, spec.borderColor);
        if (font != null) {
            return font;
        }
        return linear(skin, name);
    }

    public static void scaleLabel(Label label, Skin skin, String styleName, float scale) {
        if (label == null) {
            return;
        }
        BitmapFont font = getScaled(skin, styleName, scale);
        Label.LabelStyle baseStyle = skin != null && styleName != null && skin.has(styleName, Label.LabelStyle.class)
            ? skin.get(styleName, Label.LabelStyle.class)
            : label.getStyle();
        Label.LabelStyle newStyle = baseStyle != null ? new Label.LabelStyle(baseStyle) : new Label.LabelStyle();
        newStyle.font = font;
        label.setStyle(newStyle);
        label.setFontScale(1f);
    }

    public static void scaleButton(TextButton button, Skin skin, String styleName, float scale) {
        if (button == null) {
            return;
        }
        BitmapFont font = getScaled(skin, styleName != null ? styleName : "HOUSE_OF_TERROR", scale);
        TextButton.TextButtonStyle baseStyle = skin != null && styleName != null
                && skin.has(styleName, TextButton.TextButtonStyle.class)
            ? skin.get(styleName, TextButton.TextButtonStyle.class)
            : button.getStyle();
        TextButton.TextButtonStyle newStyle = baseStyle != null
                ? new TextButton.TextButtonStyle(baseStyle) : new TextButton.TextButtonStyle();
        newStyle.font = font;
        button.setStyle(newStyle);
        button.getLabel().setFontScale(1f);
    }

    public static void scaleCheckBox(CheckBox checkBox, Skin skin, String styleName, float scale) {
        if (checkBox == null) {
            return;
        }
        BitmapFont font = getScaled(skin, styleName != null ? styleName : "default", scale);
        CheckBox.CheckBoxStyle baseStyle = skin != null && styleName != null
                && skin.has(styleName, CheckBox.CheckBoxStyle.class)
            ? skin.get(styleName, CheckBox.CheckBoxStyle.class)
            : checkBox.getStyle();
        CheckBox.CheckBoxStyle newStyle = baseStyle != null
                ? new CheckBox.CheckBoxStyle(baseStyle) : new CheckBox.CheckBoxStyle();
        newStyle.font = font;
        checkBox.setStyle(newStyle);
        checkBox.getLabel().setFontScale(1f);
    }

    public static void disposeDynamicFonts() {
        synchronized (DYNAMIC_FONTS) {
            for (BitmapFont font : DYNAMIC_FONTS.values()) {
                if (font != null) {
                    font.dispose();
                }
            }
            DYNAMIC_FONTS.clear();
        }
    }

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
