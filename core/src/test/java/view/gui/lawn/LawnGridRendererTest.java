package view.gui.lawn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LawnGridRendererTest {

    @Test
    void layoutCalculationsMatchGridBounds() {
        LawnLayout layout = LawnLayout.frontLawnDefault();
        assertEquals(5, layout.rows());
        assertEquals(9, layout.cols());

        // Left edge of column 0 is lawn origin
        assertEquals(LawnLayout.LAWN_ORIGIN_X, layout.cellLeft(0), 1e-4f);
        // Left edge of column 9 is rightmost boundary
        assertEquals(LawnLayout.LAWN_ORIGIN_X + LawnLayout.GRID_WIDTH, layout.cellLeft(0) + layout.cols() * layout.cellWidth(), 1e-4f);

        // Top and bottom grid bounds
        float top = LawnLayout.LAWN_ORIGIN_Y + LawnLayout.GRID_HEIGHT;
        float bottom = LawnLayout.LAWN_ORIGIN_Y;
        assertEquals(top, layout.cellBottom(0) + layout.cellHeight(), 1e-4f);
        assertEquals(bottom, layout.cellBottom(layout.rows() - 1), 1e-4f);
    }
}
