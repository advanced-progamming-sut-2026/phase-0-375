package view.gui.assets;

import model.enums.ArmorType;
import model.enums.Chapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Definition-name → PAM catalog name aliases for zombies.
 *
 * <p>{@code ZombieDefault} / {@code ZombieArmor*} share one body PAM per biome.
 * Armor is the same clip with extra parts shown, not a different file.
 */
public final class ZombiePamAliases {
    public static final String EGYPT_BASIC = "ZOMBIE_EGYPT_BASIC";
    public static final String ICEAGE_BASIC = "ZOMBIE_ICEAGE_BASIC";
    public static final String BEACH_BASIC = "ZOMBIE_BEACH_BASIC";
    public static final String DARK_BASIC = "ZOMBIE_DARK_BASIC";

    private static final Set<String> BIOME_BASIC_BODIES = Set.of(
            "ZombieDefault", "ZombieArmor1", "ZombieArmor2", "ZombieArmor4",
            "ZombieIZombieSun");

    private static final Map<String, String> BY_NAME = createByName();

    private ZombiePamAliases() {}

    /** Unmodifiable map of exclusive definition name → animations.json PAM name. */
    public static Map<String, String> all() {
        return BY_NAME;
    }

    /**
     * Catalog PAM name for this definition in {@code chapter}. Biome basics follow the
     * lawn; everything else uses {@link #all()}.
     */
    public static String pamName(String definitionName, Chapter chapter) {
        if (definitionName == null) {
            return null;
        }
        if (BIOME_BASIC_BODIES.contains(definitionName)) {
            return basicForChapter(chapter);
        }
        if ("ZombieGargantuar".equals(definitionName)) {
            return gargantuarForChapter(chapter);
        }
        if ("ZombieImp".equals(definitionName)) {
            return impForChapter(chapter);
        }
        return BY_NAME.get(definitionName);
    }

    public static boolean usesBiomeBasicBody(String definitionName) {
        return BIOME_BASIC_BODIES.contains(definitionName);
    }

    /** Basics plus Gargantuar and Imp: one PAM per lawn chapter. */
    public static boolean usesChapterArt(String definitionName) {
        return usesBiomeBasicBody(definitionName)
                || "ZombieGargantuar".equals(definitionName)
                || "ZombieImp".equals(definitionName);
    }

    public static String basicForChapter(Chapter chapter) {
        if (chapter == null) {
            return EGYPT_BASIC;
        }
        return switch (chapter) {
            case ANCIENT_EGYPT -> EGYPT_BASIC;
            case FROSTBITE_CAVES -> ICEAGE_BASIC;
            case BIG_WAVE_BEACH -> BEACH_BASIC;
            case DARK_AGES -> DARK_BASIC;
        };
    }

    public static String gargantuarForChapter(Chapter chapter) {
        if (chapter == null) {
            return "EGYPT_GARGANTUAR";
        }
        return switch (chapter) {
            case ANCIENT_EGYPT -> "EGYPT_GARGANTUAR";
            case FROSTBITE_CAVES -> "ZOMBIE_ICEAGE_GARGANTUAR";
            case BIG_WAVE_BEACH -> "BEACH_GARGANTUAR";
            case DARK_AGES -> "DARK_GARGANTUAR";
        };
    }

    public static String impForChapter(Chapter chapter) {
        if (chapter == null) {
            return "ZOMBIE_EGYPT_IMP";
        }
        return switch (chapter) {
            case ANCIENT_EGYPT -> "ZOMBIE_EGYPT_IMP";
            case FROSTBITE_CAVES -> "ZOMBIE_ICEAGE_IMP";
            case BIG_WAVE_BEACH -> "ZOMBIE_BEACH_IMP_MERMAID";
            case DARK_AGES -> "ZOMBIE_DARK_IMP_MONK";
        };
    }

