package model;

import model.plant.instance.PlantInstance;

public class Cell {
    private int column;
    private int row;
    private Placeable placeableObject;

    public Cell(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public Placeable getPlaceableObject() {
        return placeableObject;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public void setPlaceableObject(Placeable placeableObject) {
        this.placeableObject = placeableObject;
    }

    public boolean isOccupied(PlantInstance plantInstance) {
        return this.placeableObject == null;
    }

    public void remove() {}

    public void place(Placeable placeableObject) {}
}
