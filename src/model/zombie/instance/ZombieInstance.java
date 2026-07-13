package model.zombie.instance;

import model.enums.*;
import model.game.core.Tickable;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.item.equippable.Equippable;
import model.item.pushable.Pushable;
import model.plant.instance.PlantInstance;
import model.zombie.armor.Armor;
import model.zombie.behavior.*;
import model.zombie.definition.Zombie;

import java.util.ArrayList;
import java.util.List;

/**
 * The runtime representation of a zombie on the game field
 */
public class ZombieInstance implements Tickable {
    private Zombie definition;
    private ZombieState state;
    private int currentHP;
    private Point gridPosition;                            // grid coordinates on the map; null if not yet on field
    private FloatPoint continuousPosition;
    private float currentSpeed;                            // can be modified by chill, buff, etc.
    private float speedModifier;
    private boolean isGlowing;                             // a glowing zombie drops plant food after dying
    private int chillLevel;
    private boolean movingBackward;                        // true while this zombie moves away from the house

    private List<Armor> armors;                            // instantiated armor pieces
    private Pushable pushableItem;                         // null if not a pusher
    private Equippable equippedItem;                       // null if not equipped
    private List<ZombieBehavior> behaviors;                // zombie behaviors

    /**
     * Multiplier applied to incoming FIRE-elemental damage. Mirrors
     * {@link Zombie#getFireDamageMultiplier()} so combat systems can
     * read it directly off the instance without round-tripping to the
     * definition. Defaults to {@code 1.0f}.
     */
    private float fireDamageMultiplier = 1.0f;

    private PlantInstance eatingTarget;                    // null if this zombie isn't eating any plants

    public ZombieInstance(Zombie definition) {
        this.definition = definition;
        this.state = ZombieState.SPAWNING;
        this.currentHP = definition.getBaseHP();
        this.currentSpeed = definition.getSpeed();
        this.speedModifier = 1.0f;
        this.isGlowing = false;
        this.chillLevel = 0;
        this.movingBackward = false;
        this.armors = new ArrayList<>();
        this.pushableItem = null;
        this.equippedItem = null;
        this.behaviors = new ArrayList<>();
        this.eatingTarget = null;
        this.fireDamageMultiplier = definition.getFireDamageMultiplier();

        // Add a ZombieBehavior to behaviors for every behavior type on the definition.
        for (ZombieBehaviorType type : definition.getBehaviors()) {
            ZombieBehavior behavior = createBehavior(type);
            if (behavior != null) {
                behaviors.add(behavior);
            }
        }
    }

    public ZombieInstance(Zombie definition, List<Armor> armors, Pushable pushableItem, Equippable equippedItem) {
        this(definition);
        this.armors = new ArrayList<>(armors);
        this.pushableItem = pushableItem;
        this.equippedItem = equippedItem;
    }

    // --- Tick & lifecycle ---

    /**
     * Advances this zombie by one game tick.
     * Moves, eats, or performs active behaviors.
     */
    public void tick(float deltaTime) {
        removeDestroyedArmor();
        if (currentHP <= 0 && state != ZombieState.DYING && state != ZombieState.DEAD) {
            state = ZombieState.DYING;
        }
    }

    /**
     * Applies damage to this zombie instance.
     * Damage is first absorbed by armor,
     * then overflow hits the zombie's HP.
     * Triggers reactive behaviors.
     */
    public void takeDamage(int damage) {
        if (damage <= 0 || state == ZombieState.DEAD || state == ZombieState.DYING) {
            return;
        }
        int damageOverflow = damage;
        for(Armor armor : armors) {
            damageOverflow = armor.takeDamage(damageOverflow);

            if(damageOverflow <= 0) {
                return;
            }
        }
        currentHP -= damageOverflow;
    }

    /**
     * Variant of {@link #takeDamage(int)} that respects the zombie's
     * {@link #fireDamageMultiplier}.
     *
     * @return the actual damage dealt after the multiplier was applied
     *         (0 if the zombie is immune to fire).
     */
    public int takeFireDamage(int damage) {
        if (damage <= 0) return 0;
        int scaled = (int) (damage * fireDamageMultiplier);
        if (scaled <= 0) return 0;
        takeDamage(scaled);
        return scaled;
    }

    /** Bypasses all armor. */
    public void takePoisonDamage(int damage) {
        if (damage <= 0 || state == ZombieState.DEAD || state == ZombieState.DYING) {
            return;
        }
        currentHP -= damage;
    }

    /** Applies a chill stack to this zombie. Three stacks freezes it solid */
    public void applyChill() {
        if (isFrozen()) return;
        chillLevel = Math.min(3, chillLevel + 1);
        if (chillLevel > 0 && chillLevel < 3 && state != ZombieState.CHILLED
                && state != ZombieState.EATING) {
            state = ZombieState.CHILLED;
        }
    }

