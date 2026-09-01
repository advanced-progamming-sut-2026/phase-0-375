package model.plant.instance;

import model.enums.PlacableLayer;
import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantState;
import model.enums.PlantTags;
import model.app.App;
import model.game.map.Cell;
import model.game.map.Point;
import model.item.placeable.Placeable;
import model.plant.PlantFactory;
import model.plant.ability.PlantAbility;
import model.plant.ability.PlantAbilityContext;
import model.plant.ability.TimedPlantAction;
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
    private int armorHP;
    private int armorMaxHP;
    private int reflectDamageBonus;
    private boolean armorExplodesOnBreak;
    private boolean pendingArmorExplosion;
    private int armorBreakEpoch;
    private boolean deathDetonated;
    private int level;                                 // 1-4
    private Point position;                            // grid (col, row); null if not yet placed
    private float currentRecharge;                     // seconds remaining before the seed is available again
    private boolean isPlantFoodActive;
    private float lifespanRemaining;                   // -1 = infinite
    private float lifespanTotal;                       // >0 when a finite stay was scheduled (mints)
    private float plantFoodDurationRemaining;
    private boolean pendingPlantFoodEffect;
    private PlantAction activeAction; // Multi-tick ability sequence (attack anim, windup, …); at most one.
    private int actionEpoch; // Bumped when a presentation action starts so the view can restart clips
    private final Map<PlantAbilityType, AbilityState> abilityStates; // Per-ability runtime state
    private int freezeHitCount;
    private PlantState stateBeforeFreeze;
    private PlantState stateBeforeTransform;
    private int iceHp = 0;
    public static final int DEFAULT_ICE_HP = 600;
    private boolean octopusCoating;
    private String imitateTarget; // Imitater support: null for non-Imitaters
    private float transformCountdown; // Imitater support: -1 means already transformed
    private int stackCount;
    private PlantAbility abilityStrategy; // Cached ability strategy

    public PlantInstance(Plant definition) {
        this.definition = definition;
        this.state = PlantState.IDLE;
        this.currentHP = definition.getBaseHP();
        this.armorHP = 0;
        this.armorMaxHP = 0;
        this.reflectDamageBonus = 0;
        this.armorExplodesOnBreak = false;
        this.pendingArmorExplosion = false;
        this.armorBreakEpoch = 0;
        this.deathDetonated = false;
        this.level = 1;
        this.currentRecharge = definition.getRechargeTime();
        this.isPlantFoodActive = false;
        this.plantFoodDurationRemaining = 0f;
        this.pendingPlantFoodEffect = false;
        this.activeAction = null;
        this.actionEpoch = 0;
        this.lifespanRemaining = -1f;
        this.lifespanTotal = 0f;
        this.abilityStates = new EnumMap<>(PlantAbilityType.class);
        this.freezeHitCount = 0;
        this.stateBeforeFreeze = null;
        this.stateBeforeTransform = null;
        this.stackCount = 1;
        if (definition.getAbilityType() != null) {
            AbilityState abilityState = new AbilityState(definition.getAbilityType());
            // Traps start disarmed; CHARGE mines wait actionInterval to arm.
            if (definition.hasTag(PlantTags.TRAP)) {
                abilityState.setArmed(false);
                if (definition.hasTag(PlantTags.CHARGE) && definition.getActionInterval() > 0) {
                    abilityState.setCooldownRemaining(definition.getActionInterval());
                    this.state = PlantState.ARMING;
                }
            } else if (definition.hasTag(PlantTags.CHARGE)
                    && definition.getCategory() == PlantCategory.SHOOTER
                    && definition.getActionInterval() >= 5f) {
                abilityState.setCooldownRemaining(definition.getActionInterval());
                this.state = PlantState.ARMING;
            }
            abilityStates.put(definition.getAbilityType(), abilityState);
            if (definition.getAbilityType() == PlantAbilityType.PRODUCE_SUN
                    && definition.getActionInterval() > 0f) {
                abilityState.setCooldownRemaining(definition.getActionInterval());
            }
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
            if (this.currentHP <= 0) {
                this.currentHP = 300;
            }
        }
        if (isMint(definition)) {
            if (this.currentHP <= 0) {
                this.currentHP = MINT_BASE_HP;
            }
            float stay = definition.getAbilityValue();
            if (stay <= 0f) {
                stay = MINT_DEFAULT_DURATION;
            }
            this.lifespanRemaining = stay;
            this.lifespanTotal = stay;
        }
    }

    /** @return true if the given definition represents an Imitater. */
    public static boolean isImitater(Plant def) {
        return def != null
                && def.getCategory() == PlantCategory.MODIFIER
                && def.getAbilityType() == PlantAbilityType.MODIFIER_UTILITY
                && def.getName() != null
                && def.getName().toLowerCase().contains("imitat");
    }

    public static final float IMITATER_TRANSFORM_DELAY = 1.5f; // Matches IMITATER attack clip
    public static final float SHROOM_BASE_LIFESPAN = 60.0f; // Base lifespan for non-warm-up shrooms.
    public static final float PLANT_FOOD_DURATION = 5.0f;
    public static final int MINT_BASE_HP = 300;
    public static final float MINT_DEFAULT_DURATION = 10.0f;

    public static boolean isMint(Plant def) {
        return def != null && def.getAbilityType() == PlantAbilityType.MINT_FAMILY_BOOST;
    }

    // --- Tick ---
    /** Advances this plant by one game tick. */
    public void tick(float deltaTime, PlantAbilityContext context) {
        if (state == PlantState.DYING || state == PlantState.FROZEN
                || state == PlantState.TRANSFORMED) {
            return;
        }
        if (tickImitaterTransform(deltaTime)) {
            return;
        }
        tickRecharge(deltaTime);
        tickPlantFood(deltaTime);
        tickLifespan(deltaTime, context);
        if (state == PlantState.DYING) {
            return;
        }
        tickAbilityCooldowns(deltaTime);
        PlantAbility ticking = getAbilityStrategy();
        if (ticking != null) {
            ticking.tick(this, deltaTime);
        }
        if (pendingPlantFoodEffect) {
            pendingPlantFoodEffect = false;
            firePlantFoodEffect(context);
        }
        if (activeAction != null) {
            if (activeAction.tick(this, context, deltaTime)) {
                activeAction = null;
            }
        } else if (canAct()) {
            startAbility(context);
        }
    }

    /**
     * Seed-packet recharge decay multiplier = dl/3.
     * Ability {@code actionInterval} is not scaled — that value is already
     * in-game seconds (and {@link model.game.core.PvZGameLoop} applies game speed).
     */
    private static float difficultyRechargeScale() {
        var model = App.getInstance().getCurrentGameModel();
        return model == null ? 1f : model.difficultyBoost();
    }

    private void tickRecharge(float deltaTime) {
        if (currentRecharge > 0) {
            float scale = difficultyRechargeScale();
            currentRecharge = Math.max(0, currentRecharge - deltaTime * scale);
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
            if (state.isArmed()) {
                state.addArmedElapsed(deltaTime);
            }
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
        if (state == PlantState.STUNNED || transformCountdown >= 0f ||
            state == PlantState.PLANT_FOOD) return false;
        if (isMint(definition) && mintBoostConsumed()) {
            return false;
        }
        Plant def = definition;
        if (def.getActionInterval() <= 0) {
            return true;
        }
        AbilityState state = abilityStates.get(def.getAbilityType());
        return state == null || state.getCooldownRemaining() <= 0;
    }

    private void startAbility(PlantAbilityContext context) {
        PlantAbility strategy = getAbilityStrategy();
        if (strategy == null) return;

        PlantAction action = strategy.beginAction(this, context);
        if (action != null) {
            applyAbilityCooldown(strategy);
            activeAction = action;
            activeAction.start(this, context);
        }
        if (isMint(definition)) {
            markMintBoostConsumed();
        }
    }

    private boolean mintBoostConsumed() {
        AbilityState state = abilityStates.get(definition.getAbilityType());
        return state != null && state.isArmed();
    }

    private void markMintBoostConsumed() {
        AbilityState state = abilityStates.get(definition.getAbilityType());
        if (state != null) {
            state.setArmed(true);
        }
    }

    private void applyAbilityCooldown(PlantAbility strategy) {
        AbilityState state = abilityStates.get(definition.getAbilityType());
        if (state == null || definition.getActionInterval() <= 0) {
            return;
        }
        if (state.isDigesting()) {
            return;
        }
        if (definition.getAbilityType() == PlantAbilityType.DELAYED_EXPLOSIVE && state.isArmed()) {
            state.setCooldownRemaining(0);
            return;
        }
        float custom = strategy.getNextActionCooldown(this);
        state.setCooldownRemaining(custom >= 0f
                ? Math.max(0f, custom)
                : definition.getActionInterval());
    }

    /** Interrupts any multi-tick ability sequence. */
    private void cancelActiveAction() {
        if (activeAction == null) return;
        PlantAction action = activeAction;
        activeAction = null;
        action.cancel(this);
    }

    /** Called by {@link PlantAction} implementations when a presentation clip should restart. */
    public void bumpActionEpoch() {
        actionEpoch++;
    }

    /** Elapsed seconds of the active {@link TimedPlantAction}. */
    public float getActiveActionElapsed() {
        if (activeAction instanceof TimedPlantAction timed) {
            return timed.getElapsed();
        }
        return 0f;
    }

    // --- Plant food ---
    /** No-arg overload for callers that don't have a {@link PlantAbilityContext} handy. */
    public void activatePlantFood() {
        if (!definition.hasPlantFood()) return;
        isPlantFoodActive = true;
        cancelActiveAction();
        stateBeforeFreeze = state;
        state = PlantState.PLANT_FOOD;
        plantFoodDurationRemaining = PLANT_FOOD_DURATION;
        pendingPlantFoodEffect = true;
    }

    /** Activates the plant-food effect for this instance and fires it immediately. */
    public void activatePlantFood(PlantAbilityContext context) {
        if (!definition.hasPlantFood()) return;
        isPlantFoodActive = true;
        cancelActiveAction();
        stateBeforeFreeze = state;
        state = PlantState.PLANT_FOOD;
        plantFoodDurationRemaining = PLANT_FOOD_DURATION;
        pendingPlantFoodEffect = false;
        firePlantFoodEffect(context);
    }

    /** Ends the active plant-food window immediately. */
    public void finishPlantFoodNow() {
        if (!isPlantFoodActive && state != PlantState.PLANT_FOOD) {
            return;
        }
        isPlantFoodActive = false;
        plantFoodDurationRemaining = 0f;
        pendingPlantFoodEffect = false;
        if (state == PlantState.PLANT_FOOD) {
            state = PlantState.IDLE;
        }
    }

    /** Jumps an active plant-food effect into its outro window immediately. */
    public void beginPlantFoodOffWindowNow(float offDurationSeconds) {
        if (!isPlantFoodActive || state != PlantState.PLANT_FOOD || offDurationSeconds <= 0f) {
            return;
        }
        if (plantFoodDurationRemaining > offDurationSeconds) {
            plantFoodDurationRemaining = offDurationSeconds;
        }
    }

    /** Invokes the per-category plant-food effect. */
    private void firePlantFoodEffect(PlantAbilityContext context) {
        if (context == null) return;
        PlantAbility strategy = getAbilityStrategy();
        if (strategy == null) return;
        strategy.onPlantFood(this, context);
    }

    // --- Damage ---

    /** Applies damage to this plant instance. Metal armor absorbs hits first. */
    public void takeDamage(int damage) {
        if (damage <= 0 || state == PlantState.DYING) return;
        if (armorHP > 0) {
            int absorbed = Math.min(armorHP, damage);
            armorHP -= absorbed;
            damage -= absorbed;
            if (armorHP <= 0) {
                armorHP = 0;
                if (armorExplodesOnBreak) {
                    pendingArmorExplosion = true;
                    armorBreakEpoch++;
                }
            }
        }
        if (damage <= 0) return;
        currentHP -= damage;
        if (currentHP <= 0) {
            currentHP = 0;
            cancelActiveAction();
        }
    }

    public void grantArmor(int amount, boolean explodesOnBreak) {
        if (amount <= 0) return;
        armorHP += amount;
        armorMaxHP += amount;
        if (explodesOnBreak) {
            armorExplodesOnBreak = true;
        }
    }

    public void addReflectDamageBonus(int bonus) {
        if (bonus > 0) {
            reflectDamageBonus += bonus;
        }
    }

    /** Fully restores body HP to the (upgrade-adjusted) maximum. */
    public void restoreFullHP() {
        int max = getMaxHP();
        if (currentHP < max) {
            currentHP = max;
        }
    }

    public boolean consumePendingArmorExplosion() {
        if (!pendingArmorExplosion) return false;
        pendingArmorExplosion = false;
        return true;
    }

    /**
     * Marks the body-death explosion as spent. {@code false} if it already fired
     * (so armor-break and death can each blast once, but death is not repeated).
     */
    public boolean markDeathDetonated() {
        if (deathDetonated) return false;
        deathDetonated = true;
        return true;
    }

    // --- Level upgrades ---
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
        int hpDelta = newHP - definition.getBaseHP();
        this.currentHP = Math.min(newHP, this.currentHP + Math.max(0, hpDelta));
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
                if (lifespanRemaining > 0) {
                    lifespanRemaining += upgrade.getValue();
                } else if (lifespanRemaining < 0 && definition.isShroom()) {
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
                AbilityState trapState = abilityStates.get(PlantAbilityType.DELAYED_EXPLOSIVE);
                if (trapState != null && !trapState.isArmed() && trapState.getCooldownRemaining() > 0) {
                    trapState.setCooldownRemaining(
                            Math.max(0f, trapState.getCooldownRemaining() - upgrade.getValue()));
                }
                break;
            case DURATION_EXT:
                plantFoodDurationRemaining += upgrade.getValue();
                if (lifespanRemaining > 0f) {
                    lifespanRemaining += upgrade.getValue();
                    if (lifespanTotal > 0f) {
                        lifespanTotal += upgrade.getValue();
                    }
                }
                break;
            default:
                break;
        }
    }
    // --- Ability strategy resolution ---
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
    public int getFreezeHitCount() { return freezeHitCount; }
    public boolean isChilled() { return !isFrozen() && freezeHitCount > 0; }
    public void registerFreezeHit(int hitsToFreeze) {
        if (isFrozen()) return;
        freezeHitCount++;
        if (freezeHitCount >= hitsToFreeze) freeze();
    }
    public void freeze() {
        freeze(false);
    }

    /** Beach Octopus wrap: same ice rules, tagged so the renderer plays the octopus PAM. */
    public void freezeFromOctopus() {
        freeze(true);
    }

    private void freeze(boolean octopus) {
        if (!isFrozen()) {
            cancelActiveAction();
            stateBeforeFreeze = state;
            state = PlantState.FROZEN;
            iceHp = DEFAULT_ICE_HP;
        }
        if (octopus) {
            octopusCoating = true;
        }
    }

    public void unfreeze() {
        if (!isFrozen()) return;
        state = (stateBeforeFreeze != null) ? stateBeforeFreeze : PlantState.IDLE;
        stateBeforeFreeze = null;
        freezeHitCount = 0;
        iceHp = 0;
        octopusCoating = false;
    }

    public boolean hasOctopusCoating() {
        return octopusCoating;
    }

    /** @return remaining ice/octopus coating HP while frozen. */
    public int getIceHp() {
        return iceHp;
    }

    /**
     * Damages the ice/octopus coating on a frozen plant. When the coating
     * is destroyed the plant thaws (spec: other plants must destroy the ice).
     *
     * @return {@code true} if the plant unfroze as a result
     */
    public boolean damageIce(int damage) {
        if (!isFrozen() || damage <= 0) return false;
        iceHp -= damage;
        if (iceHp <= 0) {
            unfreeze();
            return true;
        }
        return false;
    }

    /**
     * Melts {@code iceDamage} points of ice coating (fiery thaw helper).
     *
     * @return {@code true} if the plant unfroze
     */
    public boolean meltIce(int iceDamage) {
        return damageIce(iceDamage);
    }

    // --- Transform handling (Wizard's cat) ---
    public boolean isTransformed() { return state == PlantState.TRANSFORMED; }
    public void transform() {
        if (isTransformed()) return;
        cancelActiveAction();
        stateBeforeTransform = state;
        state = PlantState.TRANSFORMED;
    }
    public void revertTransform() {
        if (!isTransformed()) return;
        state = (stateBeforeTransform != null) ? stateBeforeTransform : PlantState.IDLE;
        stateBeforeTransform = null;
    }
    // --- Imitater transform ---
    /** Tick the Imitater's transform countdown. */
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
    /** Morphs this instance into the plant named by {@link #imitateTarget}. */
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
    public void transformInto(Plant newDefinition) {
        if (newDefinition == null) return;
        PlacableLayer oldLayer = getLayer();

        this.definition = newDefinition;

        if (position != null && App.getInstance().getCurrentGameModel() != null) {
            Cell cell = App.getInstance().getCurrentGameModel().getCellAt(position.getY(), position.getX());
            if (cell != null) {
                cell.rekeyPlaceable(this, oldLayer);
            }
        }

        this.state = PlantState.IDLE;
        this.currentHP = newDefinition.getBaseHP();
        this.armorHP = 0;
        this.armorMaxHP = 0;
        this.reflectDamageBonus = 0;
        this.armorExplodesOnBreak = false;
        this.pendingArmorExplosion = false;
        this.deathDetonated = false;
        this.currentRecharge = newDefinition.getRechargeTime();
        this.isPlantFoodActive = false;
        this.plantFoodDurationRemaining = 0f;
        this.pendingPlantFoodEffect = false;
        this.activeAction = null;
        this.abilityStates.clear();
        if (newDefinition.getAbilityType() != null) {
            AbilityState fresh = new AbilityState(newDefinition.getAbilityType());
            if (newDefinition.hasTag(PlantTags.TRAP)) {
                fresh.setArmed(false);
            } else if (newDefinition.hasTag(PlantTags.CHARGE)
                    && newDefinition.getCategory() == PlantCategory.SHOOTER
                    && newDefinition.getActionInterval() >= 5f) {
                fresh.setCooldownRemaining(newDefinition.getActionInterval());
            }
            this.abilityStates.put(newDefinition.getAbilityType(), fresh);
        }
        this.abilityStrategy = null;
        this.imitateTarget = null;
        bumpActionEpoch();
        if (newDefinition.isShroom() && !isImitater(newDefinition)) {
            this.lifespanRemaining = SHROOM_BASE_LIFESPAN;
        } else {
            this.lifespanRemaining = -1f;
        }
    }
    // --- Getters ---
    public Plant getDefinition() { return definition; }
    public PlantState getState() { return state; }
    public int getCurrentHP() { return currentHP; }
    public int getMaxHP() { return definition == null ? 0 : Math.max(1, definition.getBaseHP()); }
    public int getArmorHP() { return armorHP; }
    public int getArmorMaxHP() { return armorMaxHP; }
    public boolean hasArmor() { return armorHP > 0; }
    public int getReflectDamageBonus() { return reflectDamageBonus; }
    public boolean armorExplodesOnBreak() { return armorExplodesOnBreak; }
    public int getArmorBreakEpoch() { return armorBreakEpoch; }
    public int getLevel() { return level; }
    public Point getPosition() { return position; }
    public Map<PlantAbilityType, AbilityState> getAbilityStates() { return abilityStates; }
    public AbilityState getAbilityState(PlantAbilityType type) { return abilityStates.get(type); }
    public float getPlantFoodDurationRemaining() { return plantFoodDurationRemaining; }
    public float getLifespanRemaining() { return lifespanRemaining; }
    public float getLifespanTotal() { return lifespanTotal; }
    public boolean isPlantFoodActive() { return isPlantFoodActive; }
    public int getActionEpoch() { return actionEpoch; }
    public boolean hasActiveAction() { return activeAction != null; }

    /**
     * Buried charge mines (Potato Mine, Primal Potato Mine) are walked over
     * until they finish arming — zombies must not chew them in that window.
     */
    public boolean isIgnoredByZombies() {
        if (definition == null
                || !definition.hasTag(PlantTags.CHARGE)
                || !definition.hasTag(PlantTags.TRAP)) {
            return false;
        }
        AbilityState state = abilityStates.get(PlantAbilityType.DELAYED_EXPLOSIVE);
        return state == null || !state.isArmed();
    }
    /** @return true while an Imitater is still counting down to morph. */
    public boolean isImitating() { return transformCountdown >= 0f; }
    /** @return true if this instance is a Hypno-shroom (eaten → hypnotize). */
    public boolean isHypnoShroom() {
        Plant def = definition;
        if (def == null) return false;
        String name = def.getName();
        if (name != null && name.toLowerCase().contains("hypno")) {
            return true;
        }
        return def.getCategory() == PlantCategory.MODIFIER
                && def.hasTag(PlantTags.SHROOM)
                && def.hasTag(PlantTags.MAGIC);
    }
    /** @return the plant name this Imitater will morph into, or {@code null}. */
    public String getImitateTarget() { return imitateTarget; }

    @Override public PlacableLayer getLayer() {
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
    public void setStackCount(int stackCount) { this.stackCount = Math.max(1, stackCount); }
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
    public boolean canStackMore() { return stackCount < getStackLimit(); }
    // --- Setters ---
    public void setDefinition(Plant definition) { this.definition = definition; }
    public void setState(PlantState state) { this.state = state; }
    public void setCurrentHP(int currentHP) { this.currentHP = currentHP; }
    public void setLevel(int level) { this.level = level; }
    public void setPosition(Point position) { this.position = position; }
    public void setCurrentRecharge(float currentRecharge) { this.currentRecharge = currentRecharge; }
    /** Sets the plant name this Imitater should morph into. */
    public void setImitateTarget(String imitateTarget) {
        this.imitateTarget = imitateTarget;
        if (!isImitater(definition) || imitateTarget == null || imitateTarget.isEmpty()) {
            return;
        }
        try {
            Plant target = PlantFactory.getDefinition(imitateTarget);
            if (target != null && target.getBaseHP() > 0) {
                this.currentHP = target.getBaseHP();
            }
        } catch (IllegalStateException ignored) {
            // PlantFactory not initialized yet.
        }
    }

    public void setLifespanRemaining(float lifespanRemaining) { this.lifespanRemaining = lifespanRemaining; }

    public void setLifespanTotal(float lifespanTotal) { this.lifespanTotal = lifespanTotal; }

    public void setArmorHp(int armorHp, int armorMaxHp) {
        this.armorHP = Math.max(0, armorHp);
        this.armorMaxHP = Math.max(this.armorHP, armorMaxHp);
    }

    public void setPlantFoodActive(boolean active, float durationRemaining) {
        this.isPlantFoodActive = active;
        this.plantFoodDurationRemaining = Math.max(0f, durationRemaining);
    }

    public void setTransformCountdown(float transformCountdown) {
        this.transformCountdown = transformCountdown;
    }

    public float getTransformCountdown() {
        return transformCountdown;
    }

    public void setIceHp(int iceHp) {
        this.iceHp = Math.max(0, iceHp);
    }

    public void setOctopusCoating(boolean octopusCoating) {
        this.octopusCoating = octopusCoating;
    }

    public void setFreezeHitCount(int freezeHitCount) {
        this.freezeHitCount = Math.max(0, freezeHitCount);
    }

    public float getCurrentRecharge() {
        return currentRecharge;
    }
}
