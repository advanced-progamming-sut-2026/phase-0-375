package model.game.systems;


import model.enums.*;
import model.game.core.Tickable;
import model.event.EventBus;
import model.event.GameEvent;
import model.game.core.GameModel;
import model.item.GridItem;
import model.item.Grave;
import model.item.placeable.Placeable;
import model.item.pushable.Pushable;
import model.plant.ability.ModifierAbility;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.projectile.BowlingBulb;
import model.projectile.FumeCloud;
import model.projectile.Pellet;
import model.projectile.Projectile;
import model.projectile.Splash;
import model.zombie.behavior.JumpBehavior;
import model.zombie.behavior.ShootBehavior;
import model.zombie.instance.ZombieInstance;

import model.game.map.Cell;
import model.game.map.terrain.IceTerrainStrategy;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ProjectileSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

    private static final float DIAGONAL = (float) (1.0 / Math.sqrt(2.0));
    private static final int BULB_EXPLOSION_RADIUS = 1;

    /** How far past the plantable grid a projectile may travel before despawn. */
    private static final float OFF_GRID_MARGIN = 1.5f;

    /**
     * Tracks the last ice-cell column each projectile has already damaged,
     * so a fire pea melting an ice block deals its damage exactly once per
     * tile instead of once per frame.
     */
    private final Map<Projectile, Integer> iceDamagedColumns = new IdentityHashMap<>();

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
            if (tickProjectile(projectile, deltaTime)) return;
        }
    }

    /**
     * Advances one projectile: movement, terrain interactions, bounds check
     * and collision handling.
     *
     * @return true if the whole tick must stop early (a pellet consumed the
     *         frame by hitting a grid item), preserving the original behavior
     */
    private boolean tickProjectile(Projectile projectile, float deltaTime) {
        if (projectile instanceof FumeCloud) {
            tickFumeCloud((FumeCloud) projectile, deltaTime);
            return false;
        }

        moveProjectile(projectile, deltaTime);

        if (projectile instanceof BowlingBulb) {
            bounceOffLaneEdges((BowlingBulb) projectile);
        }
        applyTorchwood(projectile);

        // Fire peas melt ice terrain they cross (Frostbite Caves).
        damageIceIfPresent(projectile);
        thawFrozenPlantIfPresent(projectile);

        if (leftPlayableArea(projectile)) {
            discard(projectile, false);
            return false;
        }

        // Non-lobber shots are blocked by ice/octopus coatings on frozen plants.
        if (hitFrozenPlantIfBlocking(projectile)) {
            return false;
        }

        // Juggler-reflected pellets travel leftward and damage plants.
        if (projectile.isReflected() && hitPlantIfReflected(projectile)) {
            return false;
        }

        if (projectile instanceof Pellet && hitGridItemsIfAny(projectile)) {
            discard(projectile, true);
            return true;
        }

        // Lobs fly over zombies until they land, then splash at the impact tile.
        if (projectile instanceof Splash splash && splash.isLobbing() && !splash.hasLanded()) {
            return false;
        }

        ZombieInstance target = findCollision(projectile);
        if (target != null) {
            handleZombieHit(projectile, target);
        } else if (projectile instanceof Splash splash && splash.hasLanded()) {
            applySplashDamage(splash, null);
            discard(splash, true);
        }
        return false;
    }

    /**
     * Fume bubbles sit on a tile, damage every zombie there once, then
     * dissipate with the clip instead of traveling.
     */
    private void tickFumeCloud(FumeCloud cloud, float deltaTime) {
        if (!cloud.isBurstDone()) {
            applyFumeBurst(cloud);
            cloud.markBurstDone();
        }
        if (cloud.tickLifetime(deltaTime)) {
            discard(cloud, true);
        }
    }

    private void applyFumeBurst(FumeCloud cloud) {
        int lane = cloud.getRow();
        int tile = Math.round(cloud.getX());
        List<ZombieInstance> zombiesInLane = gameModel.getZombiesInLane(lane);
        if (zombiesInLane == null) return;

        boolean isPlantFired = cloud.getSourcePlant() != null;
        for (ZombieInstance zombie : zombiesInLane) {
            if (zombie == null || zombie.isDead()) continue;
            if (zombie.getContinuousPosition() == null) continue;
            if (isPlantFired && zombie.isHypnotized()) continue;
            if (Math.round(zombie.getContinuousX()) != tile) continue;
            handleZombieHit(cloud, zombie);
        }
    }

    /** Applies damage and on-hit effects once a projectile reaches a zombie. */
    private void handleZombieHit(Projectile projectile, ZombieInstance target) {
        if (projectile.hasAlreadyHit(target)) {
            return;
        }
        projectile.markHit(target);

        applyDamage(projectile, target);
        applyOnHitEffects(projectile, target);

        // Splash projectiles apply AoE damage around the impact point.
        if (projectile instanceof Splash) {
            applySplashDamage((Splash) projectile, target);
        }

        // Bowling Bulbs either detonate or deflect and keep rolling.
        if (projectile instanceof BowlingBulb) {
            handleBulbCollision((BowlingBulb) projectile, target);
            return;
        }

        if (!projectile.pierce()) {
            discard(projectile, true);
        }
    }

    /** Removes a projectile from play, optionally announcing the hit. */
    private void discard(Projectile projectile, boolean dispatchHitEvent) {
        gameModel.removeProjectile(projectile);
        iceDamagedColumns.remove(projectile);
        if (dispatchHitEvent && eventBus != null) {
            eventBus.dispatch(new GameEvent(GameEvent.Type.PROJECTILE_HIT));
        }
    }

    /** True once the shot has flown well past the lawn (not merely off a plantable cell). */
    private boolean leftPlayableArea(Projectile projectile) {
        float x = projectile.getX();
        float y = projectile.getY();
        int cols = gameModel.getColumnCount();
        int rows = gameModel.getRowCount();
        return x < -OFF_GRID_MARGIN
                || x >= cols + OFF_GRID_MARGIN
                || y < -OFF_GRID_MARGIN
                || y > (rows - 1) + OFF_GRID_MARGIN;
    }

    // --- Movement ---

    private void moveProjectile(Projectile projectile, float deltaTime) {
        if (projectile instanceof Splash splash) {
            moveLobbedSplash(splash, deltaTime);
            return;
        }
        // Homing projectiles steer toward their target each tick.
        if (projectile.isHoming()) {
            steerHoming(projectile, deltaTime);
            return;
        }
        float newX = projectile.getX() + projectile.getVelocity() * projectile.getDirection() * deltaTime;
        float newY = projectile.getY() + projectile.getYVelocity() * deltaTime;
        projectile.setX(newX);
        projectile.setY(newY);

        int newRow = Math.round(newY);
        if (newRow != projectile.getRow()) {
            projectile.setRow(newRow);
        }
    }

    /**
     * Interpolates a splash along its throw parabola. If the ability did not
     * call {@link Splash#beginLob}, a default lob is started toward the homing
     * target (or the far edge of the lawn).
     */
    private void moveLobbedSplash(Splash splash, float deltaTime) {
        if (!splash.isLobbing()) {
            float originX = splash.getX();
            float originY = splash.getY();
            float landingX = originX;
            float landingY = originY;
            ZombieInstance target = splash.getHomingTarget();
            if (target != null && !target.isDead() && target.getContinuousPosition() != null) {
                landingX = target.getContinuousX();
                landingY = target.getContinuousY();
            } else {
                int cols = gameModel.getColumnCount();
                landingX = splash.getDirection() >= 0 ? cols - 0.5f : -0.5f;
            }
            splash.beginLob(originX, originY, landingX, landingY);
        }
        splash.advanceLob(deltaTime);
    }

    /** Steers a homing projectile toward its target. */
    private void steerHoming(Projectile projectile, float deltaTime) {
        ZombieInstance target = projectile.getHomingTarget();
        if (target == null || target.isDead() || target.getContinuousPosition() == null) {
            float newX = projectile.getX()
                    + projectile.getVelocity() * projectile.getDirection() * deltaTime;
            float newY = projectile.getY()
                    + projectile.getYVelocity() * deltaTime;
            projectile.setX(newX);
            projectile.setY(newY);

            int newRow = Math.round(newY);
            if (newRow != projectile.getRow()) {
                projectile.setRow(newRow);
            }
            return;
        }

        float dx = target.getContinuousX() - projectile.getX();
        float dy = target.getContinuousY() - projectile.getY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.001f) {
            // Already on the target, let collision detection finish it.
            return;
        }

        float speed = projectile.getVelocity();
        float step = speed * deltaTime;
        // Don't overshoot the target.
        if (step > dist) step = dist;

        float newX = projectile.getX() + (dx / dist) * step;
        float newY = projectile.getY() + (dy / dist) * step;
        projectile.setX(newX);
        projectile.getCurrentPosition().setY(newY);

        // Update the projectile's row so lane-based collision detection
        // still works.
        int newRow = Math.round(newY);
        if (newRow != projectile.getRow()) {
            projectile.setRow(newRow);
        }
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
        if (plant.getAbilityStrategy() instanceof ModifierAbility) {
            multiplier = ((ModifierAbility) plant.getAbilityStrategy()).getTorchwoodDamageMultiplier();
        }
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
        boolean isPlantFired = projectile.getSourcePlant() != null;

        List<ZombieInstance> zombiesInLane = gameModel.getZombiesInLane(lane);
        ZombieInstance best = null;
        float bestDist = Float.MAX_VALUE;

        for (ZombieInstance zombie : zombiesInLane) {
            if (zombie == null || zombie.isDead()) continue;

            // Reflected projectiles travel toward plants, not zombies.
            if (projectile.isReflected()) continue;

            // skip anyone this projectile has already damaged.
            if (projectile.hasAlreadyHit(zombie)) continue;

            if (isPlantFired && zombie.isHypnotized()) {
                continue;
            }

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

    /**
     * Checks if there is any GridItem in the current cell of the projectile,
     * if any exists, and it blocks the projectiles, the projectile would hit it
     * and apply {@link Projectile#getDamage()} damage to it. However, Pushable
     * items collisions with projectiles is handled via {@link ProjectileSystem}.
     *
     * @return {@code true} if the {@code projectile} has hit any grid items.
     */
    private boolean hitGridItemsIfAny(Projectile projectile) {
        int lane = projectile.getRow();
        float projX = projectile.getX();

        Cell cell = gameModel.getCellAt(lane, (int) projX);
        if (cell == null) return false;

        Placeable placeable = cell.getPlaceable(PlacableLayer.GROUND);
        if (!(placeable instanceof GridItem)) return false;

        GridItem item = (GridItem) placeable;
        if (!item.blocksProjectiles() || (item instanceof Pushable)) return false;

        boolean wasAlive = !item.isDestroyed();
        item.takeDamage(projectile.getDamage());
        // Drop loot + remove a grave that just died from a projectile hit.
        if (wasAlive && item.isDestroyed() && item instanceof Grave grave) {
            grave.applyLoot(gameModel);
            cell.removePlaceable(grave);
        }
        return true;
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

        // Attribute the (possibly lethal) damage to this projectile so the
        // Myopoint score level can detect multi-kills from a single source.
        zombie.setLastDamageSource(projectile);

        if (projectile.isFire()) {
            gameModel.attributePlantDamage(zombie, projectile.getSourcePlant());
            zombie.takeFireDamage(damage);
        } else if (projectile.isPoison()) {
            gameModel.attributePlantDamage(zombie, projectile.getSourcePlant());
            zombie.takePoisonDamage(damage);
        } else {
            gameModel.damageZombie(zombie, damage, projectile.getSourcePlant());
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

            zombie.setLastDamageSource(splash);

            if (splash.isFire()) {
                gameModel.attributePlantDamage(zombie, splash.getSourcePlant());
                zombie.takeFireDamage(splash.getDamage());
            } else if (splash.isPoison()) {
                gameModel.attributePlantDamage(zombie, splash.getSourcePlant());
                zombie.takePoisonDamage(splash.getDamage());
            } else {
                gameModel.damageZombie(zombie, splash.getDamage(), splash.getSourcePlant());
            }
        }
    }

    // --- On-hit elemental effects ---

    /** Applies on-hit status effects based on the projectile's element */
    private void applyOnHitEffects(Projectile projectile, ZombieInstance zombie) {
        if (projectile.isIce()) {
            // Frostbite Caves / frostbite-native zombies: chill stacks apply,
            // but they never freeze solid (cap at 2 stacks).
            boolean frostbiteNative = gameModel.getChapter() == Chapter.FROSTBITE_CAVES
                    || (zombie.getDefinition() != null
                    && zombie.getDefinition().getChapter() == Chapter.FROSTBITE_CAVES);
            if (frostbiteNative) {
                if (zombie.getChillLevel() < 2) {
                    zombie.applyChill();
                }
            } else {
                zombie.applyChill();
            }

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

        if (projectile.isButter()) {
            zombie.applyChill();
            zombie.applyChill();
            zombie.applyChill();
        }
    }

    // --- Bowling Bulb physics ---

    private void handleBulbCollision(BowlingBulb bulb, ZombieInstance target) {
        bulb.incrementHitCount();

        if (bulb.isExplosive()) {
            explodeBulb(bulb, target);
            gameModel.removeProjectile(bulb);
            iceDamagedColumns.remove(bulb);
            if (eventBus != null) {
                eventBus.dispatch(new GameEvent(GameEvent.Type.PROJECTILE_HIT));
            }
            return;
        }

        bulb.consumeBounce();
        if (bulb.canBounce()) {
            deflectBulb(bulb);
            return;
        }
        // Bounces exhausted: bulb is consumed on this hit.
        gameModel.removeProjectile(bulb);
        iceDamagedColumns.remove(bulb);
        if (eventBus != null) {
            eventBus.dispatch(new GameEvent(GameEvent.Type.PROJECTILE_HIT));
        }
    }

    /** Reflects a Bowling Bulb when it crosses the top or bottom edge of the lawn. */
    private void bounceOffLaneEdges(BowlingBulb bulb) {
        int rows = gameModel.getRowCount();
        if (rows <= 0) return;
        float y = bulb.getY();
        if (y < 0f) {
            bulb.setY(-y);
            bulb.setYVelocity(-bulb.getYVelocity());
        } else if (y > rows - 1) {
            bulb.setY(2 * (rows - 1) - y);
            bulb.setYVelocity(-bulb.getYVelocity());
        }
        int newRow = Math.round(bulb.getY());
        if (newRow != bulb.getRow()) {
            bulb.setRow(newRow);
        }
    }

    private void deflectBulb(BowlingBulb bulb) {
        float speed = bulb.getVelocity();
        if (bulb.getYVelocity() != 0f) {
            speed = (float) Math.sqrt(
                    bulb.getVelocity() * bulb.getVelocity() + bulb.getYVelocity() * bulb.getYVelocity()
            );
        }
        if (bulb.getHitCount() <= 1 || bulb.getYVelocity() == 0f) {
            int rows = gameModel.getRowCount();
            float sign = (rows <= 1)
                    ? 1f
                    : (bulb.getY() <= (rows - 1) / 2f ? 1f : -1f);
            bulb.setVelocity(speed * DIAGONAL);
            bulb.setYVelocity(sign * speed * DIAGONAL);
        } else {
            bulb.setYVelocity(-bulb.getYVelocity());
        }
    }

    private void explodeBulb(BowlingBulb bulb, ZombieInstance primaryTarget) {
        int centerRow = bulb.getRow();
        int centerCol = Math.round(bulb.getX());
        int damage = bulb.getDamage();
        if (damage <= 0) return;

        List<ZombieInstance> zombies = gameModel.getZombies();
        if (zombies == null) return;
        for (ZombieInstance zombie : new ArrayList<>(zombies)) {
            if (zombie == null || zombie.isDead()) continue;
            if (zombie == primaryTarget) continue;
            int zRow = Math.round(zombie.getContinuousY());
            int zCol = Math.round(zombie.getContinuousX());
            if (Math.abs(zRow - centerRow) <= BULB_EXPLOSION_RADIUS
                    && Math.abs(zCol - centerCol) <= BULB_EXPLOSION_RADIUS) {
                zombie.setLastDamageSource(bulb);
                gameModel.damageZombie(zombie, damage, bulb.getSourcePlant());
            }
        }
    }
    // --- Ice-terrain melting ---

    /**
     * If this projectile is fire-elemental and currently overlies an
     * ice-terrain cell, applies the projectile's damage to the ice block.
     */
    private void damageIceIfPresent(Projectile projectile) {
        if (!projectile.isFire()) {
            return;
        }

        int row = projectile.getRow();
        int col = (int) Math.floor(projectile.getX());
        if (col < 0 || col >= gameModel.getColumnCount()) {
            return;
        }

        Integer lastDamaged = iceDamagedColumns.get(projectile);
        if (lastDamaged != null && lastDamaged == col) {
            return;
        }

        Cell cell = gameModel.getCellAt(row, col);
        if (cell == null) {
            return;
        }
        if (cell.getTerrainStrategy() instanceof IceTerrainStrategy) {
            IceTerrainStrategy ice = (IceTerrainStrategy) cell.getTerrainStrategy();
            ice.takeDamage(projectile.getDamage());
            iceDamagedColumns.put(projectile, col);
        }
    }

    /**
     * A fire-elemental projectile passing over a frozen plant instantly
     * melts the ice coating it (Frostbite Caves spec).
     */
    private void thawFrozenPlantIfPresent(Projectile projectile) {
        if (!projectile.isFire() || projectile instanceof Splash) {
            return;
        }
        int row = projectile.getRow();
        int col = (int) Math.floor(projectile.getX());
        Cell cell = gameModel.getCellAt(row, col);
        if (cell == null) {
            return;
        }
        model.plant.instance.PlantInstance plant = cell.getTopmostPlant();
        if (plant != null && plant.isFrozen()) {
            plant.unfreeze();
        }
    }

    /**
     * Non-lobber projectiles that reach a frozen plant are consumed by the
     * ice/octopus coating.
     *
     * @return true if the projectile was consumed
     */
    private boolean hitFrozenPlantIfBlocking(Projectile projectile) {
        if (projectile instanceof Splash) {
            return false; // lobbers pass over frozen plants
        }
        if (projectile.isReflected()) {
            return false; // reflected pellets use plant-hit path instead
        }

        int row = projectile.getRow();
        int col = (int) Math.floor(projectile.getX());
        PlantInstance plant = gameModel.getPlantAt(row, col);
        if (plant == null || !plant.isFrozen()) {
            return false;
        }

        if (projectile.isFire()) {
            plant.unfreeze();
        } else {
            plant.damageIce(projectile.getDamage());
        }
        discard(projectile, true);
        return true;
    }

    /**
     * Applies a leftward (juggler-reflected) projectile to the first plant
     * it reaches. Ice reflections register freeze hits like Hunter snowballs.
     *
     * @return true if a plant was hit and the projectile consumed
     */
    private boolean hitPlantIfReflected(Projectile projectile) {
        if (projectile.getDirection() >= 0) {
            return false;
        }

        int row = projectile.getRow();
        float projX = projectile.getX();
        float tolerance = 0.5f;

        PlantInstance best = null;
        float bestDist = Float.MAX_VALUE;
        for (PlantInstance plant : gameModel.getPlantsInLane(row)) {
            if (plant == null || plant.getCurrentHP() <= 0) continue;
            if (plant.getPosition() == null) continue;
            float plantX = plant.getPosition().getX();
            float dist = Math.abs(plantX - projX);
            if (dist > tolerance) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = plant;
            }
        }
        if (best == null) {
            return false;
        }

        if (best.isFrozen()) {
            if (projectile.isFire()) {
                best.unfreeze();
            } else {
                best.damageIce(projectile.getDamage());
            }
        } else {
            if (projectile.isIce()) {
                best.registerFreezeHit(ShootBehavior.HUNTER_HITS_TO_FREEZE);
            }
            int damage = projectile.getDamage();
            if (damage > 0) {
                gameModel.damagePlant(best, damage);
            }
        }

        if (!projectile.pierce()) {
            discard(projectile, true);
        }
        return true;
    }
}
