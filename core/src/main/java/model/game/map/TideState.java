package model.game.map;

import model.enums.GroundType;
import model.game.level.LevelConfig;
import model.game.map.terrain.TerrainStrategyFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Single source of truth for Big Wave Beach flooding: permanent sea, the
 * dynamic tide band, and low-tide ambush markers. {@link #applyToMap} is the
 * only writer of {@link GroundType#WATER} / {@link GroundType#LOW_TIDE} in
 * that band (static sea included).
 */
public final class TideState {

    private final Set<Long> staticWater;
    private final int limitColumns;
    private Set<Long> lowTide = Set.of();
    private int dynamicColumns;

    private TideState(Set<Long> staticWater, int limitColumns) {
        this.staticWater = staticWater;
        this.limitColumns = Math.max(0, limitColumns);
    }

    /** Inactive tide (no static sea, no dynamic band). */
    public static TideState inactive() {
        return new TideState(Set.of(), 0);
    }

    public static TideState fromConfig(LevelConfig config) {
        if (config == null) {
            return inactive();
        }
        List<Point> water = config.getWaterTiles();
        int limit = config.getTideLimitColumn();
        Set<Long> sea = encodePoints(water);
        if (sea.isEmpty() && limit <= 0) {
            return inactive();
        }
        TideState state = new TideState(sea, limit);
        state.setLowTide(config.getLowTideTiles());
        return state;
    }

    public boolean isActive() {
        return !staticWater.isEmpty() || limitColumns > 0;
    }

    /** True when wave-start tide shifts are enabled ({@code tideLimitColumn > 0}). */
    public boolean hasDynamicBand() {
        return limitColumns > 0;
    }

    public int getLimitColumns() {
        return limitColumns;
    }

    public int getDynamicColumns() {
        return dynamicColumns;
    }

    public void setDynamicColumns(int columns) {
        dynamicColumns = Math.max(0, columns);
    }

    public void setLowTide(List<Point> tiles) {
        lowTide = encodePoints(tiles);
    }

    /**
     * Columns the wave wall should cover: dynamic tide width, floored only by
     * contiguous full-column permanent sea (every row static). Partial static
     * columns — e.g. corner-only water — do not widen the PAM past the model.
     */
    public int floodedColumns(int mapCols, int mapRows) {
        if (mapCols <= 0 || mapRows <= 0) {
            return 0;
        }
        return Math.max(dynamicColumns, fullStaticColumnExtent(mapCols, mapRows));
    }

    /** Rewrites water / low-tide ground from this state. */
    public void applyToMap(GameMap map) {
        if (!isActive() || map == null) {
            return;
        }
        int cols = map.getCols();
        int rows = map.getRows();
        int bandStart = limitColumns > 0 ? cols - limitColumns : cols;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Cell cell = map.getCell(col, row);
                if (cell == null) {
                    continue;
                }
                long key = key(row, col);
                if (staticWater.contains(key)) {
                    setFlooded(cell, lowTide.contains(key));
                    continue;
                }
                if (limitColumns <= 0 || col < bandStart) {
                    continue;
                }
                if (col >= cols - dynamicColumns) {
                    setFlooded(cell, lowTide.contains(key));
                } else if (GroundType.WATER == cell.getGroundType()
                        || GroundType.LOW_TIDE == cell.getGroundType()) {
                    setDry(cell);
                }
            }
        }
    }

    private int fullStaticColumnExtent(int cols, int rows) {
        int count = 0;
        for (int col = cols - 1; col >= 0; col--) {
            if (!isFullStaticColumn(col, rows)) {
                break;
            }
            count++;
        }
        return count;
    }

    /** True when every row in {@code col} is a configured {@code waterTiles} cell. */
    private boolean isFullStaticColumn(int col, int rows) {
        for (int row = 0; row < rows; row++) {
            if (!staticWater.contains(key(row, col))) {
                return false;
            }
        }
        return true;
    }

    private static void setFlooded(Cell cell, boolean shallow) {
        GroundType ground = shallow ? GroundType.LOW_TIDE : GroundType.WATER;
        cell.setGroundType(ground);
        cell.setTerrainStrategy(TerrainStrategyFactory.create(ground));
    }

    private static void setDry(Cell cell) {
        cell.setGroundType(GroundType.NORMAL);
        cell.setTerrainStrategy(TerrainStrategyFactory.create(GroundType.NORMAL));
    }

    private static Set<Long> encodePoints(List<Point> points) {
        if (points == null || points.isEmpty()) {
            return Set.of();
        }
        Set<Long> out = new HashSet<>(points.size());
        for (Point p : points) {
            out.add(key(p.getY(), p.getX()));
        }
        return Collections.unmodifiableSet(out);
    }

    private static long key(int row, int col) {
        return ((long) row << 32) | (col & 0xFFFFFFFFL);
    }
}
