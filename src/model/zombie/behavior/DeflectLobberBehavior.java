package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.projectile.Projectile;
import model.projectile.Splash;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Deflect-lobber behavior.
 */
public class DeflectLobberBehavior implements ZombieBehavior {

    // --- Constants ---

    /**
     * Distance ahead of the zombie at which a lobbed projectile is
     * considered "in range" of the parasol and gets deflected.
     */
    public static final float DEFLECT_RANGE = 0.5f;

    // --- State ---

    /** Total lobbed projectiles deflected by this zombie. */
    private int deflectedCount = 0;

    /** True while at least one lobbed projectile was deflected this tick. */
    private boolean deflectedThisTick = false;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        deflectedThisTick = false;

        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }

        deflectIncomingLobbedProjectiles(zombie, context);
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.DEFLECT_LOBBER;
    }

    // --- Core logic ---

    /**
     * Scans the zombie's lane for any incoming {@link Splash}
     * projectile that has reached the zombie's column, and removes
     * each one from the field.
     */
    private void deflectIncomingLobbedProjectiles(ZombieInstance zombie, BehaviorContext context) {
        int lane = zombie.getGridY();
        float zombieX = zombie.getContinuousX();

        List<Projectile> projectilesInLane = context.getProjectilesInLane(lane);
        if (projectilesInLane == null || projectilesInLane.isEmpty()) {
            return;
        }

        for (Projectile projectile : projectilesInLane.toArray(new Projectile[0])) {
            if (projectile == null) {
                continue;
            }

            if (!(projectile instanceof Splash)) {
                continue;
            }

            if (projectile.getDirection() <= 0) {
                continue;
            }

            if (projectile.getX() < zombieX - DEFLECT_RANGE) {
                continue;
            }

            context.removeProjectile(projectile);
            deflectedCount++;
            deflectedThisTick = true;
        }
    }

    // --- Getters / setters ---

    /** @return total lobbed projectiles deflected by this zombie so far. */
    public int getDeflectedCount() {
        return deflectedCount;
    }

    /** @return true if at least one lobbed projectile was deflected during the last tick. */
    public boolean isDeflectedThisTick() {
        return deflectedThisTick;
    }

    public void setDeflectedCount(int deflectedCount) {
        this.deflectedCount = deflectedCount;
    }

    public void setDeflectedThisTick(boolean deflectedThisTick) {
        this.deflectedThisTick = deflectedThisTick;
    }
}
