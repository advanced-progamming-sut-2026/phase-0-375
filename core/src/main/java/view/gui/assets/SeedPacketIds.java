package view.gui.assets;

import com.badlogic.gdx.graphics.Color;
import model.enums.PlantCategory;
import model.enums.PlantTags;
import model.plant.definition.Plant;

import java.util.HashMap;
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
    private static final String MINTFAM_PREFIX = "IMAGE_UI_PACKETS_MINTFAM_";

    /** Definition name → compact suffix after {@link #PREFIX}. */
    private static final Map<String, String> ALIASES = aliases();

    private SeedPacketIds() {}

    public static String portraitId(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return null;
        }
        // No packet art shipped for Cat-tail — sheet portrait is applied at packet call sites.
        if ("Cat-tail".equals(plantName)) {
            return null;
        }
        return PREFIX + atlasSuffix(plantName);
    }

    /** Almanac family badge from {@link Plant#getCategory()}, e.g. {@code IMAGE_UI_PACKETS_MINTFAM_PEASHOOTER}. */
    public static String familyIconId(Plant plant) {
        if (plant == null || plant.getCategory() == null) {
            return null;
        }
        String suffix = familySuffix(plant);
        return suffix == null ? null : MINTFAM_PREFIX + suffix;
    }

    /** Circle tint behind the mint-family icon in the almanac overlay. */
    public static Color familyColor(Plant plant) {
        String suffix = familySuffix(plant);
        if (suffix == null) {
            return new Color(0.45f, 0.45f, 0.45f, 1f);
        }
        return FAMILY_COLORS.getOrDefault(suffix, new Color(0.45f, 0.45f, 0.45f, 1f));
    }

    private static final Map<String, Color> FAMILY_COLORS = familyColors();

    private static Map<String, Color> familyColors() {
        Map<String, Color> m = new HashMap<>();
        m.put("PEASHOOTER", color(0.24f, 0.62f, 0.28f));
        m.put("LOBBER", color(0.82f, 0.24f, 0.18f));
        m.put("MELEE", color(0.92f, 0.52f, 0.12f));
        m.put("EXPLOSIVE", color(0.78f, 0.18f, 0.12f));
        m.put("SUN", color(0.95f, 0.78f, 0.15f));
        m.put("DEFENSE", color(0.58f, 0.40f, 0.22f));
        m.put("MAGIC", color(0.55f, 0.28f, 0.72f));
        m.put("SHARP", color(0.20f, 0.48f, 0.82f));
        m.put("ELECTRICITY", color(0.35f, 0.72f, 0.95f));
        m.put("FIRE", color(0.92f, 0.32f, 0.12f));
        m.put("COLD", color(0.35f, 0.72f, 0.88f));
        m.put("POISON", color(0.42f, 0.72f, 0.22f));
        m.put("SHADOW", color(0.32f, 0.22f, 0.48f));
        m.put("SLOW", color(0.45f, 0.62f, 0.88f));
        m.put("TRAP", color(0.48f, 0.36f, 0.24f));
        m.put("BANNER", color(0.50f, 0.50f, 0.55f));
        return Map.copyOf(m);
    }

    private static Color color(float r, float g, float b) {
        return new Color(r, g, b, 1f);
    }

    static String familySuffix(Plant plant) {
        if (plant.getTags() != null) {
            for (PlantTags tag : plant.getTags()) {
                if (tag == PlantTags.FIRE) {
                    return "FIRE";
                }
                if (tag == PlantTags.ICE) {
                    return "COLD";
                }
                if (tag == PlantTags.POISON) {
                    return "POISON";
                }
            }
        }
        return switch (plant.getCategory()) {
            case SUN_PRODUCER -> "SUN";
            case SHOOTER -> "PEASHOOTER";
            case LOBBER -> "LOBBER";
            case EXPLOSIVE -> "EXPLOSIVE";
            case MELEE -> "MELEE";
            case WALL_NUT -> "DEFENSE";
            case MODIFIER -> "MAGIC";
            case STRIKE_THROUGH -> "SHARP";
            case HOMING -> "ELECTRICITY";
            case MINT -> "BANNER";
        };
    }

    private static String atlasSuffix(String plantName) {
        return ALIASES.getOrDefault(plantName, compact(plantName));
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

    private static Map<String, String> aliases() {
        Map<String, String> m = new HashMap<>();
        m.put("Iceberg Lettuce", "ICEBURG");
        m.put("Cherry Bomb", "CHERRY_BOMB");
        m.put("Pierce-mint", "PEPPERMINT");
        m.put("catTail-mint", "SPEARMINT");
        m.put("Mega Gatling Pea", "MEGAGATLING");
        m.put("Giant Wall-nut", "WALLNUT");
        m.put("Explode-o-nut", "EXPLODEONUT");
        // No dedicated GOOPEASHOOTER / ROTOBAGA packet IDs — these are the real portraits.
        m.put("Goo Peashooter", "POISONPEASHOOTER");
        m.put("Rotobaga", "XSHOT");
        return Map.copyOf(m);
    }
}
