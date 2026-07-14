package model.plant.instance;

import model.enums.PlacableLayer;
import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantState;
import model.enums.PlantTags;
import model.game.map.Point;
import model.item.placeable.Placeable;
import model.plant.ability.PlantAbility;
import model.plant.ability.PlantAbilityContext;
import model.plant.ability.*;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.definition.PlantLevels;

import java.util.EnumMap;
import java.util.Map;

/**
 * The runtime representation of a plant on the game field.
 */
public class PlantInstance implements Placeable {
    private Plant definition;
    private PlantState state;
    private int currentHP;
    private int level;                                          // 1-4
    private Point position;                                     // grid (col, row); null if not yet placed
    private float currentRecharge;                              // seconds remaining before the seed is available again
    private boolean isPlantFoodActive;
    private float plantFoodDurationRemaining;
    private boolean pendingPlantFoodEffect;                     // true when activatePlantFood() (no-arg) was called and the effect still needs to fire on the next tick
    private float lifespanRemaining;                            // -1 = infinite

    /** Per-ability runtime state. Keyed by ability type from the definition. */
    private final Map<PlantAbilityType, AbilityState> abilityStates;

    private int freezeHitCount;
    private PlantState stateBeforeFreeze;
    private PlantState stateBeforeTransform;

    /** Cached ability strategy. */
    private PlantAbility abilityStrategy;

    public PlantInstance(Plant definition) {
        this.definition = definition;
        this.state = PlantState.IDLE;
        this.currentHP = definition.getBaseHP();
        this.level = 1;
        this.currentRecharge = definition.getRechargeTime();
        this.isPlantFoodActive = false;
        this.plantFoodDurationRemaining = 0f;
        this.pendingPlantFoodEffect = false;
        this.lifespanRemaining = -1f;
        this.abilityStates = new EnumMap<>(PlantAbilityType.class);
        this.freezeHitCount = 0;
        this.stateBeforeFreeze = null;
        this.stateBeforeTransform = null;

        if (definition.getAbilityType() != null) {
            AbilityState abilityState = new AbilityState(definition.getAbilityType());
            // Traps start disarmed; armed state is ticked by the system.
            if (definition.hasTag(PlantTags.TRAP)) {
                abilityState.setArmed(false);
            }
            abilityStates.put(definition.getAbilityType(), abilityState);
        }

        // Temporary plants (Puff-shroom, Sea-shroom, etc.) get a finite lifespan.
        if (definition.hasTag(PlantTags.WARM_UP)) {
            // Warm-up plants start growing; lifespan is infinite, but they ramp.
            this.lifespanRemaining = -1f;
        }
    }

    // --- Tick ---

    /**
     * Advances this plant by one game tick.
     */
    public void tick(float deltaTime, PlantAbilityContext context) {
        if (state == PlantState.DYING || state == PlantState.FROZEN
                || state == PlantState.TRANSFORMED) {
            return;
        }
        tickRecharge(deltaTime);
        tickPlantFood(deltaTime);
        tickLifespan(deltaTime, context);
        tickAbilityCooldowns(deltaTime);

        if (pendingPlantFoodEffect) {
            pendingPlantFoodEffect = false;
            firePlantFoodEffect(context);
        }

        if (canAct()) {
            executeAbility(context);
        }
    }

    private void tickRecharge(float deltaTime) {
        if (currentRecharge > 0) {
            currentRecharge = Math.max(0, currentRecharge - deltaTime);
        }
    }

    private void tickPlantFood(float deltaTime) {
        if (!isPlantFoodActive) return;
        plantFoodDurationRemaining -= deltaTime;
        if (plantFoodDurationRemaining <= 0) {
            isPlantFoodActive = false;
            plantFoodDurationRemaining = 0;
            if (state == PlantState.PLANT_FOOD) {
                state = PlantState.IDLE;
            }
        }
    }

    private void tickLifespan(float deltaTime, PlantAbilityContext context) {
        if (lifespanRemaining < 0) return;
        lifespanRemaining -= deltaTime;
        if (lifespanRemaining <= 0) {
            state = PlantState.DYING;
            context.destroyPlant(this);
        }
    }

    private void tickAbilityCooldowns(float deltaTime) {
        for (AbilityState state : abilityStates.values()) {
            if (state.getCooldownRemaining() > 0) {
                state.setCooldownRemaining(Math.max(0, state.getCooldownRemaining() - deltaTime));
            }
        }
    }

