package model.game.save;

import model.enums.LootPickupKind;

/** Snapshot of one loot pickup. */
public class LootSave {
    private LootPickupKind kind;
    private int amount;
    private int x;
    private int y;
    private float offsetX;
    private float offsetY;

    public LootPickupKind getKind() { return kind; }
    public void setKind(LootPickupKind kind) { this.kind = kind; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public float getOffsetX() { return offsetX; }
    public void setOffsetX(float offsetX) { this.offsetX = offsetX; }
    public float getOffsetY() { return offsetY; }
    public void setOffsetY(float offsetY) { this.offsetY = offsetY; }
}
