package view.gui.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IZombieDropCursorTest {

    @Test
    void originIsTopLeftOfZombieTiles() {
        int[] cell = new int[2];
        IZombieDropCursor.origin(cell, 3);
        assertArrayEquals(new int[]{3, 0}, cell);
    }

    @Test
    void arrowsStayInsideRedLineAndBoard() {
        int[] cell = {3, 0};
        IZombieDropCursor.nudge(cell, -1, -1, 3, 8, 5);
        assertArrayEquals(new int[]{3, 0}, cell);

        IZombieDropCursor.nudge(cell, 1, 1, 3, 8, 5);
        assertArrayEquals(new int[]{4, 1}, cell);

        IZombieDropCursor.nudge(cell, 20, 20, 3, 8, 5);
        assertArrayEquals(new int[]{8, 4}, cell);
        assertEquals(8, IZombieDropCursor.clamp(99, 3, 8));
    }
}
