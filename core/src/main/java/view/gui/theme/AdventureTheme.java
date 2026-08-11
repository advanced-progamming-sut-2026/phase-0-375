package view.gui.theme;

import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import model.enums.Chapter;

/**
 * Optional art hooks for adventure menus. Default returns null (text-only cards).
 * Later: wire TextureBank / atlas regions per chapter without changing screens.
 */
public final class AdventureTheme {
    private static AdventureTheme instance = new AdventureTheme();

    private AdventureTheme() {}

    public static AdventureTheme get() {
        return instance;
    }

    /** Package-visible for tests / future asset bootstrap. */
    public static void set(AdventureTheme theme) {
        instance = theme == null ? new AdventureTheme() : theme;
    }

    public Drawable chapterArt(Chapter chapter) {
        return null;
    }

    public Drawable levelArt(Chapter chapter, int levelId) {
        return null;
    }

    public Drawable plantIcon(String plantName) {
        return null;
    }
}
