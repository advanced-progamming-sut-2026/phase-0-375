package view.gui.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

final class UiDrawables {
    private UiDrawables() {}

    static Drawable tryDrawable(Skin skin, String name) {
        try {
            return skin.getDrawable(name);
        } catch (Exception e) {
            return null;
        }
    }
}
