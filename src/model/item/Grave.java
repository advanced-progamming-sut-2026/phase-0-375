package model.item;

import model.enums.PlacableLayer;
import model.item.placeable.Placeable;

/**
 * A gravestone dropped onto the field.
 */
public class Grave extends GridItem implements Placeable {

    /** Default starting HP for a freshly-spawned grave. */
    public static final int DEFAULT_HP = 700;

    public Grave() {
        this(DEFAULT_HP);
    }

    public Grave(int hp) {
        super(hp);
    }

    @Override
    public boolean killsOnContact() {
        return false;
    }

    @Override
    public boolean blocksProjectiles() {
        return true;
    }

    @Override
    public PlacableLayer getLayer() {
        return PlacableLayer.GROUND;
    }
}