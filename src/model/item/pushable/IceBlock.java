package model.item.pushable;

import model.item.GridItem;
import model.zombie.instance.ZombieInstance;

public class IceBlock extends GridItem implements Pushable {
    public IceBlock(int hp) {
        super(hp);
    }

    public boolean killsOnContact() {
        return true;
    }

    public boolean blocksProjectiles() {
        return true;
    }

    @Override
    public void push() {

    }

    @Override
    public void onCrushPlant() {

    }

    @Override
    public void onDestroyed() {

    }

    @Override
    public ZombieInstance getPusher() {
        return null;
    }

    @Override
    public void setPusher(ZombieInstance pusher) {

    }
}
