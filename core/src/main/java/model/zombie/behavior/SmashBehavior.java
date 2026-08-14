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

    /** Charge: 0.75 tiles/s so the 0.67s run clip covers half a tile. */
    public static final float ALL_STAR_BEFORE_SMASH_SPEED_MODIFIER = 4.6875f;

    /** After the charge: Speed itself, so the 3s walk clip covers half a tile. */
    public static final float DEFAULT_ALL_STAR_AFTER_SMASH_SPEED_MODIFIER = 1.0f;

    /** Seconds the All-Star spends in the {@code tackle} clip. */
    public static final float ALL_STAR_TACKLE_DURATION = 1.3f;

    /** Seconds the All-Star spends in the {@code kick} clip. */
    public static final float ALL_STAR_KICK_DURATION = 1.6f;

    /** Seconds into {@code kick} when the plant or hypnotized zombie dies. */
    public static final float ALL_STAR_KICK_IMPACT_AT = 0.53f;

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

    /** Current phase of the All-Star charge. */
    private AllStarPhase allStarPhase = AllStarPhase.RUNNING;

    /** Hypnotized zombie locked in for the All-Star kick; null when the target is a plant. */
    private ZombieInstance currentHypnoTarget = null;

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
     * Charges until the first plant or hypnotized zombie, plays {@code tackle} then
     * {@code kick}, one-shots at 0.53s of kick, then crawls at reduced speed.
     */
    private void tickAllStar(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        switch (allStarPhase) {
            case RUNNING:
                tickAllStarRunning(zombie, context);
                break;
            case TACKLING:
                tickAllStarTackling(deltaTime);
                break;
            case KICKING:
                tickAllStarKicking(zombie, context, deltaTime);
                break;
            default:
                break;
        }
    }

    private void tickAllStarRunning(ZombieInstance zombie, BehaviorContext context) {
        zombie.applySpeedModifier(ALL_STAR_BEFORE_SMASH_SPEED_MODIFIER);

        int col = zombie.plantColumnAtFacingBorder();
        if (col < 0 || col >= context.getColumnCount()) {
            return;
        }

        PlantInstance plant = context.getPlantAt(zombie.getGridY(), col);
        if (plant != null && plant.getCurrentHP() > 0) {
            currentTarget = plant;
        } else {
            ZombieInstance hypno = findHypnotizedZombieAt(zombie, context, col);
            if (hypno == null) {
                return;
            }
            currentHypnoTarget = hypno;
        }

        smashTimer = 0f;
        allStarPhase = AllStarPhase.TACKLING;
        zombie.setState(ZombieState.SPECIAL_ACTION);
    }

    private void tickAllStarTackling(float deltaTime) {
        smashTimer += deltaTime;
        if (smashTimer >= ALL_STAR_TACKLE_DURATION) {
            smashTimer = 0f;
            allStarPhase = AllStarPhase.KICKING;
        }
    }

    private void tickAllStarKicking(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        smashTimer += deltaTime;
        if ((currentTarget != null || currentHypnoTarget != null)
                && smashTimer >= ALL_STAR_KICK_IMPACT_AT) {
            dealAllStarImpact(context);
        }
        if (smashTimer >= ALL_STAR_KICK_DURATION) {
            finishAllStarCharge(zombie);
        }
    }

    private void dealAllStarImpact(BehaviorContext context) {
        if (currentTarget != null && currentTarget.getCurrentHP() > 0) {
            context.damagePlant(currentTarget, ALL_STAR_SMASH_DAMAGE);
        }
        if (currentHypnoTarget != null && !currentHypnoTarget.isDead()) {
            context.damageZombie(currentHypnoTarget, ALL_STAR_SMASH_DAMAGE);
        }
        currentTarget = null;
        currentHypnoTarget = null;
    }

    private void finishAllStarCharge(ZombieInstance zombie) {
        float afterSmash = zombie.getDefinition().getBehaviorPropFloat(
                "RunningSpeedScale", DEFAULT_ALL_STAR_AFTER_SMASH_SPEED_MODIFIER);
        if (afterSmash <= 0f) afterSmash = DEFAULT_ALL_STAR_AFTER_SMASH_SPEED_MODIFIER;
        zombie.applySpeedModifier(afterSmash);
        hasSmashedOnce = true;
        allStarPhase = AllStarPhase.WALKING;
        smashTimer = 0f;
        currentTarget = null;
        currentHypnoTarget = null;
        zombie.setState(ZombieState.WALKING);
    }

    /** @return a hypnotized zombie in {@code col}, or {@code null}. */
    private ZombieInstance findHypnotizedZombieAt(ZombieInstance zombie, BehaviorContext context, int col) {
        int row = zombie.getGridY();
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

    public AllStarPhase getAllStarPhase() {
        return allStarPhase;
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

    /**
     * The phases of the All-Star's opening charge.
     */
    public enum AllStarPhase {
        RUNNING,  // Sprinting; looking for the first plant or hypnotized zombie
        TACKLING, // {@code tackle} clip; target is still alive
        KICKING,  // {@code kick} clip; impact at {@link #ALL_STAR_KICK_IMPACT_AT}
        WALKING   // Charge spent; crawls and eats like a regular zombie
    }
}