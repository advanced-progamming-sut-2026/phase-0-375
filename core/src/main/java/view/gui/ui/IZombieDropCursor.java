package view.gui.ui;

/**
 * Keyboard drop-cell for couch-play I, Zombie. Stays on tiles at/right of the red line.
 */
public final class IZombieDropCursor {
    private IZombieDropCursor() {}

    /** Top-left tile of the zombie-side grid: row 0, column = red line. */
    public static void origin(int[] colRow, int redLineColumn) {
        colRow[0] = Math.max(0, redLineColumn);
        colRow[1] = 0;
    }

    public static void nudge(int[] colRow, int dCol, int dRow,
                             int minCol, int maxCol, int rows) {
        if (colRow == null || colRow.length < 2 || rows <= 0) {
            return;
        }
        colRow[0] = clamp(colRow[0] + dCol, minCol, maxCol);
        colRow[1] = clamp(colRow[1] + dRow, 0, rows - 1);
    }

    static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
