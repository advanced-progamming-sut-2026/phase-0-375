package view.gui.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadySetPlantBannerTest {

    @Test
    void plantIsLastAndBigger() {
        assertEquals("Ready...", ReadySetPlantBanner.WORDS[0]);
        assertEquals("Set...", ReadySetPlantBanner.WORDS[1]);
        assertEquals("PLANT", ReadySetPlantBanner.WORDS[2]);
        assertEquals(3, ReadySetPlantBanner.WORDS.length);
        assertTrue(ReadySetPlantBanner.PEAK_SCALE[2] > ReadySetPlantBanner.PEAK_SCALE[0]);
        assertEquals(ReadySetPlantBanner.PEAK_SCALE[0], ReadySetPlantBanner.PEAK_SCALE[1]);
    }
}
