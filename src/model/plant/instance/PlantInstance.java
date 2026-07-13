package model.plant.instance;

import model.enums.PlacableLayer;
import model.enums.PlantAbilityType;
import model.game.map.Point;
import model.item.placeable.Placeable;
import model.enums.PlantState;
import model.plant.ability.PlantAbility;
import model.plant.definition.Plant;

import java.util.EnumMap;
import java.util.Map;

/**
 * The runtime representation of a plant on the game field.
 */
public class PlantInstance implements Placeable {
    private Plant definition;                                   // the blueprint
    private PlantState state;                                   // current state
    private int currentHP;                                      // remaining HP
    private int level;                                          // 1-4; affects stats via level upgrades
    private Point position;                                     // grid coordinates on the map; null if not yet placed
    private float currentRecharge;                              // seconds remaining before the seed is available again
    private boolean isPlantFoodActive;                          // true while plant food effect is running
    private float plantFoodDurationRemaining;                   // seconds left on the plant food effect
    private float lifespanRemaining;                            // for temporary plants (e.g. Puff-shroom); -1 = infinite
    private Map<PlantAbilityType, AbilityState> abilityStates;  // per-ability runtime state
    private int freezeHitCount;                                 // number of ice/snowball hits accumulated toward freezing
    private PlantState stateBeforeFreeze;                       // state to restore to once unfrozen
    private PlantState stateBeforeTransform;                    // state to restore to once un-transformed

    public PlantInstance(Plant definition) {
        this.definition = definition;
        this.state = PlantState.IDLE;
        this.currentHP = definition.getBaseHP();
        this.level = 1;
        this.currentRecharge = definition.getRechargeTime();
        this.isPlantFoodActive = false;
        this.lifespanRemaining = -1;
        this.abilityStates = new EnumMap<>(PlantAbilityType.class);
        this.freezeHitCount = 0;
        this.stateBeforeFreeze = null;
        this.stateBeforeTransform = null;

        // Initialize an AbilityState for every ability on the definition
        for (PlantAbility ability : definition.getAbilities()) {
            abilityStates.put(ability.getType(), new AbilityState(ability.getType()));
        }
    }

    // --- Ability state access ---

    /**
     * Returns the runtime state for the given ability type,
     * or null if this plant doesn't have that ability.
     */
    public AbilityState getAbilityState(PlantAbilityType type) {
        return abilityStates.get(type);
    }

    /**
     * Delegates a tick to all abilities. Each ability checks its
     * own cooldown and decides whether to act.
     */
    public void tick(float deltaTime) {}

    /**
     * Activates the plant food effect for this instance.
     */
    public void activatePlantFood() {}

    /**
     * Applies damage to this plant instance, reducing currentHP.
     * May trigger reactive abilities (e.g. Sun Bean producing sun on hit,
     * Endurian reflecting damage).
     */
    public void takeDamage(int damage) {}

    /**
     * Applies the stat upgrade for the given level (2, 3, or 4).
     */
    public void applyLevelUpgrade(int targetLevel) {}

    // --- Freeze handling ---

    /**
     * @return true if this plant is currently frozen and unable to act.
     */
    public boolean isFrozen() {
        return state == PlantState.FROZEN;
    }

    /**
     * Registers one ice/snowball hit against this plant. Once
     * {@code hitsToFreeze} hits have accumulated the plant becomes
     * {@link PlantState#FROZEN}.
     *
     * @param hitsToFreeze number of hits required before freezing solid
     */
    public void registerFreezeHit(int hitsToFreeze) {
        if (isFrozen()) {
            return;
        }
        freezeHitCount++;
        if (freezeHitCount >= hitsToFreeze) {
            freeze();
        }
    }

    /**
     * Immediately freezes this plant, bypassing the hit-count threshold
     */
    public void freeze() {
        if (isFrozen()) {
            return;
        }
        stateBeforeFreeze = state;
        state = PlantState.FROZEN;
    }

    /**
     * Thaws this plant, restoring the state it had before being frozen
     * and resetting the accumulated freeze-hit counter.
     */
    public void unfreeze() {
        if (!isFrozen()) {
            return;
        }
        state = (stateBeforeFreeze != null) ? stateBeforeFreeze : PlantState.IDLE;
        stateBeforeFreeze = null;
        freezeHitCount = 0;
    }

    public int getFreezeHitCount() {
        return freezeHitCount;
    }

    public void setFreezeHitCount(int freezeHitCount) {
        this.freezeHitCount = freezeHitCount;
    }

    // --- Transform handling (Wizard's cat) ---

    /**
     * @return true if this plant is currently transformed (e.g. into a cat
     * by the Wizard zombie) and can neither attack nor be eaten.
     */
    public boolean isTransformed() {
        return state == PlantState.TRANSFORMED;
    }

    /**
     * Transforms this plant, saving its current state so it can be
     * restored later via {@link #revertTransform()}.
     */
    public void transform() {
        if (isTransformed()) {
            return;
        }
        stateBeforeTransform = state;
        state = PlantState.TRANSFORMED;
    }

    /**
     * Reverts this plant back to the state it had before being
     * transformed (e.g. once the Wizard that transformed it dies).
     */
    public void revertTransform() {
        if (!isTransformed()) {
            return;
        }
        state = (stateBeforeTransform != null) ? stateBeforeTransform : PlantState.IDLE;
        stateBeforeTransform = null;
    }

    // --- Getters ---

    public Plant getDefinition() {
        return definition;
    }

    public PlantState getState() {
        return state;
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public int getLevel() {
        return level;
    }

    public Point getPosition() {
        return position;
    }

    public float getCurrentRecharge() {
        return currentRecharge;
    }

    public boolean isPlantFoodActive() {
        return isPlantFoodActive;
    }

    public float getPlantFoodDurationRemaining() {
        return plantFoodDurationRemaining;
    }

    public float getLifespanRemaining() {
        return lifespanRemaining;
    }

    public Map<PlantAbilityType, AbilityState> getAbilityStates() {
        return abilityStates;
    }

    @Override
    public PlacableLayer getLayer() {
        return  PlacableLayer.MAIN; // returns which layer this instance is going to be placed on
    }

    // --- Setters ---

    public void setDefinition(Plant definition) {
        this.definition = definition;
    }

    public void setState(PlantState state) {
        this.state = state;
    }

    public void setCurrentHP(int currentHP) {
        this.currentHP = currentHP;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public void setCurrentRecharge(float currentRecharge) {
        this.currentRecharge = currentRecharge;
    }

    public void setPlantFoodActive(boolean plantFoodActive) {
        isPlantFoodActive = plantFoodActive;
    }

    public void setPlantFoodDurationRemaining(float plantFoodDurationRemaining) {
        this.plantFoodDurationRemaining = plantFoodDurationRemaining;
    }

    public void setLifespanRemaining(float lifespanRemaining) {
        this.lifespanRemaining = lifespanRemaining;
    }

    public void setAbilityStates(Map<PlantAbilityType, AbilityState> abilityStates) {
        this.abilityStates = abilityStates;
    }
}
