package model.game.save;

import model.enums.PlantState;

import java.util.ArrayList;
import java.util.List;

/** Snapshot of one planted instance. */
public class PlantSave {
    private String definitionName;
    private int row;
    private int col;
    private PlantState state = PlantState.IDLE;
    private int currentHp;
    private int armorHp;
    private int armorMaxHp;
    private int level = 1;
    private float currentRecharge;
    private boolean plantFoodActive;
    private float plantFoodDurationRemaining;
    private float lifespanRemaining = -1f;
    private float lifespanTotal;
    private int stackCount = 1;
    private String imitateTarget;
    private float transformCountdown = -1f;
    private int iceHp;
    private boolean octopusCoating;
    private int freezeHitCount;
    private List<AbilitySave> abilities = new ArrayList<>();

    public String getDefinitionName() { return definitionName; }
    public void setDefinitionName(String definitionName) { this.definitionName = definitionName; }
    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }
    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }
    public PlantState getState() { return state; }
    public void setState(PlantState state) { this.state = state; }
    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }
    public int getArmorHp() { return armorHp; }
    public void setArmorHp(int armorHp) { this.armorHp = armorHp; }
    public int getArmorMaxHp() { return armorMaxHp; }
    public void setArmorMaxHp(int armorMaxHp) { this.armorMaxHp = armorMaxHp; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public float getCurrentRecharge() { return currentRecharge; }
    public void setCurrentRecharge(float currentRecharge) { this.currentRecharge = currentRecharge; }
    public boolean isPlantFoodActive() { return plantFoodActive; }
    public void setPlantFoodActive(boolean plantFoodActive) { this.plantFoodActive = plantFoodActive; }
    public float getPlantFoodDurationRemaining() { return plantFoodDurationRemaining; }
    public void setPlantFoodDurationRemaining(float plantFoodDurationRemaining) {
        this.plantFoodDurationRemaining = plantFoodDurationRemaining;
    }
    public float getLifespanRemaining() { return lifespanRemaining; }
    public void setLifespanRemaining(float lifespanRemaining) { this.lifespanRemaining = lifespanRemaining; }
    public float getLifespanTotal() { return lifespanTotal; }
    public void setLifespanTotal(float lifespanTotal) { this.lifespanTotal = lifespanTotal; }
    public int getStackCount() { return stackCount; }
    public void setStackCount(int stackCount) { this.stackCount = stackCount; }
    public String getImitateTarget() { return imitateTarget; }
    public void setImitateTarget(String imitateTarget) { this.imitateTarget = imitateTarget; }
    public float getTransformCountdown() { return transformCountdown; }
    public void setTransformCountdown(float transformCountdown) { this.transformCountdown = transformCountdown; }
    public int getIceHp() { return iceHp; }
    public void setIceHp(int iceHp) { this.iceHp = iceHp; }
    public boolean isOctopusCoating() { return octopusCoating; }
    public void setOctopusCoating(boolean octopusCoating) { this.octopusCoating = octopusCoating; }
    public int getFreezeHitCount() { return freezeHitCount; }
    public void setFreezeHitCount(int freezeHitCount) { this.freezeHitCount = freezeHitCount; }
    public List<AbilitySave> getAbilities() { return abilities; }
    public void setAbilities(List<AbilitySave> abilities) {
        this.abilities = abilities == null ? new ArrayList<>() : abilities;
    }
}
