package view.gui.anim.zombie;

import org.junit.jupiter.api.Test;
import view.gui.lawn.ScreenShake;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GargantuarAnimTest {

    private static final float CLIP = 2.4f;

    @Test
    void firstSampleDoesNotStomp() {
        assertFalse(GargantuarAnim.crossedWalkStomp(-1f, 0.80f, CLIP));
        assertFalse(GargantuarAnim.crossedWalkStomp(-1f, 1.95f, CLIP));
    }

    @Test
    void stompsAtWalkClipTimes() {
        assertTrue(GargantuarAnim.crossedWalkStomp(0.72f, 0.74f, CLIP));
        assertTrue(GargantuarAnim.crossedWalkStomp(1.89f, 1.91f, CLIP));
        assertFalse(GargantuarAnim.crossedWalkStomp(0.74f, 0.80f, CLIP));
        assertFalse(GargantuarAnim.crossedWalkStomp(1.91f, 2.00f, CLIP));
    }

    @Test
    void wrapAndSecondCycleStillHit() {
        assertFalse(GargantuarAnim.crossedWalkStomp(2.30f, 0.10f, CLIP));
        assertTrue(GargantuarAnim.crossedWalkStomp(1.85f, 0.80f, CLIP));
        assertTrue(GargantuarAnim.crossedWalkStomp(CLIP + 0.72f, CLIP + 0.74f, CLIP));
    }

    @Test
    void pulseRumblesThenStops() {
        ScreenShake shake = new ScreenShake();
        shake.pulse();
        shake.update(0.05f);
        assertTrue(shake.active());
        shake.update(1f);
        assertFalse(shake.active());
    }
}
