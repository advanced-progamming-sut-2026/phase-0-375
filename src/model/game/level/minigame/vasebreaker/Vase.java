package model.game.level.minigame.vasebreaker;

import model.enums.PlacableLayer;
import model.enums.VaseContent;
import model.game.map.Point;
import model.item.placeable.Placeable;
import model.plant.definition.Plant;
import model.zombie.definition.Zombie;

/**
 * Represents a single vase on the Vase Breaker board.
 */
public class Vase implements Placeable {
    private final Point position;
    private final VaseContent contentType;
    private boolean broken;

    /** Non-null when contentType == ZOMBIE or GIANT_VASE */
    private Zombie hiddenZombie;

    /** Non-null when contentType == SEED_PACKET */
    private Plant hiddenPlant;

    public Vase(Point position, VaseContent contentType) {
        this.position = position;
        this.contentType = contentType;
        this.broken = false;
    }

    // --- Getters ---

    public Point getPosition() {
        return position;
    }

    public VaseContent getContentType() {
        return contentType;
    }

    public boolean isBroken() {
        return broken;
    }

    public Zombie getHiddenZombie() {
        return hiddenZombie;
    }

    public Plant getHiddenPlant() {
        return hiddenPlant;
    }

    @Override
    public PlacableLayer getLayer() {
        return PlacableLayer.GROUND;
    }

    // --- Setters ---

    public void setBroken(boolean broken) {
        this.broken = broken;
    }

    public void setHiddenZombie(Zombie hiddenZombie) {
        this.hiddenZombie = hiddenZombie;
    }

    public void setHiddenPlant(Plant hiddenPlant) {
        this.hiddenPlant = hiddenPlant;
    }
}
