package model.item;

import model.enums.SunType;

public class Sun {
    private int x;
    private int y;
    private int value;
    private SunType type;

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
}
