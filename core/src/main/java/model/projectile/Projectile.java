package model.projectile;

import model.game.map.FloatPoint;
import model.game.map.Point;
import model.plant.definition.Plant;
import model.zombie.instance.ZombieInstance;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
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

    /**
     * True after {@link #reflect()} sends this projectile back toward plants
     * (Juggler, etc.). Distinct from a plant that simply fires leftward
     * (Split Pea, Rotobaga, Starfruit).
     */
    protected boolean reflected;

    /** World-units per second. */
    protected float velocity;
    protected float yVelocity;

    /** True if this projectile pierces through zombies and doesn't get destroyed. */
    protected boolean pierce;

    /** Zombies this projectile has already damaged. */
    private final Set<ZombieInstance> alreadyHit =
        Collections.newSetFromMap(new IdentityHashMap<>());

    /** Elemental affinity, preserved across reflection. */
    protected Element element;

    /**
     * Optional homing target. When non-null, the projectile system
     * steers this projectile toward the target each tick (overriding
     * the linear x-axis movement). Used by homing shooters like
     * Caulipower, Electric Blueberry, and Cat-tail.
     */
    protected ZombieInstance homingTarget;

    /** Definition of the plant that fired this projectile; null for non-plant sources. */
    protected Plant sourcePlant;

    /**
     * True after a Torchwood has ignited / boosted this pea, so a pea sitting
     * on the same tile is not multiplied every frame.
     */
    protected boolean torchwoodBoosted;

    /** True when Torchwood plant-food restyled this pea as a blue fire pea. */
    protected boolean blueFire;

    /** Optional view art selector  */
    protected int artVariant;

    public Plant getSourcePlant() { return sourcePlant; }

    public void setSourcePlant(Plant sourcePlant) { this.sourcePlant = sourcePlant; }

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
        this.yVelocity = 0f;
        this.pierce = false;
        this.element = element == null ? Element.NONE : element;
        this.direction = direction >= 0 ? +1 : -1;
        this.reflected = false;
    }

    // --- Reflection ---

    /**
     * Flips this projectile's travel direction and re-targets it back
     * toward the plants. Called by {@code JuggleBehavior} when a
     * spinning Juggler deflects an incoming projectile.
     */
    public void reflect() {
        this.direction = -this.direction;
        this.reflected = this.direction < 0;
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

    public void setY(float y) {
        if (currentPosition == null) {
            currentPosition = new FloatPoint(0, y);
        } else {
            currentPosition.setY(y);
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

    public float getYVelocity() {
        return yVelocity;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    public void setYVelocity(float yVelocity) {
        this.yVelocity = yVelocity;
    }

    public boolean pierce() {
        return pierce;
    }

    public void setPierce(boolean pierce) {
        this.pierce = pierce;
    }

    /** @return true if this projectile has already damaged {@code zombie}. */
    public boolean hasAlreadyHit(ZombieInstance zombie) {
        return zombie != null && alreadyHit.contains(zombie);
    }

    /** Records that this projectile has damaged {@code zombie}. */
    public void markHit(ZombieInstance zombie) {
        if (zombie != null) alreadyHit.add(zombie);
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

    /** @return true if Torchwood has already ignited / boosted this projectile. */
    public boolean isTorchwoodBoosted() {
        return torchwoodBoosted;
    }

    public void setTorchwoodBoosted(boolean torchwoodBoosted) {
        this.torchwoodBoosted = torchwoodBoosted;
    }

    /** @return true if this is a Torchwood plant-food blue fire pea. */
    public boolean isBlueFire() {
        return blueFire;
    }

    public void setBlueFire(boolean blueFire) {
        this.blueFire = blueFire;
    }

    /** View-only art selector; {@code 0} = default. */
    public int getArtVariant() {
        return artVariant;
    }

    public void setArtVariant(int artVariant) {
        this.artVariant = artVariant;
    }

    /** @return true if a juggler (or similar) deflected this projectile back toward plants. */
    public boolean isReflected() {
        return reflected;
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
