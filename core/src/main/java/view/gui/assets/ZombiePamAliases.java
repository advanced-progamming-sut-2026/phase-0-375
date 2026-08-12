package view.gui.assets;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Definition-name → PAM catalog name aliases for zombies.
 */
public final class ZombiePamAliases {
    private ZombiePamAliases() {}

    /** Unmodifiable map of game definition name → animations.json PAM name. */
    public static Map<String, String> all() {
        Map<String, String> m = new HashMap<>();
        m.put("ZombieDefault", "ZOMBIE_EGYPT_BASIC");
        m.put("ZombieArmor1", "ZOMBIE_EGYPT_BASIC");
        m.put("ZombieArmor2", "ZOMBIE_EGYPT_BASIC");
        m.put("ZombieArmor4", "ZOMBIE_EGYPT_BASIC");
        m.put("ZombieDarkArmor3", "ZOMBIE_DARK_BASIC");
        m.put("ZombieRa", "ZOMBIE_EGYPT_RA");
        m.put("ZombieExplorer", "ZOMBIE_EGYPT_EXPLORER");
        m.put("ZombieTombRaiser", "ZOMBIE_EGYPT_TOMBRAISER");
        m.put("ZombieImp", "IMP");
        m.put("ZombieGargantuar", "GARGANTUAR");
        m.put("ZombieIceAgeDodo", "ZOMBIE_ICEAGE_DODO");
        m.put("ZombieIceAgeHunter", "ZOMBIE_ICEAGE_HUNTER");
        m.put("ZombieIceAgeTroglobite", "ZOMBIE_ICEAGE_TROGLOBITE");
        m.put("ZombieBeachFisherman", "ZOMBIE_BEACH_FISHERMAN");
        m.put("ZombieBeachOctopus", "ZOMBIE_BEACH_OCTOPUS");
        m.put("ZombieBeachSnorkel", "ZOMBIE_BEACH_SNORKEL");
        m.put("ZombieDarkJuggler", "ZOMBIE_DARK_JESTER");
        m.put("ZombieWizard", "ZOMBIE_DARK_WIZARD");
        m.put("ZombieDarkKing", "ZOMBIE_DARK_KING");
        m.put("ZombieDarkImpDragon", "ZOMBIE_DARK_IMP_DRAGON");
        m.put("ZombieModernAllStar", "ZOMBIE_MODERN_ALLSTAR");
        m.put("ZombieLostCityJane", "ZOMBIE_LOSTCITY_JANE");
        m.put("ZombieCrystalSkull", "ZOMBIE_CRYSTALSKULL");
        m.put("ZombieProspector", "ZOMBIE_COWBOY_PROSPECTOR");
        m.put("ZombiePiano", "ZOMBIE_PIANO");
        m.put("ZombieNewspaper", "ZOMBIE_MODERN_NEWSPAPER");
        m.put("ZombieArcade", "ZOMBIE_80S_ARCADE");
        m.put("ZombieBarrelRoller", "ZOMBIE_PIRATE_BARRELROLLER");
        return Collections.unmodifiableMap(m);
    }
}
