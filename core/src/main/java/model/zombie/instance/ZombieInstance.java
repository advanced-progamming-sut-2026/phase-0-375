package model.zombie.instance;

import model.enums.PlacableLayer;
import model.item.placeable.Placeable;
import model.enums.*;
import model.game.core.Tickable;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.item.pushable.Pushable;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.armor.Armor;
import model.zombie.behavior.*;
import model.zombie.behavior.zombotany.*;
import model.zombie.definition.Zombie;

import java.util.*;

/**
 * The runtime representation of a zombie on the game field
 */
public class ZombieInstance implements Tickable, Placeable {
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
    /** Persists across EATING / SPECIAL_ACTION so hypnosis is not lost when state changes. */
    private boolean hypnotized;

    private List<Armor> armors;                            // instantiated armor pieces
    private Pushable pushableItem;                         // null if not a pusher
    private List<ZombieBehavior> behaviors;                // zombie behaviors

    private float fireDamageMultiplier = 1.0f;             // Multiplier applied to incoming FIRE-elemental damage.

    private PlantInstance eatingTarget;                    // null if this zombie isn't eating any plants
    private ZombieInstance combatTargetZombie;             // The opposing zombie this zombie is currently biting.

    private float chillStackTimer = 0f;
    private float poisonTimer = 0f;
    private int poisonDPS = 0;
    private float burnTimer = 0f;
    private int burnDPS = 0;

    /** The last thing that damaged this zombie (e.g. a projectile). Used by
     *  the Myopoint score level to attribute kills to their sources. */
    private Object lastDamageSource;

    /** Names of plants that damaged this zombie (kill attribution for quests). */
    private final Set<String> plantDamagers = new HashSet<>();

    /** Families of plants that damaged this zombie. */
    private final Set<PlantCategory> plantDamagerFamilies = new HashSet<>();

    /** True if any non-plant source (mower, environment, zombie) damaged this zombie. */
    private boolean nonPlantDamaged = false;

    public ZombieInstance(Zombie definition) {
        this.definition = definition;
        this.state = ZombieState.SPAWNING;
        this.currentHP = definition.getBaseHP();
        this.currentSpeed = definition.getSpeed();
        this.speedModifier = 1.0f;
        this.isGlowing = shouldSpawnGlowing();
        this.chillLevel = 0;
        this.movingBackward = false;
        this.armors = new ArrayList<>();
        this.pushableItem = null;
        this.behaviors = new ArrayList<>();
        this.eatingTarget = null;
        this.hypnotized = false;
        this.fireDamageMultiplier = definition.getFireDamageMultiplier();

        // Add a ZombieBehavior to behaviors for every behavior type on the definition.
        for (ZombieBehaviorType type : definition.getBehaviors()) {
            ZombieBehavior behavior = createBehavior(type);
            if (behavior != null) {
                behaviors.add(behavior);
            }
        }
    }

    public ZombieInstance(Zombie definition, List<Armor> armors, Pushable pushableItem) {
        this(definition);
        this.armors = new ArrayList<>(armors);
        this.pushableItem = pushableItem;
    }

    // --- Glowing ---

    public boolean shouldSpawnGlowing() {
        float glowingChance = 0.1f;
        Random rng = new Random();
        return rng.nextFloat() <= glowingChance;
    }

    // --- Hypnosis helpers ---

    /** @return true if this zombie has been hypnotized. */
    public boolean isHypnotized() {
        return hypnotized || state == ZombieState.HYPNOTIZED;
    }

    /** Flips this zombie to the player's side. */
    public void hypnotise() {
        if (isDead()) return;
        this.hypnotized = true;
        this.movingBackward = true;
        this.eatingTarget = null;
        this.combatTargetZombie = null;
        if (state != ZombieState.SPECIAL_ACTION) {
            this.state = ZombieState.HYPNOTIZED;
        }
    }

    // --- Tick & lifecycle ---

    /**
     * Advances this zombie by one game tick.
     * Moves, eats, or performs active behaviors.
     */
    public void tick(float deltaTime) {
        removeDestroyedArmor();
        if (state == ZombieState.SPAWNING) {
            state = ZombieState.WALKING;
        }
        if (currentHP <= 0 && state != ZombieState.DYING && state != ZombieState.DEAD) {
            state = ZombieState.DYING;
        }
    }

