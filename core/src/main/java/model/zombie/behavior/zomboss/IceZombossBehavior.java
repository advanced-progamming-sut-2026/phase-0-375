package model.zombie.behavior.zomboss;

import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Frostbite Caves mammoth Zomboss stub — no lane change, no summon.
 */
public class IceZombossBehavior extends ZombossBehavior {

    public static final String DEFINITION_NAME = "ZombieIceZomboss";

    @Override
    protected List<ZombossAction> buildActionPool() {
        return List.of(
                ZombossAction.ICE_MISSILE,
                ZombossAction.ICE_WIND,
                ZombossAction.FREEZE_COLUMN
        );
    }

    @Override
    protected List<String> buildSummonPool() {
        return List.of();
    }

    @Override
    protected boolean canChangeLane() {
        return false;
    }

    @Override
    protected boolean canSummon() {
        return false;
    }

    @Override
    protected void beginAction(ZombieInstance zombie, BehaviorContext context, ZombossAction action) {
        // Chapter-specific attacks not implemented yet.
    }

    @Override
    protected boolean tickAction(ZombieInstance zombie, BehaviorContext context,
                                 float deltaTime, ZombossAction action) {
        return true;
    }
}
