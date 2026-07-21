package model.projectile;

import model.game.map.FloatPoint;
import model.game.map.Point;
import model.zombie.instance.ZombieInstance;

/**
 * Abstract base class for all projectiles fired by plants.
 */
public abstract class Projectile {
    /** Elemental affinity of a projectile. */
    public enum Element {
        /** No special element. */
        NONE,
        /** Chills/freezes the target on hit. */
        ICE,
        /** Ignites or relights flame sources on hit. */
        FIRE,
        /** Poison the target on hit. */
        POISON,
        /** Butter. */
        BUTTER,
    }

    /** Damage dealt on hit (before armor absorption). */
    protected int damage;

    /** Continuous world position of the projectile. */
    protected FloatPoint currentPosition;

    /** Grid row (lane) the projectile is traveling along. */
    protected int row;

    /** Horizontal travel direction: +1 = rightward (toward zombies), -1 = leftward (toward plants). */
    protected int direction;

    /** World-units per second. */
    protected float velocity;

    /** True if this projectile pierces through zombies and doesn't get destroyed. */
    protected boolean pierce;

    /** Elemental affinity, preserved across reflection. */
    protected Element element;

    /**
     * Optional homing target. When non-null, the projectile system
     * steers this projectile toward the target each tick (overriding
     * the linear x-axis movement). Used by homing shooters like
     * Caulipower, Electric Blueberry, and Cat-tail.
     */
    protected ZombieInstance homingTarget;

    // --- Constructors ---

    protected Projectile(int damage, FloatPoint position, int row, float velocity) {
        this(damage, position, row, velocity, Element.NONE, +1);
    }

    protected Projectile(int damage, FloatPoint position, int row, float velocity,
                         Element element, int direction) {
        this.damage = damage;
        this.currentPosition = position;
        this.row = row;
        this.velocity = velocity;
        this.pierce = false;
        this.element = element == null ? Element.NONE : element;
        this.direction = direction >= 0 ? +1 : -1;
    }

    // --- Reflection ---

    /**
     * Flips this projectile's travel direction and re-targets it back
     * toward the plants. Called by {@code JuggleBehavior} when a
     * spinning Juggler deflects an incoming projectile.
     */
    public void reflect() {
        this.direction = -this.direction;
    }

    // --- Getters / setters ---

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public FloatPoint getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(FloatPoint currentPosition) {
        this.currentPosition = currentPosition;
    }

    public float getX() {
        return currentPosition == null ? 0f : currentPosition.getX();
    }

    public float getY() {
        return currentPosition == null ? 0f : currentPosition.getY();
    }

    public void setX(float x) {
        if (currentPosition == null) {
            currentPosition = new FloatPoint(x, row);
        } else {
            currentPosition.setX(x);
        }
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    /** @return +1 if the projectile is traveling rightward, -1 if leftward. */
    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction >= 0 ? +1 : -1;
    }

    public float getVelocity() {
        return velocity;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    public boolean pierce() {
        return pierce;
    }

    public void setPierce(boolean pierce) {
        this.pierce = pierce;
    }

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element == null ? Element.NONE : element;
    }

    /** @return true if this projectile carries the {@link Element#ICE} affinity. */
    public boolean isIce() {
        return element == Element.ICE;
    }

    /** @return true if this projectile carries the {@link Element#FIRE} affinity. */
    public boolean isFire() {
        return element == Element.FIRE;
    }

    /** @return true if this projectile carries the {@link Element#POISON} affinity. */
    public boolean isPoison() {
        return element == Element.POISON;
    }

    /** Butter. */
    public boolean isButter() {
        return element == Element.BUTTER;
    }

    /** @return true if this projectile is traveling leftward. */
    public boolean isReflected() {
        return direction < 0;
    }

    // --- Homing ---

    /** @return the zombie this projectile is homing in on, or {@code null}. */
    public ZombieInstance getHomingTarget() {
        return homingTarget;
    }

    /** Sets the zombie this projectile should steer toward. */
    public void setHomingTarget(ZombieInstance target) {
        this.homingTarget = target;
    }

    /** @return true if this projectile is actively homing in on a target. */
    public boolean isHoming() {
        return homingTarget != null && !homingTarget.isDead();
    }
}