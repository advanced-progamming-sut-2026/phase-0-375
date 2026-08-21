package view.gui.ui;

import model.game.systems.ChapterEffectsSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaveAnnounceBannerTest {

    @Test
    void shrinksFromLargeToSmall() {
        assertTrue(WaveAnnounceBanner.START_SCALE > WaveAnnounceBanner.END_SCALE);
    }

    @Test
    void chapterStingsMatchRequestedCopy() {
        assertEquals("Necromancy!", ChapterEffectsSystem.NECROMANCY_ANNOUNCE);
        assertEquals("Low Tide!", ChapterEffectsSystem.LOW_TIDE_ANNOUNCE);
    }
}
