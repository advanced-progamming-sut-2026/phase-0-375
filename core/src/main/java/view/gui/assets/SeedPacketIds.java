package view.gui.assets;

import java.util.Map;

/**
 * Plant definition name → {@code IMAGE_UI_PACKETS_*} region in {@code UI_SeedPackets}.
 */
public final class SeedPacketIds {
    public static final String ATLAS = "UI_SeedPackets";
    public static final String READY = "IMAGE_UI_PACKETS_READY";
    public static final String EMPTY = "IMAGE_UI_PACKETS_EMPTY_PACKET";
    public static final String SELECT = "IMAGE_UI_PACKETS_SELECT";
    public static final String BOOST = "IMAGE_UI_PACKETS_BOOST";
    public static final String LOCK = "IMAGE_UI_CARDS_LOCK_MEDIUM";

    private static final String PREFIX = "IMAGE_UI_PACKETS_";
    private static final Map<String, String> ALIASES = Map.of(
            "Iceberg Lettuce", "ICEBURG",
            "Cherry Bomb", "CHERRY_BOMB",
            "Pierce-mint", "PEPPERMINT",
            "Mega Gatling Pea", "MEGAGATLING",
            "Giant Wall-nut", "WALLNUT",
            "Explode-o-nut", "EXPLODEONUT");

    private SeedPacketIds() {}

    public static String portraitId(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return null;
        }
        String key = ALIASES.getOrDefault(plantName, compact(plantName));
        return PREFIX + key;
    }

    static String compact(String name) {
        StringBuilder out = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                out.append(Character.toUpperCase(c));
            }
        }
        return out.toString();
    }
}