    /**
     * Removes one chill stack from this zombie. Called by the combat
     * system when a chill stack expires.
     */
    public void removeChill() {
        chillLevel = Math.max(0, chillLevel - 1);
        if (chillLevel == 0 && state == ZombieState.CHILLED) {
            state = ZombieState.WALKING;
        }
    }

    /**
     * Notifies every behavior on this zombie that the zombie has died.
     * The ZombieSystem calls this exactly once per zombie, right before
     * removing it from the field.
     */
    public void fireOnDeathBehaviors(BehaviorContext context) {
        for (ZombieBehavior behavior : behaviors) {
            behavior.onZombieDeath(this, context);
        }
    }

    /**
     * Delegates a tick to all behaviors. Each behavior checks its
     * own state and decides whether to act.
     */
    public void tickBehaviors(float deltaTime, BehaviorContext context) {
        for(ZombieBehavior behavior : behaviors) {
            behavior.execute(this, context, deltaTime);
        }
    }

    /**
     * Called when the zombie reaches a plant and starts eating.
     */
    public void startEating(PlantInstance target) {
        this.eatingTarget = target;
        this.state = ZombieState.EATING;
    }

    /**
     * Called when the plant being eaten is destroyed.
     */
    public void stopEating() {
        this.eatingTarget = null;
        if (state == ZombieState.EATING) {
            state = ZombieState.WALKING;
        }
    }

    /**
     * @return true if this zombie is eating a plant
     */
    public boolean isEating() {
        return state == ZombieState.EATING;
    }

    public boolean isDead() {
        return state == ZombieState.DEAD || state == ZombieState.DYING;
    }

    public boolean isAlive() {
        return currentHP > 0 && !isDead();
    }

    public boolean isFrozen() {
        return chillLevel >= 3;
    }

    public boolean isChilled() {
        return chillLevel > 0 && chillLevel < 3;
    }

    /** @return true if this zombie is currently flying. */
    public boolean isFlying() {
        FlyBehavior flyBehavior = (FlyBehavior) getBehavior(ZombieBehaviorType.FLY);
        return flyBehavior != null && flyBehavior.isFlying();
    }

    /**
     * @return true while this zombie is submerged underwater (e.g. a Snorkel
     *         swimming under a water tile). Combat / projectile systems use
     *         this to restrict which damage sources can hit the zombie.
     *         only lobber plants can damage a submerged zombie.
     */
    public boolean isSubmerged() {
        SwimBehavior swimBehavior = (SwimBehavior) getBehavior(ZombieBehaviorType.SWIM);
        return swimBehavior != null && swimBehavior.isSubmerged();
    }

    /**
     * @return true while this zombie is actively pushing a {@link Pushable}.
     *         Combat systems can use this to skip the normal eat-plant loop,
     *         since the pushable itself instantly crushes any plant it touches.
     */
    public boolean isPushing() {
        PushBehavior pushBehavior = (PushBehavior) getBehavior(ZombieBehaviorType.PUSH);
        return pushBehavior != null && pushBehavior.isPushing();
    }

    /**
     * @return true while this zombie is spinning.
     */
    public boolean isSpinning() {
        JuggleBehavior juggleBehavior = (JuggleBehavior) getBehavior(ZombieBehaviorType.JUGGLE);
        return juggleBehavior != null && juggleBehavior.isSpinning();
    }

    /**
     * @return true while this zombie is actively holding up a parasol
     *         that deflects lobbed plant projectiles.
     */
    public boolean isDeflectingLobbed() {
        return hasBehavior(ZombieBehaviorType.DEFLECT_LOBBER);
    }

    /** @return true while this zombie is walking away from the house instead of toward it. */
    public boolean isMovingBackward() {
        return movingBackward;
    }

    /** Reverses (or restores) this zombie's walking direction. */
    public void setMovingBackward(boolean movingBackward) {
        this.movingBackward = movingBackward;
    }

    // --- Speed modifier ---

    /** Applies the {@link #speedModifier} to the {@link #currentSpeed} */
    public void applySpeedModifier(float modifier) {
        this.speedModifier = modifier;
        this.currentSpeed = definition.getSpeed() * speedModifier;
    }

    /** Sets the {@link #currentSpeed} to its default value */
    public void clearSpeedModifier() {
        this.speedModifier = 1.0f;
        this.currentSpeed = definition.getSpeed();
    }

    // --- Armor ---

    /**
     * Adds an armor piece to this zombie.
     */
    public void addArmor(Armor armor) {
        this.armors.add(armor);
    }

    /**
     * Removes all destroyed armor pieces.
     */
    public void removeDestroyedArmor() {
        armors.removeIf(Armor::isDestroyed);
    }

    /**
     * @return total remaining armor health across all pieces
     */
    public int getTotalArmorHealth() {
        int totalHealth = 0;
        for(Armor armor : armors) {
            totalHealth += armor.getCurrentHealth();
        }
        return totalHealth;
    }

    // --- Item interaction ---

    /**
     * Called when the zombie's pushable item is destroyed.
     */
    public void onPushableItemDestroyed() {
        this.pushableItem = null;
    }

