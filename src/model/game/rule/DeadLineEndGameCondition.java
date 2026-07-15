package model.game.rule;

import model.game.core.GameModel;
import model.game.level.special.DeadLineLevel;
import model.zombie.instance.ZombieInstance;

/**
 * Dead Line levels are additionally lost the moment any zombie crosses
 * the red line column ({@code deadLineColumn}, -1 = no line).
 */
public class DeadLineEndGameCondition extends AbstractEndGameCondition {
    private final DeadLineLevel deadLineLevel;

    public DeadLineEndGameCondition(DeadLineLevel deadLineLevel) {
        this.deadLineLevel = deadLineLevel;
    }

    @Override
    public boolean isGameOver(GameModel model) {
        if (super.isGameOver(model)) return true;

        int line = deadLineLevel.getConfig().getDeadLineColumn();
        if (line < 0) return false;

        for (ZombieInstance zombie : model.getZombies()) {
            if (!zombie.isDead() && zombie.getGridX() < line) {
                return true;
            }
        }
        return false;
    }
}
