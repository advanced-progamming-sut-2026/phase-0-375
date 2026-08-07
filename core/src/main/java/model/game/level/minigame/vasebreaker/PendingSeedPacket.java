package model.game.level.minigame.vasebreaker;

import model.game.map.Point;
import model.plant.definition.Plant;

/**
 * A seed packet that has fallen from a broken vase.
 */
public class PendingSeedPacket {
    private final Plant plant;
    private final Point groundPosition;
    private float timeToExpiry;    // seconds remaining before this packet disappears

    public PendingSeedPacket(Plant plant, Point groundPosition, float timeToExpiry) {
        this.plant = plant;
        this.groundPosition = groundPosition;
        this.timeToExpiry = timeToExpiry;
    }

    // --- Getters ---

    public Plant getPlant() {
        return plant;
    }

    public Point getGroundPosition() {
        return groundPosition;
    }

    public float getTimeToExpiry() {
        return timeToExpiry;
    }

    public boolean isExpired() {
        return timeToExpiry <= 0;
    }

    // --- Setters ---

    public void setTimeToExpiry(float timeToExpiry) {
        this.timeToExpiry = timeToExpiry;
    }

    /** Called every tick */
    public void tick(float deltaTime) {
        if (timeToExpiry > 0) {
            timeToExpiry -= deltaTime;
        }
    }
}
