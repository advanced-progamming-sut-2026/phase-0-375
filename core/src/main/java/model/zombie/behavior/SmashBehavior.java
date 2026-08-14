package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

/**
 * Smash behavior.
 */
public class SmashBehavior implements ZombieBehavior {

    // --- Gargantuar constants ---

    /** Default damage dealt by each Gargantuar smash. Guaranteed to one-shot any plant. */
    public static final int DEFAULT_GARGANTUAR_SMASH_DAMAGE = 6767;

    /** Seconds the Gargantuar spends raising its club ({@code eat} clip). */
    public static final float GARGANTUAR_WINDUP_DURATION = 1.2667f;

    /** Seconds the club takes to come down and be raised again ({@code smash_left} clip). */
    public static final float GARGANTUAR_SMASH_DURATION = 1.7667f;

    // --- All Star constants ---

    /** Damage dealt by the All Star's first impact. Guaranteed to one-shot any plant. */
    public static final int ALL_STAR_SMASH_DAMAGE = DEFAULT_GARGANTUAR_SMASH_DAMAGE;

    /** All star speed modifier that applies to it before its first smash. */
    public static final float ALL_STAR_BEFORE_SMASH_SPEED_MODIFIER = 2.0f;

    /** Default all star speed modifier that applies to it after its first smash. */
    public static final float DEFAULT_ALL_STAR_AFTER_SMASH_SPEED_MODIFIER = 0.5f;

    // --- State ---

    /** Current phase of the Gargantuar smash cycle. */
    private GargantuarPhase gargantuarPhase = GargantuarPhase.WALKING;

    /** Seconds elapsed in the current WINDUP / SMASHING phase. */
    private float smashTimer = 0f;

    /** Plant currently being smashed by the Gargantuar. null when walking. */
    private PlantInstance currentTarget = null;

