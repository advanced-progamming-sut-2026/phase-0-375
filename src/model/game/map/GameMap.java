package model.game.map;

import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

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
        this.grid = IntStream.range(0, cols)
                .mapToObj(j -> IntStream.range(0, rows)
                        .mapToObj(i -> new Cell(i, j))
                        .toArray(Cell[]::new))
                .toArray(Cell[][]::new);
    }


    public Cell getCell(int x, int y) { return grid[x][y]; }

    /** @return the {@link Lane} at the given row index. */
    public Lane getLane(int row) {
        if (row < 0 || row >= rows) {
            return null;
        }
        return lanes[row];
    }

    public void addZombie(ZombieInstance instance, int x, int y) {
        if (x < 0 || y < 0 || x >= cols || y >= rows) return;

        grid[x][y].addZombie(instance);
    }

    public void addProjectile(Projectile projectile, int x, int y) {
        if (x < 0 || y < 0 || x >= cols || y >= rows) return;

        grid[x][y].addProjectile(projectile);
    }

    public void removeZombie(ZombieInstance instance) {
        for (Cell[] row : grid) {
            for (Cell cell : row) {
                // We call this function on every cell, one would eventually have the instance and remove it.
                cell.removeZombie(instance);
            }
        }
    }

    public void removeProjectile(Projectile projectile) {
        for (Cell[] row : grid) {
            for (Cell cell : row) {
                // We call this function on every cell, one would eventually have the instance and remove it.
                cell.removeProjectile(projectile);
            }
        }
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }
}
