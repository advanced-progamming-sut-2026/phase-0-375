package view.gui.lawn;

import org.junit.jupiter.api.Test;
import view.gui.anim.GraveAnim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SunCollectTest {
    @Test
    void pickupUsesSpriteRadiusNotATile() {
        assertTrue(SunCollect.hits(100f, 200f, 100f, 200f));
        assertTrue(SunCollect.hits(100f, 200f, 100f + SunCollect.HIT_RADIUS * 0.5f, 200f));
        assertFalse(SunCollect.hits(100f, 200f, 100f + SunCollect.HIT_RADIUS + 2f, 200f));
        assertFalse(SunCollect.hits(0f, 0f, LawnLayout.CELL_WIDTH, 0f));
    }

    @Test
    void flyEasesInThenPopsBigThenSmall() {
        assertEquals(0f, SunCollect.flyU(0f), 1e-4f);
        assertTrue(SunCollect.flyU(SunCollect.FLY_SEC * 0.5f) < 0.5f);
        assertEquals(1f, SunCollect.flyU(SunCollect.FLY_SEC), 1e-4f);
        assertTrue(SunCollect.flying(0f));
        assertFalse(SunCollect.flying(SunCollect.FLY_SEC));

        assertEquals(1f, SunCollect.vanishU(SunCollect.FLY_SEC), 1e-4f);
        assertEquals(1f, GraveAnim.scaleX(SunCollect.vanishU(SunCollect.FLY_SEC)), 1e-4f);
        float mid = SunCollect.FLY_SEC + SunCollect.POP_SEC * 0.4f;
        assertTrue(GraveAnim.scaleY(SunCollect.vanishU(mid)) > 1f);
        assertTrue(GraveAnim.scaleX(SunCollect.vanishU(SunCollect.FLY_SEC + SunCollect.POP_SEC)) > 1f);
        assertTrue(SunCollect.done(SunCollect.FLY_SEC + SunCollect.POP_SEC));
    }
}
