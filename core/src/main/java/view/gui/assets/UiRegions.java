package view.gui.assets;

/**
 * Stable {@code IMAGE_*} ids for menu chrome via libPVZ {@link pvz.libpvz.textures.TextureBank}.
 */
public final class UiRegions {
    public static final String ATLAS_MAIN_MENU_BACKGROUND = "MainMenu_Background_768";
    public static final String ATLAS_UI_MAIN_MENU = "UI_MainMenu_768";
    public static final String ATLAS_UI_MAIN_MENU_LOGO = "UI_MainMenuLogo_768";
    public static final String ATLAS_UI_ALWAYS_LOADED = "UI_AlwaysLoaded_768";

    public static final String MAIN_MENU_BACKGROUND = "IMAGE_MAINMENU_BACKGROUND";
    public static final String LOGO = "IMAGE_UI_MAINMENU_PVZ2_LOGO_HORIZONTAL";
    public static final String NEWS_ICON = "IMAGE_UI_MAINMENU_MM_NEWSICON";
    public static final String SETTINGS_ICON = "IMAGE_UI_MAINMENU_MM_SETTINGS";
    public static final String PROFILE_ICON = "IMAGE_UI_MAINMENU_MM_PLAYERICON";
    public static final String COIN_ICON = "IMAGE_UI_HUD_INGAME_COIN";
    public static final String GEM_ICON = "IMAGE_UI_HUD_INGAME_GEM";

    /** Debug free-currency buttons (world-map HUD). {@code _2} = pressed. */
    public static final String ATLAS_WORLD_MAP = "UI_WorldMap_768";
    public static final String FREE_COINS_UP =
            "IMAGE_UI_HUD_WORLDMAP_FREE_COINS_BUTTON_FREE_COINS_BUTTON_300X130";
    public static final String FREE_COINS_DOWN =
            "IMAGE_UI_HUD_WORLDMAP_FREE_COINS_BUTTON_FREE_COINS_BUTTON_300X130_2";
    public static final String FREE_COINS_GOLDEN_UP =
            "IMAGE_UI_HUD_WORLDMAP_FREE_COINS_BUTTON_GOLDEN_FREE_COINS_BUTTON_GOLDEN_300X130";
    public static final String FREE_COINS_GOLDEN_DOWN =
            "IMAGE_UI_HUD_WORLDMAP_FREE_COINS_BUTTON_GOLDEN_FREE_COINS_BUTTON_GOLDEN_300X130_2";

    private UiRegions() {}
}