    private boolean canAct() {
        if (state == PlantState.STUNNED) return false;
        Plant def = definition;
        if (def.getActionInterval() <= 0) {
            return true;
        }
        AbilityState state = abilityStates.get(def.getAbilityType());
        return state == null || state.getCooldownRemaining() <= 0;
    }

    private void executeAbility(PlantAbilityContext context) {
        PlantAbility strategy = getAbilityStrategy();
        if (strategy == null) return;
        strategy.execute(this, context);

        // Reset cooldown for recurring abilities
        AbilityState state = abilityStates.get(definition.getAbilityType());
        if (state != null && definition.getActionInterval() > 0) {
            state.setCooldownRemaining(definition.getActionInterval());
        }
    }

    // --- Plant food ---

    /** No-arg overload for callers that don't have a {@link PlantAbilityContext} handy. */
    public void activatePlantFood() {
        if (!definition.hasPlantFood()) return;
        isPlantFoodActive = true;
        stateBeforeFreeze = state;
        state = PlantState.PLANT_FOOD;
        plantFoodDurationRemaining = 5.0f;
        pendingPlantFoodEffect = true;
    }

    /** Activates the plant-food effect for this instance and fires it immediately. */
    public void activatePlantFood(PlantAbilityContext context) {
        if (!definition.hasPlantFood()) return;
        isPlantFoodActive = true;
        stateBeforeFreeze = state;
        state = PlantState.PLANT_FOOD;
        plantFoodDurationRemaining = 5.0f;
        pendingPlantFoodEffect = false;
        firePlantFoodEffect(context);
    }

    /** Invokes the per-category plant-food effect. */
    private void firePlantFoodEffect(PlantAbilityContext context) {
        if (context == null) return;
        PlantAbility strategy = getAbilityStrategy();
        if (strategy == null) return;
        strategy.onPlantFood(this, context);
    }

    // --- Damage ---

    /** Applies damage to this plant instance. */
    public void takeDamage(int damage) {
        if (damage <= 0 || state == PlantState.DYING) return;
        currentHP -= damage;
        if (currentHP <= 0) {
            currentHP = 0;
            state = PlantState.DYING;
        }
    }

    // --- Level upgrades ---

    /**
     * Applies every cumulative level upgrade from level 2 up to
     * {@code targetLevel}.
     */
    public void applyLevelUpgrade(int targetLevel) {
        if (targetLevel <= 1 || definition.getLevels() == null) {
            this.level = Math.max(1, targetLevel);
            return;
        }
        PlantLevels levels = definition.getLevels();
        int newHP = definition.getBaseHP();
        int newDamage = definition.getDamage();
        int newCost = definition.getCost();
        float newRecharge = definition.getRechargeTime();
        float newActionInterval = definition.getActionInterval();

        for (Map.Entry<Integer, LevelUpgrade> entry : levels.cumulativeUpgrades(targetLevel).entrySet()) {
            LevelUpgrade upgrade = entry.getValue();
            if (upgrade == null) continue;
            switch (upgrade.getType()) {
                case BUFF_HP:
                    newHP += (int) upgrade.getValue();
                    break;
                case BUFF_DAMAGE:
                    newDamage += (int) upgrade.getValue();
                    break;
                case BUFF_COST:
                    newCost = Math.max(0, newCost + (int) upgrade.getValue());
                    break;
                case BUFF_RECHARGE:
                    newRecharge = Math.max(0, newRecharge + upgrade.getValue());
                    break;
                case BUFF_ACTION_INTERVAL:
                    newActionInterval = Math.max(0, newActionInterval + upgrade.getValue());
                    break;
                case SPECIAL_MECHANIC:
                    applySpecialMechanic(upgrade);
                    break;
            }
        }

        // Adjust currentHP delta to match the new max.
        int hpDelta = newHP - definition.getBaseHP();
        this.currentHP = Math.min(newHP, this.currentHP + Math.max(0, hpDelta));

        definition.setBaseHP(newHP);
        definition.setDamage(newDamage);
        definition.setCost(newCost);
        definition.setRechargeTime(newRecharge);
        definition.setActionInterval(newActionInterval);

        this.level = targetLevel;
    }

