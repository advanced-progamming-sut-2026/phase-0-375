package model.game.map;

import java.util.List;

public class GameMap {
    private int rows;
    private int cols;
    private List<Lane> lanes;
    private Cell[][] grid;

    public GameMap(int rows, int cols, List<Lane> lanes, Cell[][] grid) {
        this.rows = rows;
        this.cols = cols;
        this.lanes = lanes;
        this.grid = grid;
    }


    public Cell getCell(int x, int y) { return grid[x][y]; }


}
