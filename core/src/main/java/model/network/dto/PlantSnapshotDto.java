package model.network.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlantSnapshotDto {
    private String id;
    private String plantName;
    private int row;
    private int col;
    private int currentHP;
    private int maxHP;
    private String state;                // "IDLE", "ATTACKING", "DYING", "EATEN"
    private boolean plantFoodActive;
    private boolean frozen;
    private int stackCount;

    public PlantSnapshotDto() {}

    public PlantSnapshotDto(String id, String plantName, int row, int col,
                            int currentHP, int maxHP, String state,
                            boolean plantFoodActive, boolean frozen, int stackCount) {
        this.id = id;
        this.plantName = plantName;
        this.row = row;
        this.col = col;
        this.currentHP = currentHP;
        this.maxHP = maxHP;
        this.state = state;
        this.plantFoodActive = plantFoodActive;
        this.frozen = frozen;
        this.stackCount = stackCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }

    public int getCurrentHP() { return currentHP; }
    public void setCurrentHP(int currentHP) { this.currentHP = currentHP; }

    public int getMaxHP() { return maxHP; }
    public void setMaxHP(int maxHP) { this.maxHP = maxHP; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public boolean isPlantFoodActive() { return plantFoodActive; }
    public void setPlantFoodActive(boolean plantFoodActive) { this.plantFoodActive = plantFoodActive; }

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    public int getStackCount() { return stackCount; }
    public void setStackCount(int stackCount) { this.stackCount = stackCount; }
}
