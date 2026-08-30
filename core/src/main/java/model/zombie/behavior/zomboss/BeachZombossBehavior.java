package model.zombie.behavior.zomboss;

import model.zombie.behavior.BehaviorContext;
import model.zombie.instance.ZombieInstance;

import java.util.List;

/**
 * Big Wave Beach shark Zomboss stub — baby sharks + turbine to be filled later.
 */
public class BeachZombossBehavior extends ZombossBehavior {

    public static final String DEFINITION_NAME = "ZombieBeachZomboss";

    @Override
    protected List<ZombossAction> buildActionPool() {
        return List.of(
                ZombossAction.BABY_SHARK,
                ZombossAction.TURBINE,
                ZombossAction.SUMMON,
                ZombossAction.CHANGE_LANE
        );
    }

    @Override
    protected List<String> buildSummonPool() {
        return List.of(
                "ZombieDefault",
                "ZombieBeachSnorkel",
                "ZombieBeachOctopus",
                "ZombieBeachFisherman"
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
        return true;
    }
}
