package view.gui.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WaveAnnounceBannerTest {

    @Test
    void shrinksFromLargeToSmall() {
        assertTrue(WaveAnnounceBanner.START_SCALE > WaveAnnounceBanner.END_SCALE);
    }
}
