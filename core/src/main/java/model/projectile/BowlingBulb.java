package model.projectile;

import model.enums.BowlingBulbType;
import model.game.map.FloatPoint;
import model.zombie.instance.ZombieInstance;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * A bulb projectile fired by the Bowling Bulb plant.
 */
public class BowlingBulb extends Projectile {

    private BowlingBulbType type;

    /** Remaining deflections. Decremented on every zombie hit. */
    private int bouncesRemaining;

    /** Counter of zombies hit so far. */
    private int hitCount;

    /** {@code true} for the plant-food effect. */
    private boolean explosive;

    /** Zombies already damaged by this bulb. */
    private final Set<ZombieInstance> alreadyHit = Collections.newSetFromMap(new IdentityHashMap<>());

    public BowlingBulb(int damage, FloatPoint position, int row, float velocity,
                       BowlingBulbType type, int maxBounces) {
        super(damage, position, row, velocity);
        this.type = type;
        this.bouncesRemaining = maxBounces;
        this.hitCount = 0;
        this.explosive = false;
    }

    public BowlingBulb(int damage, FloatPoint position, int row, float velocity,
                       Element element, int direction,
                       BowlingBulbType type, int maxBounces) {
        super(damage, position, row, velocity, element, direction);
        this.type = type;
        this.bouncesRemaining = maxBounces;
        this.hitCount = 0;
        this.explosive = false;
    }

    // --- Getters / setters ---

    public BowlingBulbType getType() {
        return type;
    }

    public void setType(BowlingBulbType type) {
        this.type = type;
    }

    public int getBouncesRemaining() {
        return bouncesRemaining;
    }

    public void setBouncesRemaining(int bouncesRemaining) {
        this.bouncesRemaining = Math.max(0, bouncesRemaining);
    }

    public boolean canBounce() {
        return bouncesRemaining > 0;
    }

    public void consumeBounce() {
        if (bouncesRemaining > 0) bouncesRemaining--;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void incrementHitCount() {
        hitCount++;
    }

    public boolean isExplosive() {
        return explosive;
    }

    public void setExplosive(boolean explosive) {
        this.explosive = explosive;
    }

    // --- Already-hit tracking ---

    public boolean hasAlreadyHit(ZombieInstance zombie) {
        return zombie != null && alreadyHit.contains(zombie);
    }

    public void markHit(ZombieInstance zombie) {
        if (zombie != null) alreadyHit.add(zombie);
    }
}
