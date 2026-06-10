package model.game.systems;


import model.core.Tickable;
import model.event.EventBus;
import model.game.GameModel;
import model.projectile.Projectile;
import model.projectile.Splash;
import model.zombie.instance.ZombieInstance;

public class ProjectileSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

    public ProjectileSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {

    }

    /**
     * Moves a projectile based on its speed and direction.
     */
    private void moveProjectile(Projectile projectile, float deltaTime) {

    }

    /**
     * Finds the first zombie that collides with this projectile.
     * Checks same lane and adjacent grid column.
     */
    private ZombieInstance findCollision(Projectile projectile) {
        return null;
    }

    /**
     * Applies projectile damage to a zombie, going through armor first.
     */
    private void applyDamage(Projectile projectile, ZombieInstance zombie) {

    }

    /**
     * Applies splash/AoE damage to zombies near the impact point.
     */
    private void applySplashDamage(Splash splash, ZombieInstance primaryTarget) {

    }
}
