package view.gui.lawn;

import model.game.map.WaterBand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class WaterUnderlayerRendererTest {

    @Test
    void oneColumnWaterLineIsLastColumnNotFirst() {
        LawnLayout layout = LawnLayout.frontLawnDefault();
        float half = 0.5f * layout.cellWidth();
        assertEquals(layout.cellLeft(8) - half, WaterUnderlayerRenderer.waterLineX(layout, 1), 1e-3f);
        assertNotEquals(layout.cellLeft(0), WaterUnderlayerRenderer.waterLineX(layout, 1), 1f);
        assertEquals(
                layout.cellLeft(layout.cols() - WaterBand.DEFAULT_COLUMNS) - half,
                WaterUnderlayerRenderer.waterLineX(layout, WaterBand.DEFAULT_COLUMNS),
                1e-3f);
    }

    @Test
    void clipLeftLandsOnWaterLine() {
        LawnLayout layout = LawnLayout.frontLawnDefault();
        float scale = WaterUnderlayerRenderer.scaleForHeight(1390f);
        float localLeft = -530f;
        float x = WaterUnderlayerRenderer.drawCenterX(
                layout, 1, localLeft, 484f, scale);
        float line = layout.cellLeft(8) - 0.5f * layout.cellWidth();
        assertEquals(line - localLeft * scale, x, 1e-3f);
        assertEquals(line, x + localLeft * scale, 1e-3f);
    }

    @Test
    void oneMoreColumnSlidesLeftByOneCell() {
        LawnLayout layout = LawnLayout.frontLawnDefault();
        float scale = WaterUnderlayerRenderer.scaleForHeight(1390f);
        float three = WaterUnderlayerRenderer.drawCenterX(layout, 3, -200f, 400f, scale);
        float four = WaterUnderlayerRenderer.drawCenterX(layout, 4, -200f, 400f, scale);
        assertEquals(layout.cellWidth(), three - four, 1e-3f);
    }

    @Test
    void heightScaleIgnoresColumnCount() {
        assertEquals(
                LawnLayout.WORLD_HEIGHT / 1390f,
                WaterUnderlayerRenderer.scaleForHeight(1390f),
                1e-4f);
    }

    @Test
    void moveTowardStepsThenSnaps() {
        assertEquals(3f, WaterUnderlayerRenderer.moveToward(0f, 10f, 3f), 1e-4f);
        assertEquals(10f, WaterUnderlayerRenderer.moveToward(8f, 10f, 3f), 1e-4f);
        assertEquals(7f, WaterUnderlayerRenderer.moveToward(10f, 0f, 3f), 1e-4f);
    }
}
