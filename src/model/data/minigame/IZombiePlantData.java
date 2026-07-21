package model.data.minigame;

/**
 * JSON DTO for one pre-planted plant in an I, Zombie stage
 * (see the "prePlantedPlants" key in minigames.json).
 */
public class IZombiePlantData {

    /** Plant definition name (from plants.json). */
    private String plant;
    /** Grid row (0-based). */
    private int row;
    /** Grid column (0-based, must be left of the red line). */
    private int col;

    public String getPlant() { return plant; }
    public void setPlant(String plant) { this.plant = plant; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }
}
