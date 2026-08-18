package model.zombie.instance;

import model.enums.Chapter;
import model.enums.ZombieSize;
import model.zombie.definition.Zombie;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JaneAshTest {

    @Test
    void fireKillMarksBlownUp() {
        ZombieInstance jane = jane();
        jane.takeFireDamage(jane.getCurrentHP());
        assertTrue(jane.isBlownUp());
    }

    @Test
    void peaKillDoesNotMarkBlownUp() {
        ZombieInstance jane = jane();
        jane.takeDamage(jane.getCurrentHP());
        assertFalse(jane.isBlownUp());
    }

    private static ZombieInstance jane() {
        return new ZombieInstance(new Zombie(
                "ZombieLostCityJane", 350, 0.25f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 200, 1, List.of(), null, null, List.of()));
    }
}
