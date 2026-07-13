package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.game.map.Point;
import model.item.pushable.Pushable;
import model.zombie.instance.ZombieInstance;

/**
 * Barrel Roller behavior.
 */
public class BarrelRollerBehavior implements ZombieBehavior {

    // --- Constants ---

    /** Definition name of the imp to spawn when the barrel breaks. */
    public static final String IMP_NAME = "ZombieImp";

    /** Number of imps spawned when the barrel is destroyed. */
    public static final int IMPS_PER_BARREL = 2;

    // --- State ---

    /** True once this behavior has already spawned its imps. */
    private boolean spawnedImps = false;

    /** True once we've seen a non-null pushable on this zombie at least once. */
    private boolean hadPushable = false;

    /** Last known row of the barrel. cached for imp spawning. */
    private int lastBarrelRow = -1;

    /** Last known column of the barrel. cached for imp spawning. */
    private int lastBarrelCol = -1;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead()) {
            return;
        }
        if (spawnedImps) {
            return;
        }

        Pushable pushable = zombie.getPushableItem();

        if (pushable != null && !pushable.isDestroyed()) {
            hadPushable = true;
            Point pos = pushable.getPosition();
            if (pos != null) {
                lastBarrelRow = pos.getY();
                lastBarrelCol = pos.getX();
            }
            return;
        }

        // Pushable is null or destroyed.
        if (hadPushable && !spawnedImps) {
            // Barrel was destroyed while the zombie is still alive - spawn imps.
            spawnImps(context);
            spawnedImps = true;
        }
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.BARREL_ROLLER;
    }

    // --- Core logic ---

    /**
     * Spawns {@value #IMPS_PER_BARREL} imps at the barrel's last known
     * position. The imps immediately start walking toward the house.
     */
    private void spawnImps(BehaviorContext context) {
        if (lastBarrelRow < 0 || lastBarrelCol < 0) {
            return;
        }

        for (int i = 0; i < IMPS_PER_BARREL; i++) {
            context.spawnZombieAt(IMP_NAME, lastBarrelRow, lastBarrelCol);
        }
    }

    // --- Getters / setters ---

    public boolean hasSpawnedImps() {
        return spawnedImps;
    }

    public boolean hadPushable() {
        return hadPushable;
    }

    public int getLastBarrelRow() {
        return lastBarrelRow;
    }

    public int getLastBarrelCol() {
        return lastBarrelCol;
    }

    public void setSpawnedImps(boolean spawnedImps) {
        this.spawnedImps = spawnedImps;
    }

    public void setHadPushable(boolean hadPushable) {
        this.hadPushable = hadPushable;
    }

    public void setLastBarrelRow(int lastBarrelRow) {
        this.lastBarrelRow = lastBarrelRow;
    }

    public void setLastBarrelCol(int lastBarrelCol) {
        this.lastBarrelCol = lastBarrelCol;
    }
}