    /**
     * Applies damage to this zombie instance. Damage is first absorbed by armor,
     * then overflow hits the zombie's HP.
     */
    public void takeDamage(int damage) {
        if (damage <= 0 || state == ZombieState.DEAD || state == ZombieState.DYING) {
            return;
        }
        int remaining = damage;
        for (Armor armor : armors) {
            if (armor.isDestroyed()) continue;

            if (armor.isPassesDamageThrough()) {
                // Pass-through pieces absorb damage but do not stop it;
                // remaining armor layers (and then the body) still take the hit.
                armor.takeDamage(remaining);
                continue;
            }

            remaining = armor.takeDamage(remaining);
            if (remaining <= 0) {
                return;
            }
        }
        currentHP -= remaining;
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
        // Reset the per-stack timer each time a new stack is applied.
        chillStackTimer = CHILL_STACK_DURATION;
        if (chillLevel > 0 && chillLevel < 3 && state != ZombieState.CHILLED
                && state != ZombieState.EATING) {
            state = ZombieState.CHILLED;
        }
    }

    /** Default duration (in seconds) of a single chill stack. */
    public static final float CHILL_STACK_DURATION = 5.0f;

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

    // --- Kill attribution (quests) ---

    public void recordPlantDamage(Plant source) {
        if (source == null) { nonPlantDamaged = true; return; }
        if (source.getName() != null) plantDamagers.add(source.getName());
        if (source.getCategory() != null) plantDamagerFamilies.add(source.getCategory());
    }

    public void recordNonPlantDamage() { nonPlantDamaged = true; }
    /** True if a lawn mower dealt the killing damage. */
    private boolean killedByMower;
    public void markKilledByMower() { killedByMower = true; }
    public boolean isKilledByMower() { return killedByMower; }
    public Set<String> getPlantDamagers() { return plantDamagers; }
    public Set<PlantCategory> getPlantDamagerFamilies() { return plantDamagerFamilies; }
    public boolean isNonPlantDamaged() { return nonPlantDamaged; }

    /**
     * Advances status-effect timers by {@code deltaTime} seconds and
     * applies any per-tick damage (poison, burn). Called by
     * {@code CombatSystem} once per tick. Returns the total status
     * damage dealt this tick so the caller can dispatch events.
     */
    public int tickStatusEffects(float deltaTime) {
        if (state == ZombieState.DEAD || state == ZombieState.DYING) return 0;
        int damage = 0;

        // Chill stack expiry.
        if (chillLevel > 0) {
            chillStackTimer -= deltaTime;
            if (chillStackTimer <= 0f) {
                removeChill();
                if (chillLevel > 0) {
                    chillStackTimer = CHILL_STACK_DURATION;
                }
            }
        }

        // Poison damage over time.
        if (poisonTimer > 0f) {
            poisonTimer -= deltaTime;
            int deltaDamage = (int) (poisonDPS * deltaTime);
            if (deltaDamage > 0) {
                takePoisonDamage(deltaDamage);
                damage += deltaDamage;
            }
            if (poisonTimer <= 0f) {
                poisonTimer = 0f;
                poisonDPS = 0;
            }
        }

        // Burn damage over time (only if not immune to fire).
        if (burnTimer > 0f && !isImmuneToFire()) {
            burnTimer -= deltaTime;
            int deltaDamage = (int) (burnDPS * fireDamageMultiplier * deltaTime);
            if (deltaDamage > 0) {
                takeDamage(deltaDamage);
                damage += deltaDamage;
            }
            if (burnTimer <= 0f) {
                burnTimer = 0f;
                burnDPS = 0;
            }
        }
        return damage;
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
        if (isHypnotized()) {
            return;
        }
        for(ZombieBehavior behavior : behaviors) {
            behavior.execute(this, context, deltaTime);
        }
    }

    /**
     * Called when the zombie reaches a plant and starts eating.
     */
    public void startEating(PlantInstance target) {
        this.eatingTarget = target;
        this.combatTargetZombie = null;
        this.state = ZombieState.EATING;
    }

    /**
     * Called when the zombie meets an opposing zombie (one of the two is
     * hypnotized, the other isn't) and starts biting it.
     */
    public void startFightingZombie(ZombieInstance target) {
        this.combatTargetZombie = target;
        this.eatingTarget = null;
        this.state = ZombieState.EATING;
    }

