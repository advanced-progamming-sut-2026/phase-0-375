package model.item.pushable;

import model.game.map.Point;
import model.item.GridItem;
import model.item.placeable.Placeable;
import model.zombie.instance.ZombieInstance;

/**
 * Ice block pushed by the Troglobite after claiming a Frostbite ice tile.
 */
public class IceBlock extends GridItem implements Pushable {

    /** The zombie currently pushing this block; null if it has been released. */
    private ZombieInstance pusher;

    /** Current grid position of this block; null until the pusher places it. */
    private Point position;

    /** Frozen occupant taken from the ice tile, or null if the cube was empty. */
    private Placeable containedEntity;

    public IceBlock(int hp) {
        super(hp);
    }

    public Placeable getContainedEntity() {
        return containedEntity;
    }

    public void setContainedEntity(Placeable containedEntity) {
        this.containedEntity = containedEntity;
    }

    @Override
    public boolean killsOnContact() {
        return true;
    }

    @Override
    public boolean blocksProjectiles() {
        return true;
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
