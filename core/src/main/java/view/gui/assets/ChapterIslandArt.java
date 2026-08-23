package view.gui.assets;

import model.enums.Chapter;
import pvz.libpvz.textures.TextureBank;

/**
 * Adventure chapter thumbnails from {@code UI_Universe_768}.
 */
public final class ChapterIslandArt {
    public static final String ATLAS_UNIVERSE = "UI_Universe_768";
    public static final String ATLAS_LOCK = "UI_Universe_Lock_768";

    public static final String EGYPT = "IMAGE_UI_UNIVERSE_WORLDS_EGYPT";
    public static final String DARK = "IMAGE_UI_UNIVERSE_WORLDS_DARK";
    public static final String BEACH = "IMAGE_UI_UNIVERSE_WORLDS_BEACH";
    public static final String ICEAGE = "IMAGE_UI_UNIVERSE_WORLDS_ICEAGE";

    /** Padlock body (bottom). Draw first, then {@link #LOCK_SHACKLE} on top. */
    public static final String LOCK_BODY = "IMAGE_UI_UNIVERSE_WORLD_LOCK_WORLD_LOCK_205X188";
    /** Padlock shackle / tongue (top piece). */
    public static final String LOCK_SHACKLE = "IMAGE_UI_UNIVERSE_WORLD_LOCK_WORLD_LOCK_165X121";

    private boolean loaded;

    public void ensureLoaded(TextureBank textures) {
        if (loaded) {
            return;
        }
        textures.loadSync(ATLAS_UNIVERSE);
        textures.loadSync(ATLAS_LOCK);
        loaded = true;
    }

    public String imageId(Chapter chapter) {
        return switch (chapter) {
            case ANCIENT_EGYPT -> EGYPT;
            case DARK_AGES -> DARK;
            case BIG_WAVE_BEACH -> BEACH;
            case FROSTBITE_CAVES -> ICEAGE;
        };
    }
}
