package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.zombie.instance.ZombieInstance;

/**
 * Throw-imp behavior.
 */
public class ThrowImpBehavior implements ZombieBehavior {

    // --- Constants ---

    /** Definition name used to look up and spawn the thrown Imp. */
    public static final String IMP_NAME = "ZombieImp";

    /** Fraction of max HP at (or below) which the Gargantuar throws its Imp. */
    public static final float HEALTH_PERCENT_THROW_IMP = 0.5f;

    /** Column the Imp is thrown to. */
    public static final int IMP_TARGET_COLUMN = 2;

    /** True once this Gargantuar has thrown its Imp. */
    private boolean hasThrownImp = false;

    // --- ZombieBehavior ---

    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie == null || context == null || zombie.isDead() || hasThrownImp) {
            return;
        }

        if (!hasReachedThrowThreshold(zombie)) {
            return;
        }

        throwImp(zombie, context);
    }

    @Override
    public ZombieBehaviorType getType() {
        return ZombieBehaviorType.THROW_IMP;
    }

    // --- Core logic ---

    /**
     * @return true if the Gargantuar's current HP has dropped to or below
     * {@value #HEALTH_PERCENT_THROW_IMP} of its max HP.
     */
    private boolean hasReachedThrowThreshold(ZombieInstance zombie) {
        int maxHP = zombie.getDefinition().getBaseHP();
        if (maxHP <= 0) return false;

        float healthFraction = (float) zombie.getCurrentHP() / maxHP;
        return healthFraction <= HEALTH_PERCENT_THROW_IMP;
    }

    /**
     * Spawns an Imp at {@link #IMP_TARGET_COLUMN} of the Gargantuar's row.
     */
    private void throwImp(ZombieInstance zombie, BehaviorContext context) {
        int row = zombie.getGridY();
        ZombieInstance imp = context.spawnZombieAt(IMP_NAME, row, IMP_TARGET_COLUMN);
        if (imp != null) {
            imp.setGridPosition(new Point(IMP_TARGET_COLUMN, row));
            imp.setContinuousPosition(new FloatPoint(IMP_TARGET_COLUMN, row));
        }

        hasThrownImp = true;
    }

    // --- Getters ---

    public boolean hasThrownImp() {
        return hasThrownImp;
    }

    // --- Setters ---

    public void setHasThrownImp(boolean hasThrownImp) {
        this.hasThrownImp = hasThrownImp;
    }
}