package model.game.systems;


import model.enums.ZombieBehaviorType;
import model.game.core.Tickable;
import model.event.EventBus;
import model.event.GameEvent;
import model.game.core.GameModel;
import model.projectile.Projectile;
import model.projectile.Splash;
import model.zombie.behavior.JumpBehavior;
import model.zombie.behavior.ShootBehavior;
import model.zombie.instance.ZombieInstance;

import java.util.List;

public class ProjectileSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

    public ProjectileSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {
        List<Projectile> projectiles = gameModel.getProjectiles();
        if (projectiles == null || projectiles.isEmpty()) return;

        Projectile[] snapshot = projectiles.toArray(new Projectile[0]);

        for (Projectile projectile : snapshot) {
            if (projectile == null) continue;

            moveProjectile(projectile, deltaTime);

            float x = projectile.getX();
            if (x < 0f || x >= gameModel.getColumnCount()) {
                gameModel.removeProjectile(projectile);
                continue;
            }

            ZombieInstance target = findCollision(projectile);
            if (target == null) {
                continue;
            }

            applyDamage(projectile, target);
            applyOnHitEffects(projectile, target);

            // Splash projectiles apply AoE damage around the impact point.
            if (projectile instanceof Splash) {
                applySplashDamage((Splash) projectile, target);
            }

            gameModel.removeProjectile(projectile);
            if (eventBus != null) {
                eventBus.dispatch(new GameEvent(GameEvent.Type.PROJECTILE_HIT));
            }
        }
    }

    // --- Movement ---

    private void moveProjectile(Projectile projectile, float deltaTime) {
        float newX = projectile.getX() + projectile.getVelocity() * projectile.getDirection() * deltaTime;
        projectile.setX(newX);
    }

    // --- Collision ---

    /**
     * Finds the first live zombie in the projectile's lane whose column
     * the projectile has reached or passed.
     */
    private ZombieInstance findCollision(Projectile projectile) {
        int lane = projectile.getRow();
        float projX = projectile.getX();
        float tolerance = 0.5f;

        List<ZombieInstance> zombiesInLane = gameModel.getZombiesInLane(lane);
        ZombieInstance best = null;
        float bestDist = Float.MAX_VALUE;

        for (ZombieInstance zombie : zombiesInLane) {
            if (zombie == null || zombie.isDead()) continue;

            if (zombie.isSubmerged() && !(projectile instanceof Splash)) {
                continue;
            }

            float zombieX = zombie.getContinuousX();
            float dist = Math.abs(zombieX - projX);
            if (dist > tolerance) continue;

            // Pick the closest match.
            if (dist < bestDist) {
                bestDist = dist;
                best = zombie;
            }
        }
        return best;
    }

    // --- Damage application ---

    /**
     * Applies projectile damage to a zombie, going through armor first.
     */
    private void applyDamage(Projectile projectile, ZombieInstance zombie) {
        int damage = projectile.getDamage();
        if (damage <= 0) return;

        if (projectile.isFire() && zombie.isImmuneToFire()) {
            return;
        }

        if (projectile.isFire()) {
            zombie.takeFireDamage(damage);
        } else if (projectile.isPoison()) {
            zombie.takePoisonDamage(damage);
        } else {
            gameModel.damageZombie(zombie, damage);
        }
    }

    /**
     * Applies splash/AoE damage to zombies near the impact point.
     */
    private void applySplashDamage(Splash splash, ZombieInstance primaryTarget) {
        float radius = splash.getSplashRadius();
        if (radius <= 0) return;

        int lane = splash.getRow();
        float centerX = splash.getX();
        List<ZombieInstance> zombiesInLane = gameModel.getZombiesInLane(lane);

        for (ZombieInstance zombie : zombiesInLane) {
            if (zombie == null || zombie.isDead() || zombie == primaryTarget) continue;

            float dist = Math.abs(zombie.getContinuousX() - centerX);
            if (dist > radius) continue;

            if (splash.isFire() && zombie.isImmuneToFire()) continue;

            if (splash.isFire()) {
                zombie.takeFireDamage(splash.getDamage());
            } else if (splash.isPoison()) {
                zombie.takePoisonDamage(splash.getDamage());
            } else {
                gameModel.damageZombie(zombie, splash.getDamage());
            }
        }
    }

    // --- On-hit elemental effects ---

    /** Applies on-hit status effects based on the projectile's element */
    private void applyOnHitEffects(Projectile projectile, ZombieInstance zombie) {
        if (projectile.isIce()) {
            zombie.applyChill();

            // Prospector dynamite extinguish.
            JumpBehavior jump = (JumpBehavior) zombie.getBehavior(ZombieBehaviorType.JUMP);
            if (jump != null) {
                jump.extinguish();
            }

            // Explorer torch extinguish.
            ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
            if (shoot != null && shoot.isExplorer(zombie)) {
                shoot.extinguishTorch();
            }
        }

        if (projectile.isFire()) {
            // Explorer torch reignite.
            ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
            if (shoot != null && shoot.isExplorer(zombie)) {
                shoot.igniteTorch();
            }
        }
    }
}