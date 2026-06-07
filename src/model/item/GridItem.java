package model.item;

public abstract class GridItem {
    private int hp;

    public GridItem(int hp) {
        this.hp = hp;
    }

    public int getHp() {
        return hp;
    }

    public void takeDamage(int damage) {
        this.hp -= damage;
    }

    public abstract boolean killsOnContact();

    public abstract boolean blocksProjectiles();
}
