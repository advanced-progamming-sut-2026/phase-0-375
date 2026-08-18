package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;
import model.projectile.Pellet;
import model.projectile.Splash;

import java.util.List;

/**
 * Juggle behavior.
 */
public class JuggleBehavior implements ZombieBehavior {

    // --- Constants ---

    /** Default multiplier applied to the zombie's base speed while spinning. */
    public static final float DEFAULT_SPIN_SPEED_MULTIPLIER = 1.1f;

    /** Seconds the zombie keeps spinning after the last projectile was reflected. */
    public static final float SPIN_TIMEOUT = 1.0f;

    /** {@code spinup} clip length in {@code ZOMBIE_DARK_JESTER}. */
    public static final float SPINUP_DURATION = 0.8667f;

    /** {@code spindown} clip length in {@code ZOMBIE_DARK_JESTER}. */
    public static final float SPINDOWN_DURATION = 0.5f;

    /** When true, only {@link Pellet} projectiles are reflected. */
    public static final boolean RESTRICT_TO_JUGGLEABLE = true;

    // --- State ---

    /** Current phase of the juggle cycle. */
    private JugglePhase phase = JugglePhase.IDLE;

    /** Seconds in the current {@link JugglePhase#SPINUP} / {@link JugglePhase#SPINDOWN} clip. */
    private float clipTimer = 0f;

    /** Seconds since the last projectile was reflected while spinning. */
    private float timeSinceLastProjectile = 0f;

    /** Total projectiles reflected by this zombie. */
    private int reflectedCount = 0;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        switch (phase) {
            case IDLE:
                tickIdle(zombie, context, deltaTime);
                break;
            case SPINUP:
            case SPIN:
                tickSpinning(zombie, context, deltaTime);
                break;
            case SPINDOWN:
                tickSpinDown(zombie, context, deltaTime);
                break;
            default:
                break;
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.JUGGLE;
    }

    // --- IDLE phase ---

    private void tickIdle(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        keepWalking(zombie);

        if (findIncomingProjectile(zombie, context) != null) {
            startSpinning(zombie);
            reflectIncomingProjectiles(zombie, context);
        }
    }

    // --- SPINUP / SPIN ---

    private void tickSpinning(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        keepWalking(zombie);

        boolean reflectedAny = reflectIncomingProjectiles(zombie, context);
        if (reflectedAny) {
            timeSinceLastProjectile = 0f;
        }

        if (phase == JugglePhase.SPINUP) {
            clipTimer += deltaTime;
            if (clipTimer >= SPINUP_DURATION) {
                phase = JugglePhase.SPIN;
                clipTimer = 0f;
                timeSinceLastProjectile = 0f;
            }
            return;
        }

        if (!reflectedAny) {
            timeSinceLastProjectile += deltaTime;
            if (timeSinceLastProjectile >= SPIN_TIMEOUT) {
                beginSpinDown(zombie);
            }
        }
    }

    // --- SPINDOWN ---

    private void tickSpinDown(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        keepWalking(zombie);

        if (findIncomingProjectile(zombie, context) != null) {
            startSpinning(zombie);
            reflectIncomingProjectiles(zombie, context);
            return;
        }

        clipTimer += deltaTime;
        if (clipTimer >= SPINDOWN_DURATION) {
            stopSpinning(zombie);
        }
    }

    // --- Reflection logic ---

    /**
     * Finds every incoming juggleable projectile in the zombie's lane
     * that has reached or passed the zombie's column, reflects each one
     * back toward the plants, and returns true if at least one was
     * reflected this tick.
     */
    private boolean reflectIncomingProjectiles(ZombieInstance zombie, BehaviorContext context) {
        int lane = zombie.getGridY();
        int zombieCol = zombie.getGridX();
        float spinSpeed = zombie.getDefinition().getBehaviorPropFloat(
                "MoveSpeedMultiplierWhileJuggling", DEFAULT_SPIN_SPEED_MULTIPLIER);
        if (spinSpeed <= 0f) spinSpeed = DEFAULT_SPIN_SPEED_MULTIPLIER;

        List<Projectile> projectilesInLane = context.getProjectilesInLane(lane);
        boolean reflectedAny = false;

        for (Projectile projectile : projectilesInLane) {
            if (projectile == null) {
                continue;
            }
            if (projectile.getDirection() <= 0) {
                continue; // already reflected, traveling toward plants
            }
            if (!isJuggleable(projectile)) {
                continue;
            }

            float projCol = projectile.getX();
            if (projCol < zombieCol - 1) {
                continue;
            }

            projectile.reflect();
            reflectedCount++;
            reflectedAny = true;
        }

        if (reflectedAny) {
            zombie.applySpeedModifier(spinSpeed);
        }
        return reflectedAny;
    }

    /**
     * @return true if this projectile type can be juggled by the Juggler.
     *         Straight-line {@link Pellet}s are juggleable; lobbed
     *         {@link Splash} projectiles are not.
     */
    private boolean isJuggleable(Projectile projectile) {
        if (!RESTRICT_TO_JUGGLEABLE) return true;
        return projectile instanceof Pellet;
    }

    /**
     * Finds the first incoming (rightward) juggleable projectile in the
     * zombie's lane that has reached or passed the zombie's column, or
     * null if there is none.
     */
    private Projectile findIncomingProjectile(ZombieInstance zombie, BehaviorContext context) {
        int lane = zombie.getGridY();
        int zombieCol = zombie.getGridX();

        for (Projectile projectile : context.getProjectilesInLane(lane)) {
            if (projectile == null) {
                continue;
            }
            if (projectile.getDirection() <= 0) {
                continue;
            }
            if (!isJuggleable(projectile)) {
                continue;
            }
            if (projectile.getX() >= zombieCol - 1f) {
                return projectile;
            }
        }
        return null;
    }

    // --- State transitions ---

    /** Transitions into {@link JugglePhase#SPINUP}: boosts speed, keeps walking. */
    private void startSpinning(ZombieInstance zombie) {
        phase = JugglePhase.SPINUP;
        clipTimer = 0f;
        timeSinceLastProjectile = 0f;
        float spinSpeed = zombie.getDefinition().getBehaviorPropFloat(
                "MoveSpeedMultiplierWhileJuggling", DEFAULT_SPIN_SPEED_MULTIPLIER);
        if (spinSpeed <= 0f) spinSpeed = DEFAULT_SPIN_SPEED_MULTIPLIER;
        zombie.applySpeedModifier(spinSpeed);
        keepWalking(zombie);
    }

    /** Spinning ended: play {@code spindown}, restore walk speed. */
    private void beginSpinDown(ZombieInstance zombie) {
        phase = JugglePhase.SPINDOWN;
        clipTimer = 0f;
        zombie.clearSpeedModifier();
        keepWalking(zombie);
    }

    /** Transitions back to IDLE after {@code spindown}. */
    private void stopSpinning(ZombieInstance zombie) {
        phase = JugglePhase.IDLE;
        clipTimer = 0f;
        timeSinceLastProjectile = 0f;
        zombie.clearSpeedModifier();
        keepWalking(zombie);
    }

    /** Stay walkable so {@code ZombieSystem} keeps advancing the zombie. */
    private static void keepWalking(ZombieInstance zombie) {
        if (zombie.getState() == ZombieState.SPECIAL_ACTION) {
            zombie.setState(ZombieState.WALKING);
        } else if (zombie.getState() != ZombieState.EATING) {
            zombie.setState(ZombieState.WALKING);
        }
    }

    // --- Getters / setters ---

    /** @return true while reflecting ({@code spinup} / looping {@code spin}). */
    public boolean isSpinning() {
        return phase == JugglePhase.SPINUP || phase == JugglePhase.SPIN;
    }

    public JugglePhase getPhase() {
        return phase;
    }

    public void setPhase(JugglePhase phase) {
        this.phase = phase;
    }

    /** Elapsed seconds in {@code spinup} / {@code spindown}; 0 at the first frame of each. */
    public float getClipTimer() {
        return clipTimer;
    }

    public float getTimeSinceLastProjectile() {
        return timeSinceLastProjectile;
    }

    public void setTimeSinceLastProjectile(float timeSinceLastProjectile) {
        this.timeSinceLastProjectile = timeSinceLastProjectile;
    }

    public int getReflectedCount() {
        return reflectedCount;
    }

    public void setReflectedCount(int reflectedCount) {
        this.reflectedCount = reflectedCount;
    }

    // --- Inner types ---

    /**
     * Walk → {@code spinup} → looping {@code spin} → {@code spindown} → walk.
     */
    public enum JugglePhase {
        IDLE,
        SPINUP,
        SPIN,
        SPINDOWN
    }
}
