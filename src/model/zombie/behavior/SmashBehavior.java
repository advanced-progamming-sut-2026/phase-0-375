package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

/**
 * Smash behavior.
 */
public class SmashBehavior implements ZombieBehavior {

    // --- Gargantuar constants ---

    /** Damage dealt by each Gargantuar smash. Guaranteed to one-shot any plant. */
    public static final int GARGANTUAR_SMASH_DAMAGE = 6767;

    /** Seconds the Gargantuar spends winding up before the smash lands. */
    public static final float GARGANTUAR_SMASH_DURATION = 2.0f;

    // --- All Star constants ---

    /** Damage dealt by the All Star's first impact. Guaranteed to one-shot any plant. */
    public static final int ALL_STAR_SMASH_DAMAGE = GARGANTUAR_SMASH_DAMAGE;

    /** All star speed modifier that applies to it before its first smash. */
    public static final float ALL_STAR_BEFORE_SMASH_SPEED_MODIFIER = 2.0f;

    /** All star speed modifier that applies to it after its first smash. */
    public static final float ALL_STAR_AFTER_SMASH_SPEED_MODIFIER = 0.5f;

    // --- State ---

    /** Current phase of the Gargantuar smash cycle. */
    private GargantuarPhase gargantuarPhase = GargantuarPhase.WALKING;

    /** Seconds elapsed in the current SMASHING phase. */
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
            case SMASHING:
                tickGargantuarSmashing(zombie, context, deltaTime);
                break;
            default:
                break;
        }
    }

    /**
     * Looks for a plant in zombie's current cell. if one is found starts smashing it.
     */
    private void tickGargantuarWalking(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        PlantInstance plant = context.getPlantAt(zombie.getGridY(), zombie.getGridX());
        if (plant == null || plant.getCurrentHP() <= 0) {
            return;
        }

        currentTarget = plant;
        smashTimer = 0f;
        gargantuarPhase = GargantuarPhase.SMASHING;
        zombie.startEating(plant); // Gargantuar eatDPS is zero so this just makes it stop moving.
    }

    /**
     * Smashes the {@link #currentTarget} plant after {@link #GARGANTUAR_SMASH_DURATION} seconds.
     */
    private void tickGargantuarSmashing(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (currentTarget == null || currentTarget.getCurrentHP() <= 0) {
            abortSmashing(zombie);
            return;
        }

        smashTimer += deltaTime;
        if (smashTimer >= GARGANTUAR_SMASH_DURATION) {
            context.damagePlant(currentTarget, GARGANTUAR_SMASH_DAMAGE);
            abortSmashing(zombie);
        }
    }

    /** Resets smash state and lets the zombie resume walking. */
    private void abortSmashing(ZombieInstance zombie) {
        currentTarget = null;
        smashTimer = 0f;
        gargantuarPhase = GargantuarPhase.WALKING;
        zombie.stopEating();
    }

    // --- All Star ---

    /**
     * On the very first contact with a plant, instantly smashes it
     * and then acts as a regular zombie.
     */
    private void tickAllStar(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (hasSmashedOnce) return;

        PlantInstance plant = context.getPlantAt(zombie.getGridY(), zombie.getGridX());
        if (plant == null || plant.getCurrentHP() <= 0) {
            zombie.applySpeedModifier(ALL_STAR_BEFORE_SMASH_SPEED_MODIFIER);
            return;
        }

        context.damagePlant(plant, ALL_STAR_SMASH_DAMAGE);
        zombie.applySpeedModifier(ALL_STAR_AFTER_SMASH_SPEED_MODIFIER);

        hasSmashedOnce = true;
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
        String lower = name.toLowerCase();
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
     * The two alternating phases of the Gargantuar's smash cycle.
     */
    public enum GargantuarPhase {
        WALKING, // Walking - no plant currently engaged
        SMASHING // Paused in front of a plant, charging the smash
    }
}