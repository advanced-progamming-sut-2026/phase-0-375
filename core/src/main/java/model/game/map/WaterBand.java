package model.game.map;

import model.enums.GroundType;
import model.game.map.terrain.TerrainStrategyFactory;

/**
 * Right-edge water strip used by Big Wave Beach and the debug sandbox.
 *
 * <p>Default coverage is the last {@link #DEFAULT_COLUMNS} columns, every row.
 * Tide / debug can pass a different width; {@link #columnsFromRight} reads the
 * live map so the underlayer PAM can follow JSON water tiles later.
 */
public final class WaterBand {
    public static final int DEFAULT_COLUMNS = 3;

    private WaterBand() {}

    public static boolean isFlooded(GroundType ground) {
        return ground == GroundType.WATER || ground == GroundType.LOW_TIDE;
    }

    public static boolean isFlooded(Cell cell) {
        return cell != null && isFlooded(cell.getGroundType());
    }

    /**
     * Contiguous flooded columns counting left from the right edge.
     * A column counts if any row in it is water / low-tide.
     */
    public static int columnsFromRight(GameMap map) {
        if (map == null) {
            return 0;
        }
        int cols = map.getCols();
        int rows = map.getRows();
        int count = 0;
        for (int col = cols - 1; col >= 0; col--) {
            if (!columnHasWater(map, col, rows)) {
                break;
            }
            count++;
        }
        return count;
    }

    /**
     * Floods the rightmost {@code columnsFromRight} columns with {@link GroundType#WATER}.
     * Dries water / low-tide in the remaining columns. Ice and other specials stay put.
     */
    public static void applyFromRight(GameMap map, int columnsFromRight) {
        if (map == null) {
            return;
        }
        int cols = map.getCols();
        int rows = map.getRows();
        int n = Math.max(0, Math.min(columnsFromRight, cols));
        int firstFlood = cols - n;
        for (int col = 0; col < cols; col++) {
            boolean flood = col >= firstFlood;
            for (int row = 0; row < rows; row++) {
                Cell cell = map.getCell(col, row);
                if (cell == null) {
                    continue;
                }
                if (flood) {
                    floodCell(cell);
                } else {
                    dryCell(cell);
                }
            }
        }
    }

    /**
     * Moves the water line one column inland ({@code delta > 0}, left) or
     * seaward ({@code delta < 0}, right). Clamped to {@code 0..cols}.
     *
     * @return live {@link #columnsFromRight} after the nudge
     */
    public static int nudgeFromRight(GameMap map, int delta) {
        if (map == null) {
            return 0;
        }
        int next = columnsFromRight(map) + delta;
        applyFromRight(map, next);
        return columnsFromRight(map);
    }

    private static boolean columnHasWater(GameMap map, int col, int rows) {
        for (int row = 0; row < rows; row++) {
            if (isFlooded(map.getCell(col, row))) {
                return true;
            }
        }
        return false;
    }

    private static void floodCell(Cell cell) {
        if (!canFlood(cell.getGroundType())) {
            return;
        }
        cell.setGroundType(GroundType.WATER);
        cell.setTerrainStrategy(TerrainStrategyFactory.create(GroundType.WATER));
    }

    private static void dryCell(Cell cell) {
        if (!isFlooded(cell.getGroundType())) {
            return;
        }
        cell.setGroundType(GroundType.NORMAL);
        cell.setTerrainStrategy(TerrainStrategyFactory.create(GroundType.NORMAL));
    }

    private static boolean canFlood(GroundType ground) {
        return ground == null
                || ground == GroundType.NORMAL
                || ground == GroundType.WATER
                || ground == GroundType.LOW_TIDE;
    }
}
