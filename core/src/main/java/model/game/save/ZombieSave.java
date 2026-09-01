package model.game.save;

import model.enums.ZombieState;

import java.util.ArrayList;
import java.util.List;

/** Snapshot of one zombie instance. */
public class ZombieSave {
    private String definitionName;
    private int gridCol;
    private int gridRow;
    private float continuousX;
    private float continuousY;
    private ZombieState state = ZombieState.WALKING;
    private int currentHp;
    private float currentSpeed;
    private float speedModifier = 1f;
    private boolean glowing;
    private int chillLevel;
    private float chillStackTimer;
    private boolean buttered;
    private boolean hypnotized;
    private boolean movingBackward;
    private boolean countsTowardCurrentWave;
    private List<ArmorSave> armors = new ArrayList<>();

    public String getDefinitionName() { return definitionName; }
    public void setDefinitionName(String definitionName) { this.definitionName = definitionName; }
    public int getGridCol() { return gridCol; }
    public void setGridCol(int gridCol) { this.gridCol = gridCol; }
    public int getGridRow() { return gridRow; }
    public void setGridRow(int gridRow) { this.gridRow = gridRow; }
    public float getContinuousX() { return continuousX; }
    public void setContinuousX(float continuousX) { this.continuousX = continuousX; }
    public float getContinuousY() { return continuousY; }
    public void setContinuousY(float continuousY) { this.continuousY = continuousY; }
    public ZombieState getState() { return state; }
    public void setState(ZombieState state) { this.state = state; }
    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }
    public float getCurrentSpeed() { return currentSpeed; }
    public void setCurrentSpeed(float currentSpeed) { this.currentSpeed = currentSpeed; }
    public float getSpeedModifier() { return speedModifier; }
    public void setSpeedModifier(float speedModifier) { this.speedModifier = speedModifier; }
    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }
    public int getChillLevel() { return chillLevel; }
    public void setChillLevel(int chillLevel) { this.chillLevel = chillLevel; }
    public float getChillStackTimer() { return chillStackTimer; }
    public void setChillStackTimer(float chillStackTimer) { this.chillStackTimer = chillStackTimer; }
    public boolean isButtered() { return buttered; }
    public void setButtered(boolean buttered) { this.buttered = buttered; }
    public boolean isHypnotized() { return hypnotized; }
    public void setHypnotized(boolean hypnotized) { this.hypnotized = hypnotized; }
    public boolean isMovingBackward() { return movingBackward; }
    public void setMovingBackward(boolean movingBackward) { this.movingBackward = movingBackward; }
    public boolean isCountsTowardCurrentWave() { return countsTowardCurrentWave; }
    public void setCountsTowardCurrentWave(boolean countsTowardCurrentWave) {
        this.countsTowardCurrentWave = countsTowardCurrentWave;
    }
    public List<ArmorSave> getArmors() { return armors; }
    public void setArmors(List<ArmorSave> armors) {
        this.armors = armors == null ? new ArrayList<>() : armors;
    }
}
