package model.item;

import model.enums.LootType;

public class LootDrop {
    private LootType type;
    private int amount;


    public LootDrop(LootType type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public LootType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }
}
