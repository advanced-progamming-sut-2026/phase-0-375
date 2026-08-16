package view.gui.assets;

import model.enums.ArmorType;
import model.enums.Chapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ZombiePamAliasesTest {

    @Test
    void defaultAndArmorFollowChapter() {
        assertEquals("ZOMBIE_EGYPT_BASIC",
                ZombiePamAliases.pamName("ZombieDefault", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_ICEAGE_BASIC",
                ZombiePamAliases.pamName("ZombieArmor1", Chapter.FROSTBITE_CAVES));
        assertEquals("ZOMBIE_BEACH_BASIC",
                ZombiePamAliases.pamName("ZombieArmor2", Chapter.BIG_WAVE_BEACH));
        assertEquals("ZOMBIE_DARK_BASIC",
                ZombiePamAliases.pamName("ZombieArmor4", Chapter.DARK_AGES));
    }

    @Test
    void gargantuarFollowsChapter() {
        assertEquals("EGYPT_GARGANTUAR",
                ZombiePamAliases.pamName("ZombieGargantuar", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_ICEAGE_GARGANTUAR",
                ZombiePamAliases.pamName("ZombieGargantuar", Chapter.FROSTBITE_CAVES));
        assertEquals("BEACH_GARGANTUAR",
                ZombiePamAliases.pamName("ZombieGargantuar", Chapter.BIG_WAVE_BEACH));
        assertEquals("DARK_GARGANTUAR",
                ZombiePamAliases.pamName("ZombieGargantuar", Chapter.DARK_AGES));
    }

    @Test
    void impFollowsChapter() {
        assertEquals("ZOMBIE_EGYPT_IMP",
                ZombiePamAliases.pamName("ZombieImp", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_ICEAGE_IMP",
                ZombiePamAliases.pamName("ZombieImp", Chapter.FROSTBITE_CAVES));
        assertEquals("ZOMBIE_BEACH_IMP_MERMAID",
                ZombiePamAliases.pamName("ZombieImp", Chapter.BIG_WAVE_BEACH));
        assertEquals("ZOMBIE_DARK_IMP_MONK",
                ZombiePamAliases.pamName("ZombieImp", Chapter.DARK_AGES));
    }

    @Test
    void exclusiveZombiesStayOnTheirPam() {
        assertEquals("ZOMBIE_DARK_BASIC",
                ZombiePamAliases.pamName("ZombieDarkArmor3", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_EGYPT_RA",
                ZombiePamAliases.pamName("ZombieRa", Chapter.DARK_AGES));
        assertEquals("ZOMBIE_EGYPT_EXPLORER",
                ZombiePamAliases.pamName("ZombieExplorer", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_EGYPT_TOMBRAISER",
                ZombiePamAliases.pamName("ZombieTombRaiser", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_ICEAGE_DODORIDER",
                ZombiePamAliases.pamName("ZombieIceAgeDodo", Chapter.FROSTBITE_CAVES));
        assertEquals("ZOMBIE_ICEAGE_HUNTER",
                ZombiePamAliases.pamName("ZombieIceAgeHunter", Chapter.FROSTBITE_CAVES));
        assertEquals("ZOMBIE_ICEAGE_TROGLOBITE",
                ZombiePamAliases.pamName("ZombieIceAgeTroglobite", Chapter.FROSTBITE_CAVES));
        assertEquals("ZOMBIE_MODERN_ALLSTAR",
                ZombiePamAliases.pamName("ZombieModernAllStar", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_80S_ARCADE",
                ZombiePamAliases.pamName("ZombieArcade", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_LOSTCITY_JANE",
                ZombiePamAliases.pamName("ZombieLostCityJane", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_LOSTCITY_CRYSTALSKULL",
                ZombiePamAliases.pamName("ZombieCrystalSkull", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_PROSPECTOR",
                ZombiePamAliases.pamName("ZombieProspector", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_PIANO",
                ZombiePamAliases.pamName("ZombiePiano", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_MODERN_NEWSPAPER",
                ZombiePamAliases.pamName("ZombieNewspaper", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_PIRATE_BARREL_PUSHER",
                ZombiePamAliases.pamName("ZombieBarrelRoller", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_PIRATE_IMP",
                ZombiePamAliases.pamName("ZombiePirateImp", Chapter.ANCIENT_EGYPT));
        assertEquals("ZOMBIE_BEACH_FISHERMAN",
                ZombiePamAliases.pamName("ZombieBeachFisherman", Chapter.BIG_WAVE_BEACH));
        assertNull(ZombiePamAliases.armorStatesPart("ZOMBIE_EGYPT_RA", "ZombieRa"));
        assertEquals("_zombie_egypt_armor1_states",
                ZombiePamAliases.armorStatesPart("ZOMBIE_EGYPT_BASIC", "ZombieArmor1"));
        assertEquals("_zombie_beach_armor2_states",
                ZombiePamAliases.armorStatesPart("ZOMBIE_BEACH_BASIC", "ZombieArmor2"));
        assertEquals("_zombie_egypt_armor4_states",
                ZombiePamAliases.armorStatesPart("ZOMBIE_EGYPT_BASIC", "ZombieArmor4"));
        assertEquals("zombie_armor_brick_norm",
                ZombiePamAliases.armorStatesPart("ZOMBIE_ICEAGE_BASIC", "ZombieArmor4"));
        assertEquals("zombie_armor_brick_norm",
                ZombiePamAliases.armorStatesPart("ZOMBIE_DARK_BASIC", "ZombieArmor4"));
        assertNull(ZombiePamAliases.armorStatesPart("ZOMBIE_DARK_BASIC", "ZombieDarkArmor3"));
        assertEquals("_zombie_armor_crown_states",
                ZombiePamAliases.armorGroupPart(ArmorType.Crown));
        assertEquals("zombie_shoulder_armor",
                ZombiePamAliases.armorGroupPart(ArmorType.ShoulderArmor));
    }
}
