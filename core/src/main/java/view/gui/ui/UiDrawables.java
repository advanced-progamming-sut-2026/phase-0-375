package view.gui.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

final class UiDrawables {
    private UiDrawables() {}

    static Drawable tryDrawable(Skin skin, String name) {
        if (skin == null || name == null) {
            return null;
        }
        try {
            return skin.getDrawable(name);
        } catch (Exception e) {
            return null;
        }
    }

    /** TenPatch drawable PvzSkin registers as {@code name_10}. Stretches; do not scale the raw region. */
    static Drawable tenPatch(Skin skin, String atlasName) {
        return tryDrawable(skin, atlasName + "_10");
    }

    /** Atlas region, or the TenPatch {@code *_10} variant PvzSkin registers. */
    static Drawable tryNamed(Skin skin, String atlasName) {
        Drawable ten = tryDrawable(skin, atlasName + "_10");
        return ten != null ? ten : tryDrawable(skin, atlasName);
    }
}