    /** Applies a non-numeric special-mechanic upgrade. */
    private void applySpecialMechanic(LevelUpgrade upgrade) {
        switch (upgrade.getSpecialTag()) {
            case LIFESPAN_EXT:
                if (lifespanRemaining > 0) {
                    lifespanRemaining += upgrade.getValue();
                }
                break;
            case GROWTH_STAGE_MAX_UP:
                AbilityState state = abilityStates.get(definition.getAbilityType());
                if (state != null) {
                    state.setGrowthStage(state.getGrowthStage() + (int) upgrade.getValue());
                }
                break;
            default:
                // Other special tags are read live by their owning system.
                break;
        }
    }

    // --- Ability strategy resolution ---

    /**
     * Lazily resolves the per-category strategy for this plant.
     * Strategies are stateless and shared across all instances.
     */
    public PlantAbility getAbilityStrategy() {
        if (abilityStrategy != null) return abilityStrategy;
        abilityStrategy = createAbilityStrategy(definition.getCategory());
        return abilityStrategy;
    }

    private static PlantAbility createAbilityStrategy(PlantCategory category) {
        if (category == null) return null;
        switch (category) {
            case SUN_PRODUCER: return new SunProducerAbility();
            case SHOOTER: return new ShooterAbility();
            case LOBBER: return new LobberAbility();
            case EXPLOSIVE: return new ExplosiveAbility();
            case MELEE: return new MeleeAbility();
            case WALL_NUT: return new WallAbility();
            case MODIFIER: return new ModifierAbility();
            case HOMING: return new HomingAbility();
            case STRIKE_THROUGH: return new StrikeThroughAbility();
            case MINT: return new MintAbility();
            default: return null;
        }
    }

    // --- Freeze handling ---

    public boolean isFrozen() { return state == PlantState.FROZEN; }

    public void registerFreezeHit(int hitsToFreeze) {
        if (isFrozen()) return;
        freezeHitCount++;
        if (freezeHitCount >= hitsToFreeze) freeze();
    }

    public void freeze() {
        if (isFrozen()) return;
        stateBeforeFreeze = state;
        state = PlantState.FROZEN;
    }

    public void unfreeze() {
        if (!isFrozen()) return;
        state = (stateBeforeFreeze != null) ? stateBeforeFreeze : PlantState.IDLE;
        stateBeforeFreeze = null;
        freezeHitCount = 0;
    }

    // --- Transform handling (Wizard's cat) ---

    public boolean isTransformed() { return state == PlantState.TRANSFORMED; }

    public void transform() {
        if (isTransformed()) return;
        stateBeforeTransform = state;
        state = PlantState.TRANSFORMED;
    }

    public void revertTransform() {
        if (!isTransformed()) return;
        state = (stateBeforeTransform != null) ? stateBeforeTransform : PlantState.IDLE;
        stateBeforeTransform = null;
    }

    // --- Getters ---

    public Plant getDefinition() { return definition; }
    public PlantState getState() { return state; }
    public int getCurrentHP() { return currentHP; }
    public int getLevel() { return level; }
    public Point getPosition() { return position; }
    public float getCurrentRecharge() { return currentRecharge; }
    public boolean isPlantFoodActive() { return isPlantFoodActive; }
    public float getPlantFoodDurationRemaining() { return plantFoodDurationRemaining; }
    public float getLifespanRemaining() { return lifespanRemaining; }
    public Map<PlantAbilityType, AbilityState> getAbilityStates() { return abilityStates; }

    public AbilityState getAbilityState(PlantAbilityType type) {
        return abilityStates.get(type);
    }

    public int getFreezeHitCount() { return freezeHitCount; }

    @Override
    public PlacableLayer getLayer() { return PlacableLayer.MAIN; }

    // --- Setters ---

    public void setDefinition(Plant definition) { this.definition = definition; }
    public void setState(PlantState state) { this.state = state; }
    public void setCurrentHP(int currentHP) { this.currentHP = currentHP; }
    public void setLevel(int level) { this.level = level; }
    public void setPosition(Point position) { this.position = position; }
    public void setCurrentRecharge(float currentRecharge) { this.currentRecharge = currentRecharge; }
    public void setPlantFoodActive(boolean plantFoodActive) { isPlantFoodActive = plantFoodActive; }
    public void setPlantFoodDurationRemaining(float plantFoodDurationRemaining) {
        this.plantFoodDurationRemaining = plantFoodDurationRemaining;
    }

    public void setLifespanRemaining(float lifespanRemaining) {
        this.lifespanRemaining = lifespanRemaining;
    }

    public void setFreezeHitCount(int freezeHitCount) { this.freezeHitCount = freezeHitCount; }
}
