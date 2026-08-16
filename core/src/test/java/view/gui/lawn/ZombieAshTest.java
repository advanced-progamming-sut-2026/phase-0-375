package view.gui.lawn;

import model.enums.Chapter;
import model.enums.ZombieSize;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZombieAshTest {

    @Test
    void blownUpAshPicksSizeAndSpecialBodies() {
        assertEquals("ZOMBIE_LOSTCITY_JANE_ASH",
                LawnEntityRenderer.ashPamFor(zombie("ZombieLostCityJane", ZombieSize.NORMAL)));
        assertEquals("ZOMBIE_BIG_ASH",
                LawnEntityRenderer.ashPamFor(zombie("ZombieArcade", ZombieSize.NORMAL)));
        assertEquals("ZOMBIE_BIG_ASH",
                LawnEntityRenderer.ashPamFor(zombie("ZombieIceAgeTroglobite", ZombieSize.NORMAL)));
        assertEquals("ZOMBIE_BIG_ASH",
                LawnEntityRenderer.ashPamFor(zombie("ZombieBeachOctopus", ZombieSize.NORMAL)));
        assertEquals("ZOMBIE_GARGANTUAR_ASH",
                LawnEntityRenderer.ashPamFor(zombie("ZombieGargantuar", ZombieSize.LARGE)));
        assertEquals("ZOMBIE_IMP_ASH",
                LawnEntityRenderer.ashPamFor(zombie("ZombieImp", ZombieSize.IMP)));
        assertEquals("ZOMBIE_IMP_ASH",
                LawnEntityRenderer.ashPamFor(zombie("ZombiePirateImp", ZombieSize.IMP)));
        assertEquals("ZOMBIE_IMP_ASH",
                LawnEntityRenderer.ashPamFor(zombie("ZombieDarkImpDragon", ZombieSize.IMP)));
        assertEquals("ZOMBIE_ASH",
                LawnEntityRenderer.ashPamFor(zombie("ZombieDefault", ZombieSize.NORMAL)));
    }

    private static ZombieInstance zombie(String name, ZombieSize size) {
        return new ZombieInstance(new Zombie(
                name, 100, 0.25f, 100f, size,
                Chapter.ANCIENT_EGYPT, 100, 1, List.of(), null, null, List.of()));
    }
}
