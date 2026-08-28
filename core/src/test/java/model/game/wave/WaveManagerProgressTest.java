package model.game.wave;

import model.enums.WaveManagerPhase;
import model.enums.WaveState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void clearingLastWaveDoesNotWalkPastEnd() {
        Wave a = new Wave(1, List.of(), 0f, false, false);
        Wave b = new Wave(2, List.of(), 0f, false, false);
        Wave c = new Wave(3, List.of(), 0f, false, true);
        WaveManager wm = new WaveManager(List.of(a, b, c));
        wm.tick(1f);
        assertTrue(wm.hasNextWave());
        a.setState(WaveState.COMPLETE);
        wm.tick(0.01f);
        assertTrue(wm.hasNextWave());
        wm.tick(1f);
        b.setState(WaveState.COMPLETE);
        wm.tick(0.01f);
        assertFalse(wm.hasNextWave());
        wm.tick(1f);
        c.setState(WaveState.COMPLETE);
        wm.tick(0.01f);
        assertTrue(wm.isLevelDone());
        assertEquals(2, wm.getCurrentWaveIndex());
    }

    @Test
    void killFractionMovesMeterWithinWave() {
        Wave a = new Wave(1, List.of(), 0f, false, false);
        Wave b = new Wave(2, List.of(), 0f, false, true);
        WaveManager wm = new WaveManager(List.of(a, b));
        wm.tick(1f);
        wm.debugSetWaveCounts(4, 0);
        assertEquals(0f, wm.debugWaveKillFraction(), 0.0001f);
        wm.debugSetWaveCounts(4, 1);
        assertEquals(0.25f, wm.debugWaveKillFraction(), 0.0001f);
        wm.debugSetWaveCounts(4, 2);
        assertEquals(0.5f, wm.debugWaveKillFraction(), 0.0001f);
        assertEquals(0.25f, wm.progress01(), 0.0001f);
    }

    @Test
    void progressIsMonotonicallyNonDecreasing() {
        Wave a = new Wave(1, List.of(), 0f, false, false);
        Wave b = new Wave(2, List.of(), 0f, false, true);
        WaveManager wm = new WaveManager(List.of(a, b));
        wm.tick(1f);
        wm.debugSetWaveCounts(4, 2); // 50% of wave 1 -> 0.25
        assertEquals(0.25f, wm.progress01(), 0.0001f);

        // If total spawns dynamically increase, progress should not drop
        wm.debugSetWaveCounts(8, 2); // 25% of wave 1 -> would calculate 0.125
        assertEquals(0.25f, wm.progress01(), 0.0001f);
    }
}
