package model.game.systems;

import model.game.core.Tickable;
import model.event.EventBus;
import model.event.GameEvent;
import model.game.core.GameModel;
import model.item.pushable.Pushable;
import model.projectile.Projectile;
import model.zombie.instance.ZombieInstance;

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

        List<ZombieInstance> zombies = gameModel.getZombies();
        if (zombies == null || zombies.isEmpty()) return;

        Projectile[] projectileSnapshot = projectiles.toArray(new Projectile[0]);

        for (Projectile projectile : projectileSnapshot) {
            if (projectile == null) continue;

            int lane = projectile.getRow();
            float projX = projectile.getX();

            for (ZombieInstance zombie : zombies) {
                if (zombie == null || zombie.isDead()) continue;
                Pushable pushable = zombie.getPushableItem();
                if (pushable == null || pushable.isDestroyed()) continue;
                if (!pushable.blocksProjectiles()) continue;

                int pushableCol = pushable.getCol();
                if (pushableCol < 0) {
                    continue;
                }

                if (zombie.getGridY() != lane) continue;

                // Projectile has reached or passed the pushable.
                if (projX >= pushableCol && projectile.getDirection() > 0) {
                    pushable.takeDamage(projectile.getDamage());
                    gameModel.removeProjectile(projectile);

                    if (pushable.isDestroyed()) {
                        pushable.onDestroyed();
                        if (eventBus != null) {
                            eventBus.dispatch(new GameEvent(GameEvent.Type.PUSHABLE_DESTROYED));
                        }
                    }
                    break;
                }
            }
        }
    }
}