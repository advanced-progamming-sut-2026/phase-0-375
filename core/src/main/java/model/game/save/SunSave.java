package model.game.save;

import model.enums.SunType;

/** Snapshot of one collectible sun. */
public class SunSave {
    private SunType type = SunType.NORMAL;
    private int value;
    private int x;
    private int y;
    private float offsetX;
    private float offsetY;
    private float fallRemaining;
    private float fallDuration;

    public SunType getType() { return type; }
    public void setType(SunType type) { this.type = type; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public float getOffsetX() { return offsetX; }
    public void setOffsetX(float offsetX) { this.offsetX = offsetX; }
    public float getOffsetY() { return offsetY; }
    public void setOffsetY(float offsetY) { this.offsetY = offsetY; }
    public float getFallRemaining() { return fallRemaining; }
    public void setFallRemaining(float fallRemaining) { this.fallRemaining = fallRemaining; }
    public float getFallDuration() { return fallDuration; }
    public void setFallDuration(float fallDuration) { this.fallDuration = fallDuration; }
}
