package view.gui.assets;

import pvz.libpvz.textures.TextureBank;

/**
 * Zen Garden / greenhouse atlas regions (libPVZ {@code IMAGE_*} ids).
 */
public final class ZenGardenArt {
    public static final String ATLAS_BACKGROUND = "DelayLoad_Background_Zen_768";
    public static final String ATLAS_GROUP = "ZenGardenGroup_768";

    public static final String BACKGROUND = "IMAGE_BACKGROUNDS_ZEN_GARDEN";
    public static final String SLOT = "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161";
    /** Empty-pot plant target (768 atlas crop 78×103). */
    public static final String EMPTY_SLOT = "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_122X161";
    public static final String LOCKED_ICON = "IMAGE_ZEN_GARDEN_LOCKED_POT_ICON";
    public static final String SPROUT = "IMAGE_ZEN_GARDEN_PLANT_ANIMATIONS_SPROUT_SPROUT_39X25";
    /** 768 native 100×36. */
    public static final String TIMER_BG = "IMAGE_ZEN_GARDEN_FINISH_TIMER_BACKGROUND";
    /** 768 native 165×74 — gem instant-grow. */
    public static final String UNLOCK_ACTIVE = "IMAGE_ZEN_GARDEN_BUTTON_UNLOCK_ACTIVE";
    public static final String UNLOCK_INACTIVE = "IMAGE_ZEN_GARDEN_BUTTON_UNLOCK_INACTIVE";
    public static final String HIGHLIGHT = "IMAGE_ZEN_GARDEN_HIGHLIGHT_HIGHLIGHT_211X211";
    public static final String GEM = "IMAGE_ZEN_GARDEN_GEM_LARGE";

    private boolean loaded;

    public void ensureLoaded(TextureBank textures) {
        if (loaded) {
            return;
        }
        textures.loadSync(ATLAS_BACKGROUND);
        textures.loadSync(ATLAS_GROUP);
        loaded = true;
    }

    private ZenGardenArt() {}

    public static ZenGardenArt create() {
        return new ZenGardenArt();
    }
}