    /**
     * Called when the zombie's equipped item is destroyed.
     */
    public void onEquippedItemDestroyed() {
        this.equippedItem = null;
    }

    // --- Behavior access ---

    /**
     * Returns the behavior based on the given {@code type},
     * or null if this zombie doesn't have that behavior.
     */
    public ZombieBehavior getBehavior(ZombieBehaviorType type) {
        for(ZombieBehavior behavior : behaviors) {
            if(behavior.getType() == type) {
                return behavior;
            }
        }
        return null;
    }

    /** Checks whether this zombie has at least one behavior of the given type. */
    public boolean hasBehavior(ZombieBehaviorType type) {
        return getBehavior(type) != null;
    }

    // --- Helpers ---

    /** Creates a new {@link ZombieBehavior} instance based on the given {@code type} */
    private ZombieBehavior createBehavior(ZombieBehaviorType type) {
        switch(type) {
            case SHOOT: return new ShootBehavior();
            case STEAL_SUN: return new StealSunBehavior();
            case JUGGLE: return new JuggleBehavior();
            case DEFLECT_LOBBER: return new DeflectLobberBehavior();
            case SWIM: return new SwimBehavior();
            case FLY: return new FlyBehavior();
            case SUMMON: return new SummonBehavior();
            case BUFF: return new BuffBehavior();
            case TRANSFORM: return new TransformBehavior();
            case FISH: return new FishBehavior();
            case THROW_IMP: return new ThrowImpBehavior();
            case SMASH: return new SmashBehavior();
            case JUMP: return new JumpBehavior();
            case PUSH: return new PushBehavior();
            case ENRAGE: return new EnrageBehavior();
            case PIANO_SWAP: return new PianoSwapBehavior();
            case BARREL_ROLLER: return new BarrelRollerBehavior();
            default: return null;
        }
    }

    // --- Getters ---

    public Zombie getDefinition() {
        return definition;
    }

    public ZombieState getState() {
        return state;
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public FloatPoint getContinuousPosition() {
        return continuousPosition;
    }

    public float getContinuousX() { return continuousPosition.getX(); }

    public float getContinuousY() { return continuousPosition.getY(); }

    public Point getGridPosition() { return gridPosition; }
    public int getGridX() { return gridPosition.getX(); }
    public int getGridY() { return gridPosition.getY(); }

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public float getSpeedModifier() {
        return speedModifier;
    }

    public boolean isGlowing() {
        return isGlowing;
    }

    public int getChillLevel() {
        return chillLevel;
    }

    public List<Armor> getArmors() {
        return armors;
    }

    public Pushable getPushableItem() {
        return pushableItem;
    }

    public Equippable getEquippedItem() {
        return equippedItem;
    }

    public List<ZombieBehavior> getBehaviors() {
        return behaviors;
    }

    public PlantInstance getEatingTarget() {
        return eatingTarget;
    }

    // --- Setters ---

    public void setDefinition(Zombie definition) {
        this.definition = definition;
    }

    public void setState(ZombieState state) {
        this.state = state;
    }

    public void setCurrentHP(int currentHP) {
        this.currentHP = currentHP;
    }

    public void setContinuousPosition(FloatPoint position) {
        this.continuousPosition = position;
    }

    public void setContinuousX(float x) { this.continuousPosition.setX(x); }

    public void setContinuousY(float y) { this.continuousPosition.setY(y); }

    public void setGridPosition(Point gridPosition) {
        this.gridPosition = gridPosition;
    }
    public void setGridX(int gridX) { this.gridPosition.setX(gridX); }
    public void setGridY(int gridY) { this.gridPosition.setY(gridY); }

    public void setCurrentSpeed(float currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public void setSpeedModifier(float speedModifier) {
        this.speedModifier = speedModifier;
    }

    public void setGlowing(boolean glowing) {
        isGlowing = glowing;
    }

    public void setChillLevel(int chillLevel) {
        this.chillLevel = Math.max(0, Math.min(3, chillLevel));
    }

    public void setArmors(List<Armor> armors) {
        this.armors = armors;
    }

    public void setPushableItem(Pushable pushableItem) {
        this.pushableItem = pushableItem;
    }

    public void setEquippedItem(Equippable equippedItem) {
        this.equippedItem = equippedItem;
    }

    public void setBehaviors(List<ZombieBehavior> behaviors) {
        this.behaviors = behaviors;
    }

    public void setEatingTarget(PlantInstance eatingTarget) {
        this.eatingTarget = eatingTarget;
    }

    public void setFireDamageMultiplier(float fireDamageMultiplier) {
        this.fireDamageMultiplier = fireDamageMultiplier;
    }

    // --- Damage modifiers ---

    /** @return multiplier applied to incoming FIRE-elemental damage (0..1). */
    public float getFireDamageMultiplier() {
        return fireDamageMultiplier;
    }

    /** @return true if this zombie takes no damage from FIRE-elemental sources. */
    public boolean isImmuneToFire() {
        return fireDamageMultiplier <= 0f;
    }
}