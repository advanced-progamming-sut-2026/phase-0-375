package view.gui.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZombieHotkeysTest {

    @Test
    void lettersBindInAsdfOrder() {
        assertEquals('A', ZombieHotkeys.letterAt(0));
        assertEquals('S', ZombieHotkeys.letterAt(1));
        assertEquals('D', ZombieHotkeys.letterAt(2));
        assertEquals(0, ZombieHotkeys.indexOf('A'));
        assertEquals(4, ZombieHotkeys.indexOf('g'));
        assertEquals(-1, ZombieHotkeys.indexOf('Z'));
        assertEquals(0, ZombieHotkeys.letterAt(-1));
        assertEquals(0, ZombieHotkeys.letterAt(99));
    }
}
