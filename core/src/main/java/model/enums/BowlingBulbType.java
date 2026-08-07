package model.enums;

public enum BowlingBulbType {
    CYAN(40, 2.0f, 1),
    BLUE(120, 5.0f, 2),
    ORANGE(180, 10.0f, 3);

    private final int baseDamage;
    private final float baseCooldownSeconds;
    private final int maxBounces;

    BowlingBulbType(int baseDamage, float baseCooldownSeconds, int maxBounces) {
        this.baseDamage = baseDamage;
        this.baseCooldownSeconds = baseCooldownSeconds;
        this.maxBounces = maxBounces;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public float getBaseCooldownSeconds() {
        return baseCooldownSeconds;
    }

    public int getMaxBounces() {
        return maxBounces;
    }

    public BowlingBulbType next() {
        switch (this) {
            case CYAN: return BLUE;
            case BLUE: return ORANGE;
            case ORANGE: return CYAN;
            default: return CYAN;
        }
    }
}
