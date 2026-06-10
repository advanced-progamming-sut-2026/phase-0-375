package model.item.pushable;

import model.zombie.instance.ZombieInstance;

/**
 * Interface for items that can be pushed by a special zombie
 */
public interface Pushable {
    /**
     * Called each tick the pushing zombie advances.
     * Moves the pushable forward if the path is clear.
     */
    void push();

    /**
     * Called when this pushable destroys a plant by moving into its tile.
     */
    void onCrushPlant();

    /**
     * Called when this pushable is destroyed.
     * The owning zombie should transition from PUSHING to WALKING.
     */
    void onDestroyed();

    /**
     * @return the zombie that is currently pushing this item
     */
    ZombieInstance getPusher();

    /**
     * @param pusher the zombie that will push this item
     */
    void setPusher(ZombieInstance pusher);
}
