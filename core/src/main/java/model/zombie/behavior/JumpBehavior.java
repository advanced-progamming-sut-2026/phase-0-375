package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.plant.instance.PlantInstance;
import model.zombie.instance.ZombieInstance;

/**
 * Jump behavior
 */
public class JumpBehavior implements ZombieBehavior {

    // --- Constants ---

    /** Seconds after spawning until the dynamite explodes. */
    public static final float LAUNCH_COUNTDOWN = 10.0f;

    /** Seconds the launch arc takes, during which the zombie is stunned mid-air. */
    public static final float TIME_TO_TRAVEL = 1.5f;

    /** Peak height of the launch arc. */
    public static final float APEX = 250.0f;

    /** Column the zombie is thrown to once the dynamite explodes (the house edge). */
    public static final int LANDING_COLUMN = 0;

    // --- State ---

    /** Current phase of the dynamite/jump cycle. */
    private JumpPhase phase = JumpPhase.COUNTDOWN;

    /** Seconds elapsed since spawn while the dynamite is still armed. */
    private float countdownTimer = 0f;

    /** Seconds elapsed since the launch arc started. */
    private float travelTimer = 0f;

    /** True once an ice projectile has extinguished the dynamite. */
    private boolean extinguished = false;

    /** True once the dynamite has exploded and the launch has been triggered. */
    private boolean hasLaunched = false;

    /** Plant currently being eaten while walking backward. */
    private PlantInstance currentTarget = null;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead() || extinguished) {
            return;
        }

        switch (phase) {
            case COUNTDOWN:
                tickCountdown(zombie, context, deltaTime);
                break;
            case JUMPING:
                tickJumping(zombie, context, deltaTime);
                break;
            case REVERSED_WALK:
                tickReversedWalk(zombie, context, deltaTime);
                break;
            default:
                break;
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.JUMP;
    }

    // --- Countdown phase ---

    /**
     * Advances the dynamite's fuse. Once {@link #LAUNCH_COUNTDOWN} seconds
     * have passed, begins the launch arc.
     */
    private void tickCountdown(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        countdownTimer += deltaTime;
        if (countdownTimer >= LAUNCH_COUNTDOWN) {
            startJump(zombie);
        }
    }

    /** Begins the launch arc: stuns the zombie midair for {@link #TIME_TO_TRAVEL} seconds. */
    private void startJump(ZombieInstance zombie) {
        phase = JumpPhase.JUMPING;
        travelTimer = 0f;
        hasLaunched = true;
        zombie.setState(ZombieState.SPECIAL_ACTION);
    }

    // --- Jumping phase ---

    /**
     * Holds the zombie stunned midair. once the travel time elapses,
     * teleports it to {@link #LANDING_COLUMN} and reverses its direction.
     */
    private void tickJumping(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        travelTimer += deltaTime;
        if (travelTimer < TIME_TO_TRAVEL) {
            return;
        }

        land(zombie);
    }

    /** Places the zombie at the house edge of its lane and flips its walking direction. */
    private void land(ZombieInstance zombie) {
        int row = zombie.getGridY();

        zombie.setGridX(LANDING_COLUMN);
        zombie.setContinuousPosition(new FloatPoint(LANDING_COLUMN, row));
        zombie.setMovingBackward(true);
        zombie.setState(ZombieState.WALKING);

        phase = JumpPhase.REVERSED_WALK;
    }

    // --- Reversed-walk phase ---

    /**
     * Walks away from the house and eats plants like a regular zombie.
     */
    private void tickReversedWalk(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        PlantInstance plantHere = context.getPlantAt(zombie.getGridY(), zombie.getGridX());

        if (plantHere == null || plantHere.getCurrentHP() <= 0 || plantHere.isIgnoredByZombies()) {
            if (currentTarget != null) {
                currentTarget = null;
                zombie.stopEating();
            }
            return;
        }

        if (currentTarget != plantHere) {
            currentTarget = plantHere;
            zombie.startEating(plantHere);
        }

        if (plantHere.getCurrentHP() <= 0) {
            currentTarget = null;
            zombie.stopEating();
        }
    }

    // --- Reactive triggers ---

    /**
     * Called by the game systems when an ice projectile hits this zombie
     * while its dynamite is still counting down.
     */
    public void extinguish() {
        if (phase == JumpPhase.COUNTDOWN) {
            extinguished = true;
        }
    }

    // --- Getters ---

    public JumpPhase getPhase() {
        return phase;
    }

    public float getCountdownTimer() {
        return countdownTimer;
    }

    public float getTravelTimer() {
        return travelTimer;
    }

    public boolean isExtinguished() {
        return extinguished;
    }

    public boolean hasLaunched() {
        return hasLaunched;
    }

    public PlantInstance getCurrentTarget() {
        return currentTarget;
    }

    // --- Setters ---

    public void setPhase(JumpPhase phase) {
        this.phase = phase;
    }

    public void setCountdownTimer(float countdownTimer) {
        this.countdownTimer = countdownTimer;
    }

    public void setTravelTimer(float travelTimer) {
        this.travelTimer = travelTimer;
    }

    public void setExtinguished(boolean extinguished) {
        this.extinguished = extinguished;
    }

    // --- Inner types ---

    /**
     * The three phases of the Prospector's dynamite/jump cycle.
     */
    public enum JumpPhase {
        COUNTDOWN, // dynamite armed, fuse ticking down
        JUMPING, // midair, traveling to the house edge
        REVERSED_WALK // landed; now walking (and eating) away from the house
    }
}
