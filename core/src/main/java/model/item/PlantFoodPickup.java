package model.item;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Ground plant-food token. Spawned when a glowing zombie dies; click collects it.
 */
public class PlantFoodPickup {
    private final int x;
    private final int y;
    /** Tile-units from cell centre. */
    private final float offsetX;
    private final float offsetY;

    public PlantFoodPickup(int x, int y) {
        this(x, y,
                (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.8f,
                (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.8f);
    }

    public PlantFoodPickup(int x, int y, float offsetX, float offsetY) {
        this.x = x;
        this.y = y;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }
}
