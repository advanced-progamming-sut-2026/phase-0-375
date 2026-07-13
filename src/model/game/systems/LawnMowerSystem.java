package model.game.systems;

import model.event.EventBus;
import model.event.GameEvent;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.game.map.Lane;
import model.game.map.LawnMower;
import model.zombie.instance.ZombieInstance;

import java.util.List;

public class LawnMowerSystem implements Tickable {

    /** Damage dealt by a mower to any zombie it touches. */
    public static final int MOWER_DAMAGE = 6767;

    private final GameModel gameModel;
    private final EventBus eventBus;

    public LawnMowerSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {
        int rowCount = gameModel.getMap().getRows();
        int colCount = gameModel.getMap().getCols();

        for (int row = 0; row < rowCount; row++) {
            Lane lane = gameModel.getMap().getLane(row);
            if (lane == null) continue;
            LawnMower mower = lane.getLawnMower();
            if (mower == null || !mower.isTriggered()) continue;

            boolean finished = mower.tick(deltaTime, colCount);
            killZombiesInPath(row, mower.getXPosition());

            if (finished) {
                if (eventBus != null) {
                    eventBus.dispatch(new GameEvent(GameEvent.Type.LAWN_MOWER_TRIGGERED));
                }
            }
        }
    }

    /**
     * Kills every zombie in the given lane whose column is at or behind
     * the mower's current X position.
     */
    private void killZombiesInPath(int row, float mowerX) {
        List<ZombieInstance> zombiesInLane = gameModel.getZombiesInLane(row);
        for (ZombieInstance zombie : zombiesInLane) {
            if (zombie == null || zombie.isDead()) continue;
            if (zombie.getGridPosition() == null) continue;

            if (zombie.getGridX() <= mowerX && !isBoss(zombie)) {
                gameModel.damageZombie(zombie, MOWER_DAMAGE);
            }
        }
    }

    private boolean isBoss(ZombieInstance zombie) {
        if (zombie.getDefinition() == null) return false;
        String name = zombie.getDefinition().getName();
        return name != null && name.toLowerCase().contains("gargantuar");
    }
}