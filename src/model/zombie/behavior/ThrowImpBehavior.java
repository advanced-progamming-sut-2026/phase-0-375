package model.zombie.behavior;

import model.enums.ImpType;
import model.enums.ZombieBehaviorType;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.zombie.instance.ZombieInstance;

/**
 * Throw-imp behavior.
 */
public class ThrowImpBehavior implements ZombieBehavior {

    // --- Constants ---

    /** Default definition name used to look up and spawn the thrown Imp. */
    public static final String DEFAULT_IMP_NAME = "ZombieImp";

    /** Default fraction of max HP at (or below) which the Gargantuar throws its Imp. */
    public static final float DEFAULT_HEALTH_PERCENT_THROW_IMP = 0.5f;

    /** Default column the Imp is thrown to. */
    public static final int DEFAULT_IMP_TARGET_COLUMN = 2;

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
     * the configured threshold of its max HP.
     */
    private boolean hasReachedThrowThreshold(ZombieInstance zombie) {
        int maxHP = zombie.getDefinition().getBaseHP();
        if (maxHP <= 0) return false;

        float threshold = zombie.getDefinition().getBehaviorPropFloat(
                "HealthPercentThrowImp", DEFAULT_HEALTH_PERCENT_THROW_IMP);
        if (threshold <= 0f || threshold > 1f) {
            threshold = DEFAULT_HEALTH_PERCENT_THROW_IMP;
        }
        float healthFraction = (float) zombie.getCurrentHP() / maxHP;
        return healthFraction <= threshold;
    }

    /**
     * Resolves which imp definition to spawn based on the zombie's
     * configured {@link ImpType}.
     */
    private String resolveImpName(ZombieInstance zombie) {
        ImpType impType = zombie.getDefinition().getImpType();
        if (impType == null) return DEFAULT_IMP_NAME;
        switch (impType) {
            case EGYPT_IMP: return "ZombieImp";
            case ICEAGE_IMP: return "ZombieImp";
            case DRAGON_IMP: return "ZombieDarkImpDragon";
            default: return DEFAULT_IMP_NAME;
        }
    }

    /**
     * Spawns an Imp at the configured target column of the Gargantuar's row.
     */
    private void throwImp(ZombieInstance zombie, BehaviorContext context) {
        int row = zombie.getGridY();
        int targetCol = zombie.getDefinition().getBehaviorPropInt(
                "ImpTargetColumn", DEFAULT_IMP_TARGET_COLUMN);
        if (targetCol < 0) targetCol = DEFAULT_IMP_TARGET_COLUMN;
        String impName = resolveImpName(zombie);
        ZombieInstance imp = context.spawnZombieAt(impName, row, targetCol);
        if (imp != null) {
            imp.setGridPosition(new Point(targetCol, row));
            imp.setContinuousPosition(new FloatPoint(targetCol, row));
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