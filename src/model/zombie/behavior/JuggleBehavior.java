package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.enums.ZombieState;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Juggle behavior.
 */
public class JuggleBehavior implements ZombieBehavior {

    // --- Constants ---

    /** Multiplier applied to the zombie's base speed while spinning. */
    public static final float SPIN_SPEED_MULTIPLIER = 2.0f;

    /** Seconds the zombie keeps spinning after the last projectile was reflected. */
    public static final float SPIN_TIMEOUT = 1.0f;

    // --- State ---

    /** Current phase of the juggle cycle. */
    private JugglePhase phase = JugglePhase.IDLE;

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
            case SPINNING:
                tickSpinning(zombie, context, deltaTime);
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
        // Make sure the zombie is in a walkable state.
        if (zombie.getState() == ZombieState.SPECIAL_ACTION) {
            zombie.setState(ZombieState.WALKING);
        }

        if (findIncomingProjectile(zombie, context) != null) {
            startSpinning(zombie);
            reflectIncomingProjectiles(zombie, context);
        }
    }

    // --- SPINNING phase ---

    private void tickSpinning(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie.getState() != ZombieState.SPECIAL_ACTION) {
            zombie.setState(ZombieState.SPECIAL_ACTION);
        }

        boolean reflectedAny = reflectIncomingProjectiles(zombie, context);

        if (reflectedAny) {
            timeSinceLastProjectile = 0f;
        } else {
            timeSinceLastProjectile += deltaTime;
            if (timeSinceLastProjectile >= SPIN_TIMEOUT) {
                stopSpinning(zombie);
            }
        }
    }

    // --- Reflection logic ---

    /**
     * Finds every incoming projectile in the zombie's lane that has reached
     * or passed the zombie's column, reflects each one back toward the plants,
     * and returns true if at least one was reflected this tick.
     */
    private boolean reflectIncomingProjectiles(ZombieInstance zombie, BehaviorContext context) {
        int lane = zombie.getGridY();
        int zombieCol = zombie.getGridX();

        List<Projectile> projectilesInLane = context.getProjectilesInLane(lane);
        boolean reflectedAny = false;

        for (Projectile projectile : projectilesInLane) {
            if (projectile == null) {
                continue;
            }

            if (projectile.getDirection() <= 0) {
                continue;
            }

            float projCol = projectile.getX();
            if (projCol < zombieCol) {
                continue;
            }

            projectile.setX(zombieCol);
            projectile.reflect();
            reflectedCount++;
            reflectedAny = true;
        }

        return reflectedAny;
    }

    /**
     * Finds the first incoming (rightward) projectile in the zombie's
     * lane that has reached or passed the zombie's column, or null if
     * there is none.
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
            if (projectile.getX() >= zombieCol) {
                return projectile;
            }
        }
        return null;
    }

    // --- State transitions ---

    /** Transitions the zombie from IDLE to SPINNING: boosts speed, sets state. */
    private void startSpinning(ZombieInstance zombie) {
        phase = JugglePhase.SPINNING;
        timeSinceLastProjectile = 0f;
        zombie.applySpeedModifier(SPIN_SPEED_MULTIPLIER);
        zombie.setState(ZombieState.SPECIAL_ACTION);
    }

    /** Transitions the zombie from SPINNING back to IDLE: restores speed, sets state. */
    private void stopSpinning(ZombieInstance zombie) {
        phase = JugglePhase.IDLE;
        timeSinceLastProjectile = 0f;
        zombie.clearSpeedModifier();
        zombie.setState(ZombieState.WALKING);
    }

    // --- Getters / setters ---

    /** @return true while the zombie is currently spinning. */
    public boolean isSpinning() {
        return phase == JugglePhase.SPINNING;
    }


    public JugglePhase getPhase() {
        return phase;
    }

    public void setPhase(JugglePhase phase) {
        this.phase = phase;
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
     * The two phases of the Juggler's cycle.
     */
    public enum JugglePhase {
        IDLE, // Walking slowly, scanning for incoming projectiles.
        SPINNING // Spinning, moving faster, reflecting all incoming projectiles.
    }
}
