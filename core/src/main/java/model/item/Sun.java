package model.item;

import model.enums.SunType;

public class Sun {
    private int x;
    private int y;
    private int value;
    private SunType type;
    /** Tile-units from cell centre. Sky drops pick a random point on the tile. */
    private float offsetX;
    private float offsetY;
    private float fallRemaining;
    private float fallDuration;
    /** Tile coords of scatter start. Used when {@link #hasOrigin()}. */
    private float originX;
    private float originY;
    private boolean hasOrigin;

    public Sun(SunType type, int value, int x, int y) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.value = value;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getValue() {
        return value;
    }

    public SunType getType() {
        return type;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void setOffset(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public void setFall(float remaining, float duration) {
        this.fallRemaining = Math.max(0f, remaining);
        this.fallDuration = Math.max(0f, duration);
    }

    public boolean isFalling() {
        return fallRemaining > 0f && fallDuration > 0f;
    }

    public float fallProgress() {
        if (fallDuration <= 0f) {
            return 1f;
        }
        return 1f - Math.max(0f, fallRemaining) / fallDuration;
    }

    public float getFallRemaining() {
        return fallRemaining;
    }

    public float getFallDuration() {
        return fallDuration;
    }

    public void tickFall(float deltaTime) {
        if (!isFalling()) {
            return;
        }
        fallRemaining -= deltaTime;
        if (fallRemaining <= 0f) {
            fallRemaining = 0f;
        }
    }

    /** Scatter/fly start in tile units (Ra death pops from the body). */
    public void setOrigin(float tileX, float tileY) {
        this.originX = tileX;
        this.originY = tileY;
        this.hasOrigin = true;
    }

    public boolean hasOrigin() {
        return hasOrigin;
    }

    public float getOriginX() {
        return originX;
    }

    public float getOriginY() {
        return originY;
    }
}
