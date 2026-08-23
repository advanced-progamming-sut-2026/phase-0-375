package model.game.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LawnMowerTest {

    @Test
    void triggerDoesNotMoveUntilTransitionEnds() {
        LawnMower mower = new LawnMower();
        mower.trigger();

        assertTrue(mower.isTriggered());
        assertFalse(mower.isActive());
        assertFalse(mower.isSweeping());
        assertEquals(LawnMower.REST_COL, mower.getXPosition(), 0.0001f);

        assertFalse(mower.tick(LawnMower.TRANSITION_SEC * 0.5f, 9));
        assertFalse(mower.isSweeping());
        assertEquals(LawnMower.REST_COL, mower.getXPosition(), 0.0001f);

        assertFalse(mower.tick(LawnMower.TRANSITION_SEC * 0.5f, 9));
        assertTrue(mower.isSweeping());
        assertEquals(LawnMower.REST_COL, mower.getXPosition(), 0.0001f);
    }

    @Test
    void beginSweepStartsImmediatelyAndLeavesTheBoard() {
        LawnMower mower = new LawnMower();
        mower.trigger();
        mower.beginSweep();

        assertTrue(mower.isSweeping());
        assertFalse(mower.tick(0.01f, 9));
        float x = mower.getXPosition();
        assertTrue(x > LawnMower.REST_COL);
        assertTrue(x < 9f);

        assertTrue(mower.tick(10f, 9));
        assertTrue(mower.getXPosition() >= 10.5f);
    }
}
