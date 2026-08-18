package model.game.systems;

import model.enums.PlacableLayer;
import model.enums.ZombieState;
import model.game.core.Tickable;
import model.event.EventBus;
import model.event.GameEvent;
import model.game.core.GameModel;
import model.game.map.Cell;
import model.item.pushable.Barrel;
import model.item.pushable.IceBlock;
import model.item.pushable.Pushable;
import model.item.placeable.Placeable;
import model.plant.instance.PlantInstance;
import model.projectile.Projectile;
import model.zombie.behavior.BarrelRollerBehavior;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;

public class PushableSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

    public PushableSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {
        List<Projectile> projectiles = gameModel.getProjectiles();
        if (projectiles == null || projectiles.isEmpty()) return;

        Projectile[] projectileSnapshot = projectiles.toArray(new Projectile[0]);

        for (Projectile projectile : projectileSnapshot) {
            if (projectile == null) continue;

            int lane = projectile.getRow();
            float projX = projectile.getX();

            if (hitZombiePushable(projectile, lane, projX)) {
                continue;
            }
            hitOrphanedPushable(projectile, lane, projX);
        }
    }

    /** @return true if a living zombie's pushable absorbed this projectile. */
    private boolean hitZombiePushable(Projectile projectile, int lane, float projX) {
        List<ZombieInstance> zombies = gameModel.getZombies();
        if (zombies == null || zombies.isEmpty()) return false;

        for (ZombieInstance zombie : zombies) {
            if (zombie == null || zombie.isDead()) continue;
            Pushable pushable = zombie.getPushableItem();
            if (pushable == null || pushable.isDestroyed()) continue;
            if (!pushable.blocksProjectiles()) continue;
            if (zombie.getGridY() != lane) continue;

            int pushableCol = pushable.getCol();
            if (pushableCol < 0) {
                continue;
            }

            if (projX >= pushableCol && projectile.getDirection() > 0) {
                damagePushable(pushable, projectile);
                return true;
            }
        }
        return false;
    }

    /** Hits pushables left behind after their pusher died (e.g. Barrel). */
    private void hitOrphanedPushable(Projectile projectile, int lane, float projX) {
        List<Pushable> orphans = gameModel.getOrphanedPushables();
        if (orphans == null || orphans.isEmpty()) return;

        for (Pushable pushable : new ArrayList<>(orphans)) {
            if (pushable == null || pushable.isDestroyed()) {
                gameModel.removeOrphanedPushable(pushable);
                continue;
            }
            if (!pushable.blocksProjectiles()) continue;
            if (pushable.getRow() != lane) continue;

            int pushableCol = pushable.getCol();
            if (pushableCol < 0) continue;

            if (projX >= pushableCol && projectile.getDirection() > 0) {
                damagePushable(pushable, projectile);
                return;
            }
        }
    }

    private void damagePushable(Pushable pushable, Projectile projectile) {
        pushable.takeDamage(projectile.getDamage());
        gameModel.removeProjectile(projectile);

        if (pushable.isDestroyed()) {
            boolean wasOrphanBarrel = pushable instanceof Barrel
                    && pushable.getPusher() == null
                    && gameModel.getOrphanedPushables().contains(pushable);

            int row = pushable.getRow();
            int col = pushable.getCol();
            Placeable iceOccupant = pushable instanceof IceBlock ice
                    ? ice.getContainedEntity() : null;
            if (pushable instanceof IceBlock ice) {
                ice.setContainedEntity(null);
            }

            pushable.onDestroyed();
            gameModel.removeOrphanedPushable(pushable);
            releaseIceOccupant(iceOccupant, row, col);

            if (wasOrphanBarrel && row >= 0 && col >= 0) {
                for (int i = 0; i < BarrelRollerBehavior.IMPS_PER_BARREL; i++) {
                    BarrelRollerBehavior.scatterImp(
                            gameModel.spawnZombieAt(BarrelRollerBehavior.IMP_NAME, row, col));
                }
            }

            if (eventBus != null) {
                eventBus.dispatch(new GameEvent(GameEvent.Type.PUSHABLE_DESTROYED));
            }
        }
    }

    private void releaseIceOccupant(Placeable occupant, int row, int col) {
        if (occupant == null || row < 0 || col < 0) {
            return;
        }
        if (occupant instanceof ZombieInstance zombie) {
            while (zombie.getChillLevel() > 0) {
                zombie.removeChill();
            }
            ZombieState state = zombie.getState();
            if (state != ZombieState.DYING && state != ZombieState.DEAD) {
                zombie.setState(ZombieState.WALKING);
            }
            gameModel.addExistingZombie(zombie, row, col);
            return;
        }
        if (occupant instanceof PlantInstance plant) {
            if (plant.isFrozen()) {
                plant.unfreeze();
            }
            Cell cell = gameModel.getCellAt(row, col);
            if (cell != null && cell.getPlaceable(PlacableLayer.MAIN) != plant) {
                cell.addPlaceable(plant);
            }
        }
    }
}
