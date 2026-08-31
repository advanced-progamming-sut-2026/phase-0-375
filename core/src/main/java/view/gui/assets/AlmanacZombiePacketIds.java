package view.gui.assets;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Zombie definition name → {@code IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_*} portrait.
 * Frame chrome: {@link #READY}, {@link #SELECTED}, {@link #GUIDE}.
 */
public final class AlmanacZombiePacketIds {
    public static final String PREFIX = "IMAGE_UI_ALMANAC_PACKETS_ZOMBIES_";
    public static final String READY = PREFIX + "READY";
    public static final String SELECTED = PREFIX + "SELECTED";
    public static final String GUIDE = PREFIX + "GUIDE";

    private static final Map<String, String> ALIASES = createAliases();

    private AlmanacZombiePacketIds() {}

    public static String portraitId(String zombieName) {
        if (zombieName == null || zombieName.isBlank()) {
            return null;
        }
        String suffix = ALIASES.get(zombieName);
        if (suffix == null) {
            suffix = guessSuffix(zombieName);
        }
        return suffix == null ? null : PREFIX + suffix;
    }

    private static String guessSuffix(String name) {
        String compact = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (compact.startsWith("ZOMBIE")) {
            compact = compact.substring("ZOMBIE".length());
        }
        if (compact.isEmpty()) {
            return null;
        }
        // Insert underscores before capitals already flattened — keep compact as last resort.
        return compact;
    }

    private static Map<String, String> createAliases() {
        Map<String, String> m = new HashMap<>();
        m.put("ZombieDefault", "MUMMY");
        m.put("ZombieArmor1", "MUMMY_ARMOR1");
        m.put("ZombieArmor2", "MUMMY_ARMOR2");
        m.put("ZombieArmor4", "MUMMY_ARMOR4");
        m.put("ZombotanyPeashooter", "PIRATE_ARMOR4");
        m.put("ZombotanyWallnut", "PIRATE_ARMOR4");
        m.put("ZombotanyJalapeno", "PIRATE_ARMOR4");
        m.put("ZombotanySquash", "PIRATE_ARMOR4");
        m.put("ZombieDarkArmor3", "DARK_ARMOR3");
        m.put("ZombieGargantuar", "EGYPT_GARGANTUAR");
        m.put("ZombieImp", "EGYPT_IMP");
        m.put("ZombieRa", "RA");
        m.put("ZombieExplorer", "EXPLORER");
        m.put("ZombieTombRaiser", "TOMB_RAISER");
        m.put("ZombieIceAgeDodo", "ICEAGE_DODO");
        m.put("ZombieIceAgeHunter", "ICEAGE_HUNTER");
        m.put("ZombieIceAgeTroglobite", "ICEAGE_TROGLOBITE");
        m.put("ZombieBeachFisherman", "BEACH_FISHERMAN");
        m.put("ZombieBeachOctopus", "BEACH_OCTOPUS");
        m.put("ZombieBeachSnorkel", "BEACH_SNORKEL");
        m.put("ZombieDarkJuggler", "DARK_JUGGLER");
        m.put("ZombieWizard", "DARK_WIZARD");
        m.put("ZombieDarkKing", "DARK_KING");
        m.put("ZombieDarkImpDragon", "DARK_IMP_DRAGON");
        m.put("ZombieModernAllStar", "MODERN_ALLSTAR");
        m.put("ZombieLostCityJane", "LOSTCITY_JANE");
        m.put("ZombieCrystalSkull", "LOSTCITY_CRYSTALSKULL");
        m.put("ZombieProspector", "PROSPECTOR");
        m.put("ZombiePiano", "PIANO");
        m.put("ZombieNewspaper", "MODERN_NEWSPAPER");
        m.put("ZombieArcade", "EIGHTIES_ARCADE");
        m.put("ZombieBarrelRoller", "BARRELROLLER");
        m.put("ZombiePirateImp", "PIRATE_IMP");
        m.put("ZombieEgyptZomboss", "ZOMBOSSMECH_EGYPT");
        m.put("ZombieIceZomboss", "ZOMBOSSMECH_ICEAGE");
        m.put("ZombieBeachZomboss", "ZOMBOSSMECH_BEACH");
        m.put("ZombieDarkZomboss", "ZOMBOSSMECH_DARK");
        // Sheet-only zombies (e.g. ZombieIZombieSun) fall back to spritesheet portraits.
        return m;
    }
}
