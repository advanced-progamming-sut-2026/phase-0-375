package model.game.save;

/** Snapshot of one lawn-mower lane. */
public class MowerSave {
    private int row;
    private boolean present = true;
    private boolean active = true;
    private boolean triggered;
    private boolean sweeping;
    private float xPosition;
    private float transitionElapsed;

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }
    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isTriggered() { return triggered; }
    public void setTriggered(boolean triggered) { this.triggered = triggered; }
    public boolean isSweeping() { return sweeping; }
    public void setSweeping(boolean sweeping) { this.sweeping = sweeping; }
    public float getXPosition() { return xPosition; }
    public void setXPosition(float xPosition) { this.xPosition = xPosition; }
    public float getTransitionElapsed() { return transitionElapsed; }
    public void setTransitionElapsed(float transitionElapsed) { this.transitionElapsed = transitionElapsed; }
}
