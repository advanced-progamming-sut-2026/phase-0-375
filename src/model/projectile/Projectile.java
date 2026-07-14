package model.projectile;

import model.game.map.FloatPoint;
import model.game.map.Point;

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

    /** Elemental affinity, preserved across reflection. */
    protected Element element;

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

    /** @return true if this projectile is traveling leftward. */
    public boolean isReflected() {
        return direction < 0;
    }
}