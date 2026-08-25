package view.gui.assets;

import java.util.Locale;
import java.util.Map;

/**
 * Zombie definition name -> {@code IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_*} region in
 * {@code UI_ZombiePackets}.
 */
public final class ZombiePacketIds {
    public static final String ATLAS_GROUP = "UI_ZombiePackets_768";
    public static final String ATLAS_PAGE = "ATLASIMAGE_ATLAS_UI_ZOMBIEPACKETS_768_00";
    public static final String READY = "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_READY";
    public static final String SELECT = "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_SELECTED";

    private static final String PREFIX = "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_";

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("ZombieDefault", "TUTORIAL"),
            Map.entry("ZombieArmor1", "TUTORIAL_ARMOR1"),
            Map.entry("ZombieArmor2", "TUTORIAL_ARMOR2"),
            Map.entry("ZombieArmor4", "TUTORIAL_ARMOR4"),
            Map.entry("ZombieImp", "TUTORIAL_IMP"),
            Map.entry("ZombieGargantuar", "TUTORIAL_GARGANTUAR"),
            Map.entry("ZombieNewspaper", "MODERN_NEWSPAPER"),
            Map.entry("ZombieModernAllStar", "MODERN_ALLSTAR"),
            Map.entry("ZombieWizard", "DARK_WIZARD"),
            Map.entry("ZombieDarkKing", "DARK_KING"),
            Map.entry("ZombieDarkArmor3", "DARK_ARMOR3"),
            Map.entry("ZombieExplorer", "EXPLORER"),
            Map.entry("ZombieProspector", "PROSPECTOR"));

    private ZombiePacketIds() {}

    public static String portraitId(String zombieName) {
        if (zombieName == null || zombieName.isBlank()) {
            return null;
        }
        String key = ALIASES.get(zombieName);
        if (key == null) {
            key = compact(zombieName);
        }
        return PREFIX + key;
    }

    static String compact(String name) {
        String rest = name;
        if (rest.regionMatches(true, 0, "Zombie", 0, 6)) {
            rest = rest.substring(6);
        }
        if (rest.isEmpty()) {
            return name.toUpperCase(Locale.ROOT);
        }
        StringBuilder out = new StringBuilder(rest.length() + 4);
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (i > 0) {
                char prev = rest.charAt(i - 1);
                boolean boundary = Character.isUpperCase(c) && Character.isLowerCase(prev);
                if (boundary) {
                    out.append('_');
                }
            }
            if (Character.isLetterOrDigit(c)) {
                out.append(Character.toUpperCase(c));
            }
        }
        return out.toString();
    }
}