    /**
     * True once an All Star has performed its initial smash.
     */
    private boolean hasSmashedOnce = false;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) return;

        if (isAllStar(zombie)) {
            tickAllStar(zombie, context, deltaTime);
        } else {
            ThrowImpBehavior toss = (ThrowImpBehavior) zombie.getBehavior(ZombieBehaviorType.THROW_IMP);
            if (toss != null && toss.isThrowing()) {
                return;
            }
            tickGargantuar(zombie, context, deltaTime);
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.SMASH;
    }

    // --- Gargantuar ---

    private void tickGargantuar(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        switch (gargantuarPhase) {
            case WALKING:
                tickGargantuarWalking(zombie, context, deltaTime);
                break;
            case WINDUP:
                tickGargantuarWindup(zombie, context, deltaTime);
                break;
            case SMASHING:
                tickGargantuarSmashing(zombie, deltaTime);
                break;
            default:
                break;
        }
    }

    /**
     * Looks for a plant on the tile whose facing border the zombie has stepped onto;
     * if one is found, starts winding up on it.
     */
    private void tickGargantuarWalking(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        int col = zombie.plantColumnAtFacingBorder();
        if (col < 0 || col >= context.getColumnCount()) {
            return;
        }
        PlantInstance plant = context.getPlantAt(zombie.getGridY(), col);
        if (plant == null || plant.getCurrentHP() <= 0) {
            return;
        }

        currentTarget = plant;
        smashTimer = 0f;
        gargantuarPhase = GargantuarPhase.WINDUP;
        zombie.setState(ZombieState.SPECIAL_ACTION);
    }

    /**
     * Raises the club over the {@link #currentTarget} plant, then smashes it: the plant
     * dies the moment the club starts coming down.
     */
    private void tickGargantuarWindup(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (currentTarget == null || currentTarget.getCurrentHP() <= 0) {
            abortSmashing(zombie);
            return;
        }

        /* The data's "SmashDuration": 2 is longer than the eat clip, which would leave the
           club frozen at the top until it expired. The art sets the pace instead.
           float windupDuration = zombie.getDefinition().getBehaviorPropFloat(
                "SmashDuration", GARGANTUAR_WINDUP_DURATION);
           int smashDamage = zombie.getDefinition().getBehaviorPropInt(
                "SmashDamage", DEFAULT_GARGANTUAR_SMASH_DAMAGE);
           if (smashDamage <= 0) smashDamage = DEFAULT_GARGANTUAR_SMASH_DAMAGE; */
        int smashDamage = DEFAULT_GARGANTUAR_SMASH_DAMAGE;

        smashTimer += deltaTime;
        if (smashTimer >= GARGANTUAR_WINDUP_DURATION) {
            context.damagePlant(currentTarget, smashDamage);
            currentTarget = null;
            smashTimer = 0f;
            gargantuarPhase = GargantuarPhase.SMASHING;
        }
    }

    /** Holds the zombie still until the swing has played out, then lets it walk on. */
    private void tickGargantuarSmashing(ZombieInstance zombie, float deltaTime) {
        smashTimer += deltaTime;
        if (smashTimer >= GARGANTUAR_SMASH_DURATION) {
            abortSmashing(zombie);
        }
    }

    /** Drops an in-progress smash without restoring WALKING — the throw owns the state. */
    public void cancelForThrow() {
        currentTarget = null;
        smashTimer = 0f;
        gargantuarPhase = GargantuarPhase.WALKING;
    }

    /** Resets smash state and lets the zombie resume walking. */
    private void abortSmashing(ZombieInstance zombie) {
        cancelForThrow();
        zombie.setState(ZombieState.WALKING);
    }

    // --- All Star ---

    /**
     * On the very first contact with a plant or hypnotized zombie, instantly
     * smashes it and then crawls at reduced speed like a regular zombie.
     */
    private void tickAllStar(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (hasSmashedOnce) return;

        zombie.applySpeedModifier(ALL_STAR_BEFORE_SMASH_SPEED_MODIFIER);

        int smashDamage = ALL_STAR_SMASH_DAMAGE;
        boolean smashed = false;

        PlantInstance plant = context.getPlantAt(zombie.getGridY(), zombie.getGridX());
        if (plant != null && plant.getCurrentHP() > 0) {
            context.damagePlant(plant, smashDamage);
            smashed = true;
        } else {
            ZombieInstance hypno = findHypnotizedZombieAt(zombie, context);
            if (hypno != null) {
                context.damageZombie(hypno, smashDamage);
                smashed = true;
            }
        }

        if (!smashed) {
            return;
        }

        float afterSmash = zombie.getDefinition().getBehaviorPropFloat(
                "RunningSpeedScale", DEFAULT_ALL_STAR_AFTER_SMASH_SPEED_MODIFIER);
        if (afterSmash <= 0f) afterSmash = DEFAULT_ALL_STAR_AFTER_SMASH_SPEED_MODIFIER;
        zombie.applySpeedModifier(afterSmash);

        hasSmashedOnce = true;
    }

    /** @return a hypnotized zombie sharing this cell, or {@code null}. */
    private ZombieInstance findHypnotizedZombieAt(ZombieInstance zombie, BehaviorContext context) {
        int row = zombie.getGridY();
        int col = zombie.getGridX();
        for (ZombieInstance other : context.getZombiesInLane(row)) {
            if (other == null || other == zombie || other.isDead()) {
                continue;
            }
            if (!other.isHypnotized()) {
                continue;
            }
            if (other.getGridX() == col) {
                return other;
            }
        }
        return null;
    }

    // --- Helpers --

    /** @return true if this zombie is an All Star Zombie. */
    public boolean isAllStar(ZombieInstance zombie) {
        String name = zombie.getDefinition().getName();
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.contains("allstar");
    }

    /** @return true if this zombie is a Gargantuar Zombie. */
    public boolean isGargantuar(ZombieInstance zombie) {
        String name = zombie.getDefinition().getName();
        if (name == null) return false;
        return name.toLowerCase().contains("gargantuar");
    }

    // --- Getters ---

    public GargantuarPhase getGargantuarPhase() {
        return gargantuarPhase;
    }

    public float getSmashTimer() {
        return smashTimer;
    }

    public PlantInstance getCurrentTarget() {
        return currentTarget;
    }

    public boolean hasSmashedOnce() {
        return hasSmashedOnce;
    }

    // --- Inner types ---

    /**
     * The phases of the Gargantuar's smash cycle.
     */
    public enum GargantuarPhase {
        WALKING, // Walking - no plant currently engaged
        WINDUP,  // Paused at a plant's border, raising the club
        SMASHING // Club coming down; the plant is already destroyed
    }
}