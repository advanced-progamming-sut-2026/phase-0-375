package model.item.pushable;

import model.item.GridItem;

public class ArcadeMachine extends GridItem implements Pushable {
    public ArcadeMachine(int hp) {
        super(hp);
    }

    public boolean killsOnContact() {
        return true;
    }

    public boolean blocksProjectiles() {
        return true;
    }
}
