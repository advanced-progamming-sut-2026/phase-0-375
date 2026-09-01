package view.gui.audio;

/**
 * Short sound effects under {@code assets/music/SFXs/} (see folder names).
 */
public enum GameSfx {
    /** Menu navigation — new screen ({@code Audio_Always_Loaded.048}). */
    NAV_CLICK("SFXs/PVZ-click/Audio_Always_Loaded.048"),
    /** Modal / overlay opened ({@code Audio_Always_Loaded.092}). */
    OVERLAY_OPEN("SFXs/PVZ-click/Audio_Always_Loaded.092"),
    COLLECT_SUN("SFXs/PVZ-collectsun/Audio_Always_Loaded.051"),
    ERROR("SFXs/PVZ-error/Audio_Always_Loaded.344"),
    LOSE_GAME("SFXs/PVZ-losegame/Audio_Always_Loaded.318"),
    PAUSE("SFXs/PVZ-pause/Audio_Always_Loaded.048"),
    PLANT("SFXs/PVZ-plantaplant/Audio_Always_Loaded.204"),
    PLANT_ON_WATER("SFXs/PVZ-plantonwater/Audio_Always_Loaded.252"),
    GRAVE_BUSTER("SFXs/PVZ-gravebuster/Audio_Always_Loaded.381"),
    PLANT_FOOD("SFXs/PVZ-plantfood/Audio_Always_Loaded.011"),
    SHOVEL("SFXs/PVZ-shovel/Audio_Always_Loaded.022"),
    /** Level 1 NPC intro — "Rise and shine, Dr. Zarrabi." */
    RISE_AND_SHINE_DR_ZARRABI("SFXs/RiseaAndShineDrZarrabi"),
    /** Zomboss laugh when the boss spawns. */
    ZOMBOSS_SPAWN("SFXs/PVZ-zomboss/Audio_Always_Loaded.213");

    public final String relativePath;

    GameSfx(String relativePath) {
        this.relativePath = relativePath;
    }
}
