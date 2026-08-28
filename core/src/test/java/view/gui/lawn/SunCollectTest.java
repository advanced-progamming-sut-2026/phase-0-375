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
        assertTrue(SunCollect.done(SunCollect.FLY_SEC + SunCollect.POP_SEC));
    }

    @Test
    void sunScaleAndClipReflectSunType() {
        model.item.Sun normalSun = new model.item.Sun(model.enums.SunType.NORMAL, 25, 0, 0);
        model.item.Sun specialSun = new model.item.Sun(model.enums.SunType.SPECIAL, 100, 0, 0);
        model.item.Sun radioactiveSun = new model.item.Sun(model.enums.SunType.RADIOACTIVE, 150, 0, 0);

        assertEquals(1.0f, LawnEntityRenderer.sunScale(normalSun), 1e-4f);
        assertEquals(1.10f, LawnEntityRenderer.sunScale(specialSun), 1e-4f);
        assertEquals(1.25f, LawnEntityRenderer.sunScale(radioactiveSun), 1e-4f);
        assertEquals(1.0f, LawnEntityRenderer.sunScale(null), 1e-4f);

        assertEquals("SUN", LawnEntityRenderer.sunPam(normalSun));
        assertEquals("SUN", LawnEntityRenderer.sunPam(specialSun));
        assertEquals("SUN_BOMB", LawnEntityRenderer.sunPam(radioactiveSun));

        assertEquals("animation", LawnEntityRenderer.sunClip(normalSun));
        assertEquals("red", LawnEntityRenderer.sunClip(specialSun));

        // Radioactive stages while falling
        radioactiveSun.setFall(5f, 5f); // progress = 0.0 -> stage 1: animation
        assertEquals("animation", LawnEntityRenderer.sunClip(radioactiveSun));

        radioactiveSun.setFall(2.5f, 5f); // progress = 0.5 -> stage 2: animation2
        assertEquals("animation2", LawnEntityRenderer.sunClip(radioactiveSun));

        radioactiveSun.setFall(0.5f, 5f); // progress = 0.9 -> stage 3: animation3
        assertEquals("animation3", LawnEntityRenderer.sunClip(radioactiveSun));

        // Transitioning into normal sun
        radioactiveSun.transitionToNormal();
        assertTrue(radioactiveSun.isTransitioning());
        assertEquals("SUN_BOMB", LawnEntityRenderer.sunPam(radioactiveSun));
        assertEquals("transition", LawnEntityRenderer.sunClip(radioactiveSun));

        // After transition
        radioactiveSun.completeTransition();
        assertTrue(radioactiveSun.isTransitioned());
        assertEquals("SUN_BOMB", LawnEntityRenderer.sunPam(radioactiveSun));
        assertEquals("normalSunIdle", LawnEntityRenderer.sunClip(radioactiveSun));
        assertEquals(1.0f, LawnEntityRenderer.sunScale(radioactiveSun), 1e-4f);
    }
}
