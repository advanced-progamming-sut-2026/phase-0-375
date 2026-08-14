package view.gui.lawn;

/**
 * Maps lawn grid cells to FrontLawn world coordinates (Y-up).
 *
 * <p>Camera / UI virtual size is LEFT + center TEXTURE only (always fully
 * visible, snapped top/bottom). {@code TEXTURE_RIGHT} is drawn past the right
 * edge and only peeks in when the window is wider than that base aspect.
 * ROW_05 and the interactive 5×9 grid sit on the center texture.
 */
public final class LawnLayout {
    public static final float WORLD_HEIGHT = 768f;

    /** Side panels flanking the main FrontLawn texture (768 atlas sizes). */
    public static final float TEXTURE_LEFT_WIDTH = 278f;
    public static final float TEXTURE_RIGHT_WIDTH = 673f;

    /** Main center texture size. */
    public static final float TEXTURE_WIDTH = 1024f;
    public static final float TEXTURE_HEIGHT = WORLD_HEIGHT;

    /**
     * Virtual world width for the camera: left + center only.
     * Right panel is drawn beyond this and is not required to fit.
     */
    public static final float WORLD_WIDTH = TEXTURE_LEFT_WIDTH + TEXTURE_WIDTH;

    /** libGDX X of each panel (left → center → right). */
    public static final float TEXTURE_LEFT_X = 0f;
    public static final float TEXTURE_ORIGIN_X = TEXTURE_LEFT_WIDTH;
    public static final float TEXTURE_RIGHT_X = TEXTURE_ORIGIN_X + TEXTURE_WIDTH;

    public static final float ROW05_WIDTH = 791f;
    public static final float ROW05_HEIGHT = 498f;

    public static final int DEFAULT_ROWS = 5;
    public static final int DEFAULT_COLS = 9;

    /**
     * Top-left of ROW_05 within the center texture (Y-down).
     * From RESOURCES: {@code (1017 - 556) / 2}, {@code 395 / 2}.
     */
    public static final float ROW05_TOP_LEFT_X = (1017f - 556f) / 2f;
    public static final float ROW05_TOP_LEFT_Y_DOWN = 395f / 2f;

    /** libGDX bottom-left draw position for ROW_05 (world space). */
    public static final float ROW05_DRAW_X = TEXTURE_ORIGIN_X + ROW05_TOP_LEFT_X;
    public static final float ROW05_DRAW_Y = WORLD_HEIGHT - ROW05_TOP_LEFT_Y_DOWN - ROW05_HEIGHT;

    /**
     * Inset of the checkered tile rect inside the ROW_05 PNG (measured from
     * opaque/green content: x 24..765, y 3..489).
     */
    public static final float GRID_INSET_LEFT = 24f;
    public static final float GRID_INSET_RIGHT = ROW05_WIDTH - 766f;
    public static final float GRID_INSET_TOP = 3f;
    public static final float GRID_INSET_BOTTOM = ROW05_HEIGHT - 490f;

    /** Playable grid size inside ROW_05. */
    public static final float GRID_WIDTH = ROW05_WIDTH - GRID_INSET_LEFT - GRID_INSET_RIGHT;
    public static final float GRID_HEIGHT = ROW05_HEIGHT - GRID_INSET_TOP - GRID_INSET_BOTTOM;

    /** Bottom-left of the playable grid (libGDX). */
    public static final float LAWN_ORIGIN_X = ROW05_DRAW_X + GRID_INSET_LEFT;
    public static final float LAWN_ORIGIN_Y = ROW05_DRAW_Y + GRID_INSET_BOTTOM;

    /** Default cell size: playable grass ÷ 9 / ÷ 5. */
    public static final float CELL_WIDTH = GRID_WIDTH / DEFAULT_COLS;
    public static final float CELL_HEIGHT = GRID_HEIGHT / DEFAULT_ROWS;

    private final int rows;
    private final int cols;
    private final float cellWidth;
    private final float cellHeight;

    public LawnLayout(int rows, int cols) {
        this.rows = Math.max(1, rows);
        this.cols = Math.max(1, cols);
        this.cellWidth = GRID_WIDTH / this.cols;
        this.cellHeight = GRID_HEIGHT / this.rows;
    }

    public static LawnLayout frontLawnDefault() {
        return new LawnLayout(DEFAULT_ROWS, DEFAULT_COLS);
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public float cellWidth() {
        return cellWidth;
    }

    public float cellHeight() {
        return cellHeight;
    }

    /** World-space center of a cell. Row 0 is the top row. */
    public float centerX(int col) {
        return LAWN_ORIGIN_X + col * cellWidth + cellWidth * 0.5f;
    }

    public float centerY(int row) {
        float top = LAWN_ORIGIN_Y + GRID_HEIGHT;
        return top - row * cellHeight - cellHeight * 0.5f;
    }

    /**
     * Center for a zombie with fractional column progress (continuous X).
     * {@code progressX} is model continuous X (column units).
     */
    public float[] centerOf(int row, float progressX) {
        return centerOf((float) row, progressX);
    }

    /**
     * World center for a continuous grid position (projectiles, bouncing bulbs).
     * {@code row} and {@code progressX} are model units; row 0 is the top lane.
     */
    public float[] centerOf(float row, float progressX) {
        float top = LAWN_ORIGIN_Y + GRID_HEIGHT;
        return new float[]{
                LAWN_ORIGIN_X + progressX * cellWidth + cellWidth * 0.5f,
                top - row * cellHeight - cellHeight * 0.5f
        };
    }

    public float[] centerOf(int row, int col) {
        return new float[]{centerX(col), centerY(row)};
    }

    /** Bottom-left of a cell in libGDX world space. */
    public float cellLeft(int col) {
        return LAWN_ORIGIN_X + col * cellWidth;
    }

    public float cellBottom(int row) {
        float top = LAWN_ORIGIN_Y + GRID_HEIGHT;
        return top - (row + 1) * cellHeight;
    }

    /**
     * @return {@code true} if the world point maps to an in-bounds cell;
     *         writes col into {@code out[0]} and row into {@code out[1]}.
     */
    public boolean worldToCell(float worldX, float worldY, int[] out) {
        int col = (int) Math.floor((worldX - LAWN_ORIGIN_X) / cellWidth);
        float top = LAWN_ORIGIN_Y + GRID_HEIGHT;
        int row = (int) Math.floor((top - worldY) / cellHeight);
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return false;
        }
        out[0] = col;
        out[1] = row;
        return true;
    }
}
