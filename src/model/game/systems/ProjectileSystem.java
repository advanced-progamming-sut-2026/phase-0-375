package model.game.systems;


import model.enums.PlantCategory;
import model.enums.PlantTags;
import model.enums.ZombieBehaviorType;
import model.game.core.Tickable;
import model.event.EventBus;
import model.event.GameEvent;
import model.game.core.GameModel;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.Pellet;
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

            applyTorchwood(projectile);

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

            if (!projectile.pierce()) { gameModel.removeProjectile(projectile); }

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

    // --- Torchwood pea-conversion hook ---

    /**
     * Modifier hook for {@code Torchwood}: if the given projectile is a
     * straight-line pea ({@link Pellet}) that is not already FIRE-aligned
     * and its current tile is occupied by a Torchwood, the pea is ignited
     * (its element becomes {@link Projectile.Element#FIRE}) and its damage
     * is multiplied by the Torchwood's {@code abilityValue}.
     */
    private void applyTorchwood(Projectile projectile) {
        if (!(projectile instanceof Pellet)) return;
        if (projectile.isFire()) return;
        if (projectile.isReflected()) return;

        int row = projectile.getRow();
        int col = (int) Math.floor(projectile.getX());
        if (col < 0 || col >= gameModel.getColumnCount()) return;

        PlantInstance plant = gameModel.getPlantAt(row, col);
        if (plant == null) return;
        Plant def = plant.getDefinition();
        if (def == null) return;
        if (def.getCategory() != PlantCategory.MODIFIER) return;
        if (!def.hasTag(PlantTags.FIRE)) return;

        // Ignite the pea
        projectile.setElement(Projectile.Element.FIRE);

        // Boost damage by the Torchwood's ability value
        float multiplier = def.getAbilityValue();
        if (multiplier <= 0f) multiplier = 2.0f;
        int boosted = Math.max(projectile.getDamage(), Math.round(projectile.getDamage() * multiplier));
        projectile.setDamage(boosted);
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