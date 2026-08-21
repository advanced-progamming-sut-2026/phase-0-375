package model.item;

import model.enums.LootPickupKind;

import java.util.concurrent.ThreadLocalRandom;

/** Ground loot token. Auto-collects via GUI fly-to coin HUD. */
public final class LootPickup {
    private final LootPickupKind kind;
    private final int amount;
    private final int x;
    private final int y;
    private final float offsetX;
    private final float offsetY;

    public LootPickup(LootPickupKind kind, int amount, int x, int y) {
        this(kind, amount, x, y,
            (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.8f,
            (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.8f);
    }

    public LootPickup(LootPickupKind kind, int amount, int x, int y,
                      float offsetX, float offsetY) {
        this.kind = kind;
        this.amount = Math.max(1, amount);
        this.x = x;
        this.y = y;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public LootPickupKind getKind() {
        return kind;
    }

    public int getAmount() {
        return amount;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }
}
