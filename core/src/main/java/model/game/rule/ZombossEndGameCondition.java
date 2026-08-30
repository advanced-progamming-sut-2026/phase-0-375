package model.game.rule;

import model.enums.ZombieBehaviorType;
import model.game.core.GameModel;
import model.zombie.instance.ZombieInstance;

/**
 * Zomboss levels are won when the boss is defeated (removed / dead).
 * House breach by summoned minions still loses the level.
 */
public class ZombossEndGameCondition extends AbstractEndGameCondition {

    private boolean bossSeen;

    @Override
    public boolean isWin(GameModel model) {
        if (model == null) {
            return false;
        }
        ZombieInstance boss = findBoss(model);
        if (boss != null) {
            bossSeen = true;
            return false;
        }
        return bossSeen;
    }

    @Override
    public boolean isGameOver(GameModel model) {
        return model != null && model.isHouseBreached();
    }

    private static ZombieInstance findBoss(GameModel model) {
        for (ZombieInstance zombie : model.getZombies()) {
            if (zombie != null && !zombie.isDead()
                    && zombie.hasBehavior(ZombieBehaviorType.ZOMBOSS)) {
                return zombie;
            }
        }
        return null;
    }
}
