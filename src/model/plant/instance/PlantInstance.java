package model.plant.instance;

import model.enums.PlacableLayer;
import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantState;
import model.enums.PlantTags;
import model.game.map.Point;
import model.item.placeable.Placeable;
import model.plant.PlantFactory;
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

    /** Seconds remaining before a frozen plant automatically unfreezes. */
    private float freezeTimer = 0f;

    /** Default duration (in seconds) that a plant stays frozen. */
    public static final float FREEZE_DURATION = 8.0f;

    /**
     * Imitater support: the name of the plant this Imitater is copying.
     * Resolved via {@code PlantFactory} when the transform countdown
     * expires. {@code null} for every non-Imitater plant.
     */
    private String imitateTarget;

    /**
     * Imitater support: seconds remaining before the instance transforms
     * into its {@link #imitateTarget}. {@code -1} means "not an Imitater"
     * or "already transformed".
     */
    private float transformCountdown;

    private int stackCount;

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
        this.stackCount = 1;

        if (definition.getAbilityType() != null) {
            AbilityState abilityState = new AbilityState(definition.getAbilityType());
            // Traps start disarmed; armed state is ticked by the system.
            if (definition.hasTag(PlantTags.TRAP)) {
                abilityState.setArmed(false);
                if (definition.hasTag(PlantTags.CHARGE) && definition.getActionInterval() > 0) {
                    abilityState.setCooldownRemaining(definition.getActionInterval());
                }
            }
            abilityStates.put(definition.getAbilityType(), abilityState);
        }

        // Temporary plants (Puff-shroom, Sea-shroom, etc.) get a finite lifespan.
        if (definition.hasTag(PlantTags.WARM_UP)) {
            // Warm-up plants start growing; lifespan is infinite, but they ramp.
            this.lifespanRemaining = -1f;
        } else if (definition.isShroom() && !isImitater(definition)) {
            // Non-warm-up shrooms (Puff-shroom, Sea-shroom) are temporary
            this.lifespanRemaining = SHROOM_BASE_LIFESPAN;
        }

        // Imitater starts a short countdown before it morphs into its target.
        this.imitateTarget = null;
        this.transformCountdown = -1f;
        if (isImitater(definition)) {
            this.transformCountdown = IMITATER_TRANSFORM_DELAY;
        }
    }

    /** @return true if the given definition represents an Imitater. */
    private static boolean isImitater(Plant def) {
        return def != null
                && def.getCategory() == PlantCategory.MODIFIER
                && def.getAbilityType() == PlantAbilityType.MODIFIER_UTILITY
                && def.getName() != null
                && def.getName().toLowerCase().contains("imitat");
    }

    /** Default delay (in seconds) before an Imitater morphs into its target. */
    private static final float IMITATER_TRANSFORM_DELAY = 1.0f;

    /**
     * Base lifespan (in seconds) for non-warm-up shrooms (Puff-shroom,
     * Sea-shroom) before any LIFESPAN_EXT upgrades are applied.
     */
    private static final float SHROOM_BASE_LIFESPAN = 60.0f;

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

        // When the countdown hits zero the instance morphs into its imitate target.
        tickImitaterTransform(deltaTime);
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
            // Tick down the Chomper's digestion timer. When it reaches
            // zero, the plant exits the digesting phase and is ready to
            // swallow again.
            if (state.isDigesting()) {
                state.setDigestRemaining(Math.max(0f, state.getDigestRemaining() - deltaTime));
                if (state.getDigestRemaining() <= 0f) {
                    state.setDigesting(false);
                    state.setDigestRemaining(0f);
                    state.setCooldownRemaining(0f);
                }
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

        AbilityState state = abilityStates.get(definition.getAbilityType());
        if (state != null && definition.getActionInterval() > 0) {
            // If the ability just entered a digesting phase (Chomper),
            // preserve the digest timer as the cooldown - don't override
            // it with the plant's actionInterval.
            if (state.isDigesting()) {
                return;
            }
            if (definition.getAbilityType() == PlantAbilityType.DELAYED_EXPLOSIVE && state.isArmed()) {
                state.setCooldownRemaining(0);
            } else {
                state.setCooldownRemaining(definition.getActionInterval());
            }
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

        // mutate a private per-instance copy; the shared factory definition stays pristine
        this.definition = new Plant(definition);
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
                // If the plant has an infinite lifespan, this upgrade is a no-op.
                // Otherwise, extend the remaining lifespan by the upgrade value.
                if (lifespanRemaining > 0) {
                    lifespanRemaining += upgrade.getValue();
                } else if (lifespanRemaining < 0 && definition.isShroom()) {
                    // First time the lifespan is set on a shroom: initialize
                    // it to the base value plus the upgrade bonus.
                    lifespanRemaining = SHROOM_BASE_LIFESPAN + upgrade.getValue();
                }
                break;
            case GROWTH_STAGE_MAX_UP:
                AbilityState state = abilityStates.get(definition.getAbilityType());
                if (state != null) {
                    state.setGrowthStage(state.getGrowthStage() + (int) upgrade.getValue());
                }
                break;
            case GROW_TIME_REDUCTION:
                AbilityState prodState = abilityStates.get(PlantAbilityType.PRODUCE_SUN);
                if (prodState != null && prodState.getCooldownRemaining() > 0) {
                    prodState.setCooldownRemaining(
                            Math.max(0f, prodState.getCooldownRemaining() - upgrade.getValue()));
                }
                break;
            case ARM_TIME_REDUCTION:
                // Traps arm faster; reduce the initial arming cooldown.
                AbilityState trapState = abilityStates.get(PlantAbilityType.DELAYED_EXPLOSIVE);
                if (trapState != null && !trapState.isArmed() && trapState.getCooldownRemaining() > 0) {
                    trapState.setCooldownRemaining(
                            Math.max(0f, trapState.getCooldownRemaining() - upgrade.getValue()));
                }
                break;
            case DURATION_EXT:
                // Mint family boost duration extension. Mints are one-shot
                // plants that detonate immediately; this upgrade increases
                // the boost's effective window by extending the plant food
                // duration on affected plants.
                plantFoodDurationRemaining += upgrade.getValue();
                break;
            // The remaining special tags are read live by their owning
            // ability (e.g. DOUBLE_SUN_CHANCE is checked in
            // SunProducerAbility.execute each tick). No permanent state
            // change is needed here.
            default:
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
        freezeTimer = FREEZE_DURATION;
    }

    public void unfreeze() {
        if (!isFrozen()) return;
        state = (stateBeforeFreeze != null) ? stateBeforeFreeze : PlantState.IDLE;
        stateBeforeFreeze = null;
        freezeHitCount = 0;
        freezeTimer = 0f;
    }

    /**
     * Advances the freeze timer by {@code deltaTime} seconds. When the
     * timer reaches zero the plant automatically unfreezes. Called by
     * {@code CombatSystem} once per tick for every plant on the field.
     *
     * @return {@code true} if the plant unfroze this tick
     */
    public boolean tickFreeze(float deltaTime) {
        if (!isFrozen()) return false;
        freezeTimer -= deltaTime;
        if (freezeTimer <= 0f) {
            unfreeze();
            return true;
        }
        return false;
    }

    public float getFreezeTimer() { return freezeTimer; }
    public void setFreezeTimer(float freezeTimer) { this.freezeTimer = freezeTimer; }

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

    // --- Imitater transform ---

    /**
     * Tick the Imitater's transform countdown. When the countdown
     * expires the instance morphs into its {@link #imitateTarget} via
     * {@link #transformIntoImitated()}.
     *
     * @param deltaTime seconds elapsed since the last tick
     * @return {@code true} if the transform fired this tick
     */
    public boolean tickImitaterTransform(float deltaTime) {
        if (transformCountdown < 0f) return false;
        transformCountdown -= deltaTime;
        if (transformCountdown <= 0f) {
            transformCountdown = -1f;
            transformIntoImitated();
            return true;
        }
        return false;
    }

    /**
     * Morphs this instance into the plant named by {@link #imitateTarget}.
     * Resolves the target definition through {@code PlantFactory}, swaps
     * the definition, resets HP / state / recharge, rebuilds the per-ability
     * state map for the new ability type, and invalidates the cached
     * ability strategy so it re-resolves on the next {@link #tick}.
     */
    public void transformIntoImitated() {
        if (imitateTarget == null || imitateTarget.isEmpty()) return;
        Plant newDef = null;
        try {
            newDef = PlantFactory.getDefinition(imitateTarget);
        } catch (IllegalStateException ignored) {
            // PlantFactory not initialized - leave the instance as-is.
        }
        if (newDef == null) return;
        transformInto(newDef);
    }

    /**
     * Swaps this instance's definition to {@code newDefinition} and
     * re-initializes every piece of derived runtime state so the
     * instance behaves exactly like a freshly-placed instance of the
     * new plant (same HP, recharge, ability cooldowns, strategy cache).
     *
     * @param newDefinition the definition to adopt
     */
    public void transformInto(Plant newDefinition) {
        if (newDefinition == null) return;
        this.definition = newDefinition;
        this.state = PlantState.IDLE;
        this.currentHP = newDefinition.getBaseHP();
        this.currentRecharge = newDefinition.getRechargeTime();
        this.isPlantFoodActive = false;
        this.plantFoodDurationRemaining = 0f;
        this.pendingPlantFoodEffect = false;

        // Rebuild ability state for the new ability type.
        this.abilityStates.clear();
        if (newDefinition.getAbilityType() != null) {
            AbilityState fresh = new AbilityState(newDefinition.getAbilityType());
            if (newDefinition.hasTag(PlantTags.TRAP)) {
                fresh.setArmed(false);
            }
            this.abilityStates.put(newDefinition.getAbilityType(), fresh);
        }

        // Force the strategy to re-resolve against the new category.
        this.abilityStrategy = null;

        // The instance is no longer an Imitater.
        this.imitateTarget = null;
        this.transformCountdown = -1f;
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
    public PlacableLayer getLayer() {
        Plant def = definition;
        if (def == null) return PlacableLayer.MAIN;
        boolean stack = def.hasTag(PlantTags.STACK);
        if (!stack) return PlacableLayer.MAIN;
        if (def.hasTag(PlantTags.WATER)) {
            return PlacableLayer.GROUND;
        }
        if (def.getCategory() == PlantCategory.WALL_NUT) {
            return PlacableLayer.OVERLAY;
        }
        return PlacableLayer.MAIN;
    }

    // --- Stack helpers ---

    /** @return current number of stacked heads on this instance (1 if not a stacker). */
    public int getStackCount() { return stackCount; }

    public void setStackCount(int stackCount) {
        this.stackCount = Math.max(1, stackCount);
    }

    /**
     * Adds one head to this instance, capping at the plant's stack limit.
     *
     * @return {@code true} if the head was added. {@code false} if the
     *         instance was already at its stack limit
     */
    public boolean incrementStackCount() {
        if (stackCount >= getStackLimit()) return false;
        stackCount++;
        currentHP += definition.getBaseHP();
        return true;
    }

    /** @return the maximum number of heads this instance can hold. */
    public int getStackLimit() {
        if (definition == null || !definition.hasTag(PlantTags.STACK)) return 1;
        int limit = (int) definition.getAbilityValue();
        return limit > 0 ? limit : 1;
    }

    /** @return {@code true} if this instance can accept one more stacked head. */
    public boolean canStackMore() {
        return stackCount < getStackLimit();
    }

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

    // --- Imitater getters / setters ---

    /** @return the plant name this Imitater is copying, or {@code null}. */
    public String getImitateTarget() { return imitateTarget; }

    /** Sets the plant name this Imitater should morph into. */
    public void setImitateTarget(String imitateTarget) { this.imitateTarget = imitateTarget; }

    /** @return seconds remaining before the Imitater transforms, or {@code -1} if inactive. */
    public float getTransformCountdown() { return transformCountdown; }

    /** Sets the Imitater transform countdown (in seconds). */
    public void setTransformCountdown(float transformCountdown) {
        this.transformCountdown = transformCountdown;
    }
}