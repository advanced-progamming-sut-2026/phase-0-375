package model.zombie.behavior.zomboss;

import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Ancient Egypt Zomboss stub — missile + charge attacks to be filled in later.
 * Shares summon / lane-change with the base class.
 */
public class EgyptZombossBehavior extends ZombossBehavior {

    public static final String DEFINITION_NAME = "ZombieEgyptZomboss";

    @Override
    protected List<ZombossAction> buildActionPool() {
        return List.of(
                ZombossAction.MISSILE,
                ZombossAction.CHARGE,
                ZombossAction.SUMMON,
                ZombossAction.CHANGE_LANE
        );
    }

    @Override
    protected List<String> buildSummonPool() {
        return List.of(
                "ZombieDefault",
                "ZombieRa",
                "ZombieExplorer",
                "ZombieTombRaiser",
                "ZombieImp"
        );
    }

    @Override
    protected void beginAction(ZombieInstance zombie, BehaviorContext context, ZombossAction action) {
        // Chapter-specific attacks not implemented yet.
    }

    @Override
    protected boolean tickAction(ZombieInstance zombie, BehaviorContext context,
                                 float deltaTime, ZombossAction action) {
        if (action == ZombossAction.SUMMON) {
            summonMinions(context, 3);
            return true;
        }
        if (action == ZombossAction.CHANGE_LANE) {
            changeLane(zombie, context);
            return true;
        }
        // Placeholder: MISSILE / CHARGE finish immediately until implemented.
        return true;
    }
}
