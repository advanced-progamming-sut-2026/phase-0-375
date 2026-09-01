package model.network.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SunSnapshotDto {
    private int x;
    private int y;
    private int value;
    private String type;
    private float offsetX;
    private float offsetY;
    private float fallRemaining;
    private float fallDuration;
    private boolean hasOrigin;
    private float originX;
    private float originY;

    public SunSnapshotDto() {}

    public SunSnapshotDto(int x, int y, int value, String type,
                          float offsetX, float offsetY, float fallRemaining, float fallDuration) {
        this(x, y, value, type, offsetX, offsetY, fallRemaining, fallDuration, false, 0f, 0f);
    }

    public SunSnapshotDto(int x, int y, int value, String type,
                          float offsetX, float offsetY, float fallRemaining, float fallDuration,
                          boolean hasOrigin, float originX, float originY) {
        this.x = x;
        this.y = y;
        this.value = value;
        this.type = type;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.fallRemaining = fallRemaining;
        this.fallDuration = fallDuration;
        this.hasOrigin = hasOrigin;
        this.originX = originX;
        this.originY = originY;
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public float getOffsetX() { return offsetX; }
    public void setOffsetX(float offsetX) { this.offsetX = offsetX; }

    public float getOffsetY() { return offsetY; }
    public void setOffsetY(float offsetY) { this.offsetY = offsetY; }

    public float getFallRemaining() { return fallRemaining; }
    public void setFallRemaining(float fallRemaining) { this.fallRemaining = fallRemaining; }

    public float getFallDuration() { return fallDuration; }
    public void setFallDuration(float fallDuration) { this.fallDuration = fallDuration; }

    public boolean isHasOrigin() { return hasOrigin; }
    public void setHasOrigin(boolean hasOrigin) { this.hasOrigin = hasOrigin; }

    public float getOriginX() { return originX; }
    public void setOriginX(float originX) { this.originX = originX; }

    public float getOriginY() { return originY; }
    public void setOriginY(float originY) { this.originY = originY; }
}
