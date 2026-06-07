package model.item.pushable;

import model.item.GridItem;

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
}
