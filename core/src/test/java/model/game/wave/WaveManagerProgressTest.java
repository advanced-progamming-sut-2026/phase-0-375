package model.game.wave;

import model.enums.WaveManagerPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaveManagerProgressTest {

    @Test
    void flagsMarkEqualSectionEnds() {
        float[] flags = WaveManager.flagStops01(3);
        assertEquals(2, flags.length);
        assertEquals(1f / 3f, flags[0], 0.0001f);
        assertEquals(2f / 3f, flags[1], 0.0001f);
    }

    @Test
    void progressWalksEqualSections() {
        assertEquals(0f, WaveManager.progress01(
            2, 0, WaveManagerPhase.WAITING_FOR_NEXT_WAVE, 0f), 0.0001f);
        assertEquals(0.5f, WaveManager.progress01(
            2, 1, WaveManagerPhase.WAITING_FOR_NEXT_WAVE, 0f), 0.0001f);
        assertEquals(0.75f, WaveManager.progress01(
            2, 1, WaveManagerPhase.ACTIVE_WAVE, 0.5f), 0.0001f);
        assertEquals(1f, WaveManager.progress01(
            2, 1, WaveManagerPhase.LEVEL_DONE, 0f), 0.0001f);
    }
}
