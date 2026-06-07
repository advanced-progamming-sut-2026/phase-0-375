package model.zombie;

import model.item.equippable.Equippable;

public class EquippedItemZombie extends  Zombie {
    private Equippable equippedItem;

    public EquippedItemZombie(String name, int baseHP, float speed, float eatDPS) {
        super(name, baseHP, speed, eatDPS);
    }

    public Equippable getEquippedItem() {
        return equippedItem;
    }

    public void setEquippedItem(Equippable equippedItem) {
        this.equippedItem = equippedItem;
    }

    public void useItem() {}

    public void onItemDestroyed() {}
}
