package model.game.map;

import java.util.stream.IntStream;

public class GameMap {
    private int rows;
    private int cols;
    private Lane[] lanes;
    private Cell[][] grid;

    public GameMap(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.lanes = IntStream.range(0, rows)
                .mapToObj(i -> new Lane())
                .toArray(Lane[]::new);
        this.grid = IntStream.range(0, rows)
                .mapToObj(i -> IntStream.range(0, cols)
                        .mapToObj(j -> new Cell(i, j))
                        .toArray(Cell[]::new))
                .toArray(Cell[][]::new);
    }


    public Cell getCell(int x, int y) { return grid[x][y]; }


}
