package model.zombie;

import model.item.pushable.Pushable;

public class PusherZombie extends Zombie {
    private Pushable pushableItem;

    public PusherZombie(String name, int baseHP, float speed, float eatDPS) {
        super(name, baseHP, speed, eatDPS);
    }

    public Pushable getPushableItem() {
        return pushableItem;
    }

    public void setPushableItem(Pushable pushableItem) {
        this.pushableItem = pushableItem;
    }

    public void push() {}

    public void onItemDestroyed() {}
}
