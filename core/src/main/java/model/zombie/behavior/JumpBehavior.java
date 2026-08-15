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

    /** Matches PAM {@code blastoff}; the body stays put while this plays. */
    public static final float BLASTOFF_DURATION = 0.2667f;

    /** Seconds of {@code fly} after blastoff, before {@code land}. */
    public static final float FLY_DURATION = 0.4f;

    /** Seconds the launch arc takes, during which the zombie is stunned mid-air. */
    public static final float TIME_TO_TRAVEL = 1.5f;

    /** Peak height of the launch arc, in 768-space pixels. */
    public static final float APEX = 250.0f;

    /** Column the zombie is thrown to once the dynamite explodes (the house edge). */
    public static final int LANDING_COLUMN = 0;

    public static final String DYNAMITE_BURNING_01 = "_dynamite_burning_01";
    public static final String DYNAMITE_BURNING_02 = "_dynamite_burning_02";
    public static final String DYNAMITE_BURNING_03 = "_dynamite_burning_03";
    public static final String DYNAMITE_BURNT = "dynamite_burnt";
    public static final String DYNAMITE_EXTINGUISHED = "_dynamite_extinguished";

    public static final String[] DYNAMITE_PARTS = {
            DYNAMITE_BURNING_01, DYNAMITE_BURNING_02, DYNAMITE_BURNING_03,
            DYNAMITE_BURNT, DYNAMITE_EXTINGUISHED};

    // --- State ---

    /** Current phase of the dynamite/jump cycle. */
    private JumpPhase phase = JumpPhase.COUNTDOWN;

    /** Seconds elapsed since spawn while the dynamite is still counting down. */
    private float countdownTimer = 0f;

    /** Seconds elapsed since the launch arc started. */
    private float travelTimer = 0f;

    /** Continuous X at the moment the dynamite exploded. */
    private float launchX = 0f;

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
     * have passed, begins the launch arc. Ice / frost snuffs it.
     */
    private void tickCountdown(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie.isChilled() || zombie.isFrozen()) {
            extinguish();
            return;
        }
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
        launchX = zombie.getContinuousPosition() != null
                ? zombie.getContinuousX()
                : zombie.getGridX();
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
            updateFlightPosition(zombie);
            return;
        }

        land(zombie);
    }

    /** Stay put through blastoff, then lerp X toward the house. */
    private void updateFlightPosition(ZombieInstance zombie) {
        if (travelTimer <= BLASTOFF_DURATION) {
            return;
        }
        float air = TIME_TO_TRAVEL - BLASTOFF_DURATION;
        float t = air <= 0f ? 1f : Math.min(1f, (travelTimer - BLASTOFF_DURATION) / air);
        float x = launchX + (LANDING_COLUMN - launchX) * t;
        zombie.setContinuousX(x);
        int gridX = (int) Math.floor(x);
        if (gridX != zombie.getGridX()) {
            zombie.setGridX(gridX);
        }
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
        int eatCol = zombie.plantColumnAtFacingBorder();
        PlantInstance plantHere = eatCol < 0
                ? null
                : context.getPlantAt(zombie.getGridY(), eatCol);

        if (plantHere == null || plantHere.getCurrentHP() <= 0) {
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

    /** Continuous X where the dynamite exploded; the blast FX sits here. */
    public float getLaunchX() {
        return launchX;
    }

    public PlantInstance getCurrentTarget() {
        return currentTarget;
    }

    /**
     * Fuse layer to show, or {@code null} once the stick has exploded.
     * {@link #DYNAMITE_BURNING_03} is default-visible in the PAM, so callers
     * must hide it when another layer is on.
     */
    public String dynamitePart() {
        if (extinguished) {
            return DYNAMITE_EXTINGUISHED;
        }
        if (phase != JumpPhase.COUNTDOWN) {
            return null;
        }
        float u = countdownTimer / LAUNCH_COUNTDOWN;
        if (u < 0.25f) {
            return DYNAMITE_BURNING_01;
        }
        if (u < 0.5f) {
            return DYNAMITE_BURNING_02;
        }
        if (u < 0.75f) {
            return DYNAMITE_BURNING_03;
        }
        return DYNAMITE_BURNT;
    }

    /**
     * Arc height in 768-space pixels. Zero on the ground and during blastoff.
     */
    public float heightPx() {
        if (phase != JumpPhase.JUMPING || travelTimer <= BLASTOFF_DURATION) {
            return 0f;
        }
        float air = TIME_TO_TRAVEL - BLASTOFF_DURATION;
        if (air <= 0f) {
            return 0f;
        }
        float t = Math.min(1f, (travelTimer - BLASTOFF_DURATION) / air);
        return 4f * APEX * t * (1f - t);
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
