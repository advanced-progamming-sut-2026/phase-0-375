package model.item.pushable;

import model.item.GridItem;

public class Piano extends GridItem implements Pushable {
    public Piano(int hp) {
        super(hp);
    }

    public boolean killsOnContact() {
        return true;
    }

    public boolean blocksProjectiles() {
        return true;
    }
}
