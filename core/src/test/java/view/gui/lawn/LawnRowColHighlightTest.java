package view.gui.lawn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LawnRowColHighlightTest {
    @Test
    void coversRowAndColumnNotDiagonal() {
        LawnLayout layout = LawnLayout.frontLawnDefault();
        int col = 2;
        int row = 1;
        assertTrue(LawnRowColHighlight.covers(layout, col, row, layout.centerX(col), layout.centerY(row)));
        assertTrue(LawnRowColHighlight.covers(layout, col, row, layout.centerX(7), layout.centerY(row)));
        assertTrue(LawnRowColHighlight.covers(layout, col, row, layout.centerX(col), layout.centerY(4)));
        assertFalse(LawnRowColHighlight.covers(layout, col, row, layout.centerX(7), layout.centerY(4)));
        assertFalse(LawnRowColHighlight.covers(layout, -1, row, layout.centerX(col), layout.centerY(row)));
    }
}
