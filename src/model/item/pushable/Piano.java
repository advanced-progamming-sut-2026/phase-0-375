package model.item.pushable;

import model.game.map.Point;
import model.item.GridItem;
import model.zombie.instance.ZombieInstance;

/**
 * Piano pushed by the Pianist Zombie.
 */
public class Piano extends GridItem implements Pushable {

    /** The zombie currently pushing this piano; always the Pianist that owns it. */
    private ZombieInstance pusher;

    /** Current grid position of this piano; null until the pusher places it. */
    private Point position;

    public Piano(int hp) {
        super(hp);
    }

    @Override
    public boolean killsOnContact() {
        return true;
    }

    @Override
    public boolean blocksProjectiles() {
        return true;
    }

    /**
     * Pianos are indestructible. Any incoming damage is silently ignored.
     */
    @Override
    public void takeDamage(int damage) {
        // No-op - the piano is bound to the Pianist and cannot be destroyed
        // independently. The Pianist's own HP is what protects the combo.
    }

    /**
     * Pianos never register as destroyed. The {@code PushBehavior} relies
     * on this to keep the Pianist in its PUSHING state for its entire life.
     */
    @Override
    public boolean isDestroyed() {
        return false;
    }

    // --- Pushable callbacks ---

    @Override
    public void push() {
        // No-op: position tracking and crush detection are handled by PushBehavior.
    }

    @Override
    public void onCrushPlant() {
        // Notification hook - no internal state to update.
    }

    @Override
    public void onCrushHypnotizedZombie() {
        // Notification hook - no internal state to update.
    }

    @Override
    public void onDestroyed() {
        // Should never be called for a piano (isDestroyed() is always false),
        // but if it ever is, just clear the pusher reference.
        if (pusher != null) {
            pusher.onPushableItemDestroyed();
            pusher = null;
        }
        position = null;
    }

    @Override
    public ZombieInstance getPusher() {
        return pusher;
    }

    @Override
    public void setPusher(ZombieInstance pusher) {
        this.pusher = pusher;
    }

    @Override
    public Point getPosition() {
        return position;
    }

    @Override
    public void setPosition(Point position) {
        this.position = position;
    }
}