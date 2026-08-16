package model.zombie.instance;

import model.enums.Chapter;
import model.enums.ZombieSize;
import model.zombie.definition.Zombie;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EatDamageCarryTest {

    @Test
    void highFpsStillDeliversEatDps() {
        ZombieInstance zombie = new ZombieInstance(new Zombie(
                "ZombieBasic", 100, 0.25f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 100, 1, List.of(), null, null, List.of()));
        int chopped = 0;
        int carried = 0;
        float dt = 1f / 144f;
        for (int i = 0; i < 144; i++) {
            chopped += (int) (100f * dt);
            carried += zombie.addEatDamage(100f * dt);
        }
        assertEquals(0, chopped);
        assertTrue(carried >= 99 && carried <= 100, "carried=" + carried);
    }
}
