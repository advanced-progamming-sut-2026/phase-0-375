package model.item.pushable;

import model.game.map.Point;
import model.item.GridItem;
import model.zombie.instance.ZombieInstance;

/**
 * Arcade machine pushed by the Arcade Zombie.
 */
public class ArcadeMachine extends GridItem implements Pushable {

    /** The zombie currently pushing this machine; null if it has been released. */
    private ZombieInstance pusher;

    /** Current grid position of this machine; null until the pusher places it. */
    private Point position;

    private final int maxHp;

    public ArcadeMachine(int hp) {
        super(hp);
        this.maxHp = hp;
    }

    public int getMaxHp() {
        return maxHp;
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
        // Notify the pusher so it can transition out of the PUSHING state.
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