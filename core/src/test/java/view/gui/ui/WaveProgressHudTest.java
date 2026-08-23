package view.gui.ui;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaveProgressHudTest {

    @Test
    void flagLeftTouchesVisiblePoleTopRight() {
        Rectangle pole = new Rectangle();
        Rectangle flag = new Rectangle();
        WaveProgressHud.layoutFlag(0.4f, pole, flag);
        float stopX = WaveProgressHud.stopCenterX(0.4f);
        assertEquals(stopX + WaveProgressHud.POLE_VIS_HALF_W - WaveProgressHud.FLAG_TRIM_L, flag.x, 0.01f);
        assertEquals(pole.y + pole.height, flag.y + flag.height, 0.01f);
    }

    @Test
    void headMovesLeftAsProgressGrows() {
        Rectangle a = new Rectangle();
        Rectangle b = new Rectangle();
        WaveProgressHud.layoutHead(0f, a);
        WaveProgressHud.layoutHead(1f, b);
        assertTrue(b.x < a.x);
    }

    @Test
    void flagsAtSectionBoundariesForThreeWaves() {
        Rectangle pole = new Rectangle();
        Rectangle flag = new Rectangle();
        WaveProgressHud.layoutFlag(1f / 3f, pole, flag);
        assertEquals(WaveProgressHud.stopCenterX(1f / 3f), pole.x + pole.width * 0.5f, 0.5f);
    }

    @Test
    void ensureKnobAfterFillPromotesKnobWhenMissing() {
        ProgressBarStyle style = new ProgressBarStyle();
        style.knob = new TextureRegionDrawable();
        style.knobAfter = null;
        WaveProgressHud.ensureKnobAfterFill(style);
        assertNotNull(style.knobAfter);
        assertSame(style.knob, style.knobAfter);
    }
}