    /**
     * Hidden armor-states group on a {@code *_BASIC} PAM. Armor1/2/4 (and DarkArmor3)
     * each have their own group; null if this clip has none.
     */
    public static String armorStatesPart(String pamName, String definitionName) {
        if (pamName == null) {
            return null;
        }
        String n = pamName.toUpperCase(Locale.ROOT);
        if (!n.contains("BASIC")) {
            return null;
        }
        int slot = armorSlot(definitionName);
        if (slot <= 0) {
            return null;
        }
        String biome;
        if (n.contains("EGYPT")) {
            biome = "egypt";
        } else if (n.contains("ICEAGE")) {
            biome = "iceage";
        } else if (n.contains("BEACH")) {
            biome = "beach";
        } else if (n.contains("DARK")) {
            biome = "dark";
        } else {
            return null;
        }
        // Ice Age / Dark Ages brick is a leaf on the BASIC clip, not *_armor4_states.
        if (slot == 4 && ("iceage".equals(biome) || "dark".equals(biome))) {
            return "zombie_armor_brick_norm";
        }
        return "_zombie_" + biome + "_armor" + slot + "_states";
    }

    /** Cone=1, bucket=2, brick=4. Knight crown/shoulder use {@link #armorGroupPart}. */
    private static int armorSlot(String definitionName) {
        if (definitionName == null) {
            return 0;
        }
        if (definitionName.endsWith("Armor1")) {
            return 1;
        }
        if (definitionName.endsWith("Armor2")) {
            return 2;
        }
        if (definitionName.endsWith("Armor4")) {
            return 4;
        }
        return 0;
    }

    /** Extra PAM group for knight crown / shoulder; biome Armor1/2/4 use {@link #armorStatesPart}. */
    public static String armorGroupPart(ArmorType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case Crown -> "_zombie_armor_crown_states";
            case ShoulderArmor -> "zombie_shoulder_armor";
            default -> null;
        };
    }

    private static Map<String, String> createByName() {
        Map<String, String> m = new HashMap<>();
        m.put("ZombieDarkArmor3", DARK_BASIC);
        m.put("ZombieRa", "ZOMBIE_EGYPT_RA");
        m.put("ZombieExplorer", "ZOMBIE_EGYPT_EXPLORER");
        m.put("ZombieTombRaiser", "ZOMBIE_EGYPT_TOMBRAISER");
        m.put("ZombieImp", "ZOMBIE_EGYPT_IMP");
        m.put("ZombieIceAgeDodo", "ZOMBIE_ICEAGE_DODORIDER");
        m.put("ZombieIceAgeHunter", "ZOMBIE_ICEAGE_HUNTER");
        m.put("ZombieIceAgeTroglobite", "ZOMBIE_ICEAGE_TROGLOBITE");
        m.put("ZombieBeachFisherman", "ZOMBIE_BEACH_FISHERMAN");
        m.put("ZombieBeachOctopus", "ZOMBIE_BEACH_OCTOPUS");
        m.put("ZombieBeachSnorkel", "ZOMBIE_BEACH_SNORKELER");
        m.put("ZombieDarkJuggler", "ZOMBIE_DARK_JESTER");
        m.put("ZombieWizard", "ZOMBIE_DARK_WIZARD");
        m.put("ZombieDarkKing", "ZOMBIE_DARK_KING");
        m.put("ZombieDarkImpDragon", "ZOMBIE_DARK_IMP_DRAGON");
        m.put("ZombieModernAllStar", "ZOMBIE_MODERN_ALLSTAR");
        m.put("ZombieLostCityJane", "ZOMBIE_LOSTCITY_JANE");
        m.put("ZombieCrystalSkull", "ZOMBIE_LOSTCITY_CRYSTALSKULL");
        m.put("ZombieProspector", "ZOMBIE_PROSPECTOR");
        m.put("ZombiePiano", "ZOMBIE_PIANO");
        m.put("ZombieNewspaper", "ZOMBIE_MODERN_NEWSPAPER");
        m.put("ZombieArcade", "ZOMBIE_80S_ARCADE");
        m.put("ZombieBarrelRoller", "ZOMBIE_PIRATE_BARREL_PUSHER");
        m.put("ZombiePirateImp", "ZOMBIE_PIRATE_IMP");
        m.put("ZombotanyPeashooter", "PEASHOOTER");
        m.put("ZombotanyWallnut", "WALLNUT");
        m.put("ZombotanyJalapeno", "JALAPENO");
        m.put("ZombotanySquash", "SQUASH");
        return Collections.unmodifiableMap(m);
    }
}
