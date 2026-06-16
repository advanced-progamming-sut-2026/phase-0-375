package model.zombie.instance;

import model.game.core.Tickable;
import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.item.equippable.Equippable;
import model.item.pushable.Pushable;
import model.zombie.armor.Armor;
import model.zombie.behavior.ZombieBehavior;
import model.zombie.definition.Zombie;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The runtime representation of a zombie on the game field
 */
public class ZombieInstance implements Tickable {
    private Zombie definition;
    private ZombieState state;
    private int currentHP;
    private Point gridPosition;                                // grid coordinates on the map; null if not yet on field
    private FloatPoint continuousPosition;
    private float currentSpeed;                            // can be modified by chill, buff, etc.
    private List<Armor> armors;                            // instantiated armor pieces
    private Pushable pushableItem;                         // null if not a pusher
    private Equippable equippedItem;                       // null if not equipped
    private Map<ZombieBehaviorType, BehaviorState> behaviorStates; // per-behavior runtime state
    private boolean hasThrownImp;                          // for Gargantuar: ensures Imp thrown only once

    public ZombieInstance(Zombie definition) {
        this.definition = definition;
        this.state = ZombieState.SPAWNING;
        this.currentHP = definition.getBaseHP();
        this.currentSpeed = definition.getSpeed();
        this.armors = new ArrayList<>();
        this.pushableItem = null;
        this.equippedItem = null;
        this.behaviorStates = new EnumMap<>(ZombieBehaviorType.class);
        this.hasThrownImp = false;

        // Initialize a BehaviorState for every behavior on the definition
        for (ZombieBehavior behavior : definition.getBehaviors()) {
            behaviorStates.put(behavior.getType(), new BehaviorState(behavior.getType()));
        }
    }

    // --- Tick & lifecycle ---

    /**
     * Advances this zombie by one game tick.
     * Moves, eats, or performs active behaviors.
     */
    public void tick(float deltaTime) {}

    /**
     * Applies damage to this zombie instance.
     * Damage is first absorbed by armor,
     * then overflow hits the zombie's HP.
     * May trigger reactive behaviors (e.g. ThrowImp).
     */
    public void takeDamage(int damage) {}

    /**
     * Delegates a tick to all behaviors. Each behavior checks its
     * own state and decides whether to act.
     */
    public void tickBehaviors(float deltaTime) {}

    /**
     * Called when the zombie reaches a plant and starts eating.
     */
    public void startEating() {}

    /**
     * Called when the plant being eaten is destroyed.
     */
    public void stopEating() {}

    // --- Armor ---

    /**
     * Adds an armor piece to this zombie.
     */
    public void addArmor(Armor armor) {}

    /**
     * Removes all destroyed armor pieces.
     */
    public void removeDestroyedArmor() {}

    /**
     * @return total remaining armor health across all pieces
     */
    public int getTotalArmorHealth() { return 0; }

    // --- Item interaction ---

    /**
     * Called when the zombie's pushable item is destroyed.
     * May change the zombie's behavior (e.g. Troglobite walks normally).
     */
    public void onPushableItemDestroyed() {}

    /**
     * Called when the zombie's equipped item is destroyed.
     * May change the zombie's speed or behavior (e.g. Newspaper zombie speeds up).
     */
    public void onEquippedItemDestroyed() {}

    // --- Behavior access ---

    /**
     * Returns the runtime state for the given behavior type,
     * or null if this zombie doesn't have that behavior.
     */
    public BehaviorState getBehaviorState(ZombieBehaviorType type) {
        return behaviorStates.get(type);
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

    public List<Armor> getArmors() {
        return armors;
    }

    public Pushable getPushableItem() {
        return pushableItem;
    }

    public Equippable getEquippedItem() {
        return equippedItem;
    }

    public Map<ZombieBehaviorType, BehaviorState> getBehaviorStates() {
        return behaviorStates;
    }

    public boolean isHasThrownImp() {
        return hasThrownImp;
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

    public void setContinuousY(float x) { this.continuousPosition.setY(x); }

    public void setGridPosition(Point gridPosition) {
        this.gridPosition = gridPosition;
    }
    public void setGridX(int gridX) { this.gridPosition.setX(gridX); }
    public void setGridY(int gridY) { this.gridPosition.setY(gridY); }

    public void setCurrentSpeed(float currentSpeed) {
        this.currentSpeed = currentSpeed;
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

    public void setBehaviorStates(Map<ZombieBehaviorType, BehaviorState> behaviorStates) {
        this.behaviorStates = behaviorStates;
    }

    public void setHasThrownImp(boolean hasThrownImp) {
        this.hasThrownImp = hasThrownImp;
    }
}
