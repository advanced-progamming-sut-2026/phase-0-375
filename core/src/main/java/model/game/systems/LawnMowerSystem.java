package model.game.systems;

import model.event.EventBus;
import model.event.GameEvent;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.game.map.Lane;
import model.game.map.LawnMower;
import model.zombie.instance.ZombieInstance;
import model.app.App;

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

            gameModel.markLawnMowerUsed();

            boolean finished = mower.tick(deltaTime, colCount);
            if (mower.isSweeping()) {
                killZombiesInPath(mower, row, mower.getXPosition());
            }

            if (finished) {
                lane.clearLawnMower();
                if (eventBus != null) {
                    eventBus.dispatch(new GameEvent(GameEvent.Type.LAWN_MOWER_TRIGGERED));
                }
            }
        }
    }

    private void killZombiesInPath(LawnMower mower, int row, float mowerX) {
        List<ZombieInstance> zombiesInLane = gameModel.getZombiesInLane(row);
        for (ZombieInstance zombie : zombiesInLane) {
            if (zombie == null || zombie.isDead()) continue;
            if (zombie.getContinuousPosition() == null) continue;

            // Stationary spawns (Fisherman) sit at x = columnCount and never walk in.
            if (zombie.getContinuousX() <= mowerX && !isBoss(zombie)) {
                zombie.recordNonPlantDamage();
                zombie.markKilledByMower();
                gameModel.damageZombie(zombie, MOWER_DAMAGE);
                mower.recordSweepKill(zombie);
            }
        }
    }

    private void notifyMowerKills(int row, LawnMower mower) {
        List<ZombieInstance> kills = mower.getSweepKills();
        StringBuilder sb = new StringBuilder();
        sb.append("The lawn mower in the row ").append(row + 1)
                .append("is triggered and killed these zombies:");
        for (ZombieInstance z : kills) {
            String type = (z.getDefinition() != null) ? z.getDefinition().getName() : "Unknown";
            int x = z.getGridPosition() != null ? z.getGridX() : -1;
            int y = z.getGridPosition() != null ? z.getGridY() : -1;
            sb.append("\n  ").append(type).append(" at (").append(x).append(", ").append(y).append(")");
        }
        App.logToShell(sb.toString());
    }

    private boolean isBoss(ZombieInstance zombie) {
        if (zombie.getDefinition() == null) return false;
        String name = zombie.getDefinition().getName();
        return name != null && name.toLowerCase().contains("gargantuar");
    }
}
