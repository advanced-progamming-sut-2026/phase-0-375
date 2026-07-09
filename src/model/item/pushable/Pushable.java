package model.item.pushable;

import model.game.map.Point;
import model.zombie.instance.ZombieInstance;

/**
 * Interface for items that can be pushed by a special zombie
 */
public interface Pushable {
    /**
     * Called each tick by {@code PushBehavior} to notify the pushable that
     * its owning zombie advanced. Concrete pushables typically use this
     * for animation/sound hooks; the actual position update is performed
     * by the behavior via {@link #setPosition(Point)}.
     */
    void push();

    /**
     * Called when this pushable destroys a plant by moving into its tile.
     */
    void onCrushPlant();

    /**
     * Called when this pushable destroys a hypnotized zombie by moving
     * into its tile.
     */
    void onCrushHypnotizedZombie();

    /**
     * Called when this pushable is destroyed. The owning zombie should
     * transition from PUSHING to WALKING (see
     * {@link ZombieInstance#onPushableItemDestroyed()}).
     */
    void onDestroyed();

    // --- Pusher reference ---

    /**
     * @return the zombie that is currently pushing this item
     */
    ZombieInstance getPusher();

    /**
     * @param pusher the zombie that will push this item
     */
    void setPusher(ZombieInstance pusher);

    // --- Durability / contact rules ---

    /**
     * @return true if this pushable has been destroyed and should be
     *         removed from the field. Concrete destructible pushables
     *         (ArcadeMachine, IceBlock) inherit this from {@code GridItem};
     *         the indestructible {@code Piano} overrides it to always
     *         return {@code false}.
     */
    boolean isDestroyed();

    /**
     * Applies damage to this pushable. Destructible pushables reduce their
     * HP and may flip {@link #isDestroyed()} to {@code true}; the
     * indestructible {@code Piano} overrides this to a no-op.
     *
     * @param damage amount of damage to apply
     */
    void takeDamage(int damage);

    /**
     * @return true if this pushable instantly kills any plant or
     *         hypnotized zombie on contact.
     */
    boolean killsOnContact();

    /**
     * @return true if projectiles cannot pass through this pushable.
     *         Combat/projectile systems should consult this before
     *         applying damage to anything behind the pushable.
     */
    boolean blocksProjectiles();

    // --- Position tracking ---

    /**
     * @return the current grid position of this pushable, or {@code null}
     *         if it hasn't been placed on the grid yet.
     */
    Point getPosition();

    /**
     * Sets the grid position of this pushable. Called by
     * {@code PushBehavior} each tick to keep the pushable synced with
     * the pushing zombie's position.
     */
    void setPosition(Point position);

    /** Convenience accessor for the pushable's current column. */
    default int getCol() {
        Point point = getPosition();
        return point == null ? -1 : point.getX();
    }

    /** Convenience accessor for the pushable's current row. */
    default int getRow() {
        Point point = getPosition();
        return point == null ? -1 : point.getY();
    }
}