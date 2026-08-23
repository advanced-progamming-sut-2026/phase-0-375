package view.gui.ui;

import com.badlogic.gdx.math.Rectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SunHudTest {
    @Test
    void countSitsInBarToTheRightOfTheSun() {
        Rectangle sun = new Rectangle();
        Rectangle bar = new Rectangle();
        Rectangle text = new Rectangle();
        SunHud.layoutRects(SunHud.WIDTH, SunHud.HEIGHT, sun, bar, text);
        assertEquals(0f, sun.x, 0.01f);
        assertEquals(SunHud.SUN_W, sun.width, 0.01f);
        assertTrue(sun.height > bar.height);
        assertTrue(bar.x > 0f && bar.x < sun.width);
        assertEquals(sun.x + sun.width, text.x, 0.01f);
        assertEquals(bar.y, text.y, 0.01f);
        assertEquals(bar.height, text.height, 0.01f);
    }

    @Test
    void hiddenWithoutAGame() {
        assertFalse(SunHud.showFor(null));
    }
}
