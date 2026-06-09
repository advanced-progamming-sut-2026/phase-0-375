package model.plant.instance;

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

    public PlantInstance(Plant definition) {
        this.definition = definition;
        this.state = PlantState.IDLE;
        this.currentHP = definition.getBaseHP();
        this.level = 1;
        this.currentRecharge = definition.getRechargeTime();
        this.isPlantFoodActive = false;
        this.lifespanRemaining = -1;
        this.abilityStates = new EnumMap<>(PlantAbilityType.class);

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
