package view.gui.assets;

import model.enums.Chapter;

/** Chapter lawn-mower PAM names ({@code animations.json}). */
public final class MowerPam {
    public static final String SPAWN = "MOWER_SPAWN";

    private MowerPam() {}

    public static String forChapter(Chapter chapter) {
        if (chapter == null) {
            return "MOWER_EGYPT";
        }
        return switch (chapter) {
            case ANCIENT_EGYPT -> "MOWER_EGYPT";
            case FROSTBITE_CAVES -> "MOWER_ICEAGE";
            case DARK_AGES -> "MOWER_DARK";
            case BIG_WAVE_BEACH -> "MOWER_BEACH";
        };
    }
}
