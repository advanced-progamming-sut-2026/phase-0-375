package model.item;

/**
 * Abstract base class for items that exist on the game grid
 */
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

    /**
     * @return true if this item instantly kills a plant on contact
     */
    public abstract boolean killsOnContact();

    /**
     * @return true if this item blocks projectiles from passing through
     */
    public abstract boolean blocksProjectiles();

    /**
     * @return true if this item has been destroyed
     */
    public boolean isDestroyed() {
        return hp <= 0;
    }
}