    /**
     * Called when the plant being eaten is destroyed, or when the
     * opposing zombie being fought has died / moved away.
     */
    public void stopEating() {
        this.eatingTarget = null;
        this.combatTargetZombie = null;
        if (state == ZombieState.EATING) {
            state = (hypnotized || movingBackward) ? ZombieState.HYPNOTIZED : ZombieState.WALKING;
        }
    }

    /**
     * @return true if this zombie is eating a plant
     */
    public boolean isEating() { return state == ZombieState.EATING; }
    public boolean isDead() { return state == ZombieState.DEAD || state == ZombieState.DYING; }
    public boolean isAlive() { return currentHP > 0 && !isDead(); }
    public boolean isFrozen() { return chillLevel >= 3; }
    public boolean isChilled() { return chillLevel > 0 && chillLevel < 3; }

    /** @return true if this zombie is currently flying. */
    public boolean isFlying() {
        FlyBehavior flyBehavior = (FlyBehavior) getBehavior(ZombieBehaviorType.FLY);
        return flyBehavior != null && flyBehavior.isFlying();
    }

    /** @return true while this zombie is submerged underwater */
    public boolean isSubmerged() {
        SwimBehavior swimBehavior = (SwimBehavior) getBehavior(ZombieBehaviorType.SWIM);
        return swimBehavior != null && swimBehavior.isSubmerged();
    }

    /** @return true while this zombie is actively pushing a {@link Pushable}. */
    public boolean isPushing() {
        PushBehavior pushBehavior = (PushBehavior) getBehavior(ZombieBehaviorType.PUSH);
        return pushBehavior != null && pushBehavior.isPushing();
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
            case ZOMBOTANY_PEASHOOTER: return new ZombotanyPeashooterBehavior();
            case ZOMBOTANY_JALAPENO: return new ZombotanyJalapenoBehavior();
            case ZOMBOTANY_SQUASH: return new ZombotanySquashBehavior();
            case PRODUCE_SUN: return new ProduceSunBehavior();
            default: return null;
        }
    }

    // --- Getters ---

    public Zombie getDefinition() { return definition; }
    public ZombieState getState() { return state; }
    public int getCurrentHP() { return currentHP; }
    /** @return the last source that damaged this zombie, or null if unknown. */
    public Object getLastDamageSource() { return lastDamageSource; }
    public void setLastDamageSource(Object lastDamageSource) { this.lastDamageSource = lastDamageSource;}
    public FloatPoint getContinuousPosition() { return continuousPosition; }
    public float getContinuousX() { return continuousPosition.getX(); }
    public float getContinuousY() { return continuousPosition.getY(); }
    public Point getGridPosition() { return gridPosition; }
    public int getGridX() { return gridPosition.getX(); }
    public int getGridY() { return gridPosition.getY(); }
    public float getCurrentSpeed() { return currentSpeed; }
    public float getSpeedModifier() { return speedModifier; }
    public boolean isGlowing() { return isGlowing; }
    public int getChillLevel() { return chillLevel; }
    public List<Armor> getArmors() { return armors; }
    public Pushable getPushableItem() { return pushableItem; }
    public List<ZombieBehavior> getBehaviors() { return behaviors; }
    public PlantInstance getEatingTarget() { return eatingTarget; }
    /** @return the opposing zombie this zombie is currently biting, or {@code null}. */
    public ZombieInstance getCombatTargetZombie() { return combatTargetZombie; }

    // --- Setters ---

    public void setDefinition(Zombie definition) { this.definition = definition; }
    public void setState(ZombieState state) { this.state = state; }
    public void setCurrentHP(int currentHP) { this.currentHP = currentHP; }
    public void setContinuousPosition(FloatPoint position) { this.continuousPosition = position; }
    public void setContinuousX(float x) { this.continuousPosition.setX(x); }
    @Override  public PlacableLayer getLayer() { return PlacableLayer.MAIN; }
    public void setGridPosition(Point gridPosition) { this.gridPosition = gridPosition; }
    public void setGridX(int gridX) { this.gridPosition.setX(gridX); }
    public void setPushableItem(Pushable pushableItem) { this.pushableItem = pushableItem; }
    /** @return true if this zombie takes no damage from FIRE-elemental sources. */
    public boolean isImmuneToFire() { return fireDamageMultiplier <= 0f; }
}
