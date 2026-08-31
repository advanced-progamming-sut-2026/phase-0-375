package view.gui.audio;

import model.enums.Chapter;

/**
 * Stable music ids mapped to files under {@code assets/music/}.
 * See {@code assets/music/README.md} for Khinsider track names.
 */
public enum MusicTracks {
    TITLE("menu/title"),
    HUB("menu/hub"),
    WORLD_MAP("menu/world_map"),
    ZEN_GARDEN("menu/zen_garden"),
    REWARD_STING("menu/reward_sting"),

    EGYPT_CHOOSE("egypt/choose_seeds"),
    EGYPT_WAVE_FIRST("egypt/wave_first"),
    EGYPT_WAVE_MID("egypt/wave_mid"),
    EGYPT_WAVE_FINAL("egypt/wave_final"),
    EGYPT_VICTORY("egypt/victory"),
    EGYPT_DEFEAT("egypt/defeat"),
    EGYPT_REWARD("egypt/reward"),

    FROSTBITE_CHOOSE("frostbite/choose_seeds"),
    FROSTBITE_WAVE_FIRST("frostbite/wave_first"),
    FROSTBITE_WAVE_MID_A("frostbite/wave_mid_a"),
    FROSTBITE_WAVE_MID_B("frostbite/wave_mid_b"),
    FROSTBITE_WAVE_FINAL("frostbite/wave_final"),
    FROSTBITE_VICTORY("frostbite/victory"),
    FROSTBITE_DEFEAT("frostbite/defeat"),
    FROSTBITE_REWARD("frostbite/reward"),

    BEACH_CHOOSE("beach/choose_seeds"),
    BEACH_WAVE_FIRST("beach/wave_first"),
    BEACH_WAVE_MID_A("beach/wave_mid_a"),
    BEACH_WAVE_MID_B("beach/wave_mid_b"),
    BEACH_WAVE_FINAL("beach/wave_final"),
    BEACH_VICTORY("beach/victory"),
    BEACH_DEFEAT("beach/defeat"),
    BEACH_REWARD("beach/reward"),

    DARK_AGES_CHOOSE("dark_ages/choose_seeds"),
    DARK_AGES_WAVE_FIRST("dark_ages/wave_first"),
    DARK_AGES_WAVE_MID_A("dark_ages/wave_mid_a"),
    DARK_AGES_WAVE_MID_B("dark_ages/wave_mid_b"),
    DARK_AGES_WAVE_FINAL("dark_ages/wave_final"),
    DARK_AGES_VICTORY("dark_ages/victory"),
    DARK_AGES_DEFEAT("dark_ages/defeat"),
    DARK_AGES_REWARD("dark_ages/reward");

    /** Relative path under {@code assets/music/} without extension. */
    public final String relativePath;

    MusicTracks(String relativePath) {
        this.relativePath = relativePath;
    }

    public enum WavePhase {
        FIRST,
        MID,
        FINAL
    }

    public static MusicTracks chooseSeeds(Chapter chapter) {
        return switch (chapter) {
            case ANCIENT_EGYPT -> EGYPT_CHOOSE;
            case FROSTBITE_CAVES -> FROSTBITE_CHOOSE;
            case BIG_WAVE_BEACH -> BEACH_CHOOSE;
            case DARK_AGES -> DARK_AGES_CHOOSE;
        };
    }

    public static MusicTracks wave(Chapter chapter, WavePhase phase) {
        return switch (chapter) {
            case ANCIENT_EGYPT -> switch (phase) {
                case FIRST -> EGYPT_WAVE_FIRST;
                case MID -> EGYPT_WAVE_MID;
                case FINAL -> EGYPT_WAVE_FINAL;
            };
            case FROSTBITE_CAVES -> switch (phase) {
                case FIRST -> FROSTBITE_WAVE_FIRST;
                case MID -> FROSTBITE_WAVE_MID_A;
                case FINAL -> FROSTBITE_WAVE_FINAL;
            };
            case BIG_WAVE_BEACH -> switch (phase) {
                case FIRST -> BEACH_WAVE_FIRST;
                case MID -> BEACH_WAVE_MID_A;
                case FINAL -> BEACH_WAVE_FINAL;
            };
            case DARK_AGES -> switch (phase) {
                case FIRST -> DARK_AGES_WAVE_FIRST;
                case MID -> DARK_AGES_WAVE_MID_A;
                case FINAL -> DARK_AGES_WAVE_FINAL;
            };
        };
    }

    /** Alternate mid-wave bed when the chapter has A/B tracks (Frostbite / Beach / Dark Ages). */
    public static MusicTracks waveMidB(Chapter chapter) {
        return switch (chapter) {
            case ANCIENT_EGYPT -> EGYPT_WAVE_MID;
            case FROSTBITE_CAVES -> FROSTBITE_WAVE_MID_B;
            case BIG_WAVE_BEACH -> BEACH_WAVE_MID_B;
            case DARK_AGES -> DARK_AGES_WAVE_MID_B;
        };
    }

    public static MusicTracks victory(Chapter chapter) {
        return switch (chapter) {
            case ANCIENT_EGYPT -> EGYPT_VICTORY;
            case FROSTBITE_CAVES -> FROSTBITE_VICTORY;
            case BIG_WAVE_BEACH -> BEACH_VICTORY;
            case DARK_AGES -> DARK_AGES_VICTORY;
        };
    }

    public static MusicTracks defeat(Chapter chapter) {
        return switch (chapter) {
            case ANCIENT_EGYPT -> EGYPT_DEFEAT;
            case FROSTBITE_CAVES -> FROSTBITE_DEFEAT;
            case BIG_WAVE_BEACH -> BEACH_DEFEAT;
            case DARK_AGES -> DARK_AGES_DEFEAT;
        };
    }

    public static MusicTracks reward(Chapter chapter) {
        return switch (chapter) {
            case ANCIENT_EGYPT -> EGYPT_REWARD;
            case FROSTBITE_CAVES -> FROSTBITE_REWARD;
            case BIG_WAVE_BEACH -> BEACH_REWARD;
            case DARK_AGES -> DARK_AGES_REWARD;
        };
    }
}
