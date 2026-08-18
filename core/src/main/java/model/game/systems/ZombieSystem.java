package model.game.systems;

import model.enums.PlantCategory;
import model.enums.PlantTags;
import model.enums.ZombieBehaviorType;
import model.event.GameEvent;
import model.event.EventBus;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.game.map.Cell;
import model.game.map.Lane;
import model.enums.ZombieState;
import model.plant.ability.PlantAbilityContext;
import model.plant.ability.WallAbility;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.zombie.behavior.BehaviorContext;
import model.zombie.behavior.EnrageBehavior;
import model.item.pushable.Barrel;
import model.item.pushable.Piano;
import model.enums.LootPickupKind;
import model.item.LootPickup;
import model.item.PlantFoodPickup;
import model.zombie.behavior.FlyBehavior;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ZombieSystem implements Tickable {

    /**
     * Maximum distance (in grid units) between two opposing zombies for
     * them to start biting each other. Anything above this and they
     * just walk past. Tuned to match the projectile-collision tolerance
     * used by {@code ProjectileSystem} so a hypnotized zombie that has
     * stopped to bite a plant-eating zombie doesn't get shot off it.
     */
    private static final float ZOMBIE_COMBAT_RANGE = 0.7f;

    private final GameModel gameModel;
    private final EventBus eventBus;

    private static final float LOOT_DROP_CHANCE = 0.10f;
    private final java.util.Random lootRandom = new java.util.Random();
    /** Death tile for {@link #maybeDropLoot}; set only during the death pass. */
    private model.game.map.Point lastDeadZombiePos;

    public ZombieSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {
        List<ZombieInstance> zombies = gameModel.getZombies();
        if (zombies == null || zombies.isEmpty()) return;

        BehaviorContext context = gameModel;

        List<ZombieInstance> snapshot = new ArrayList<>(zombies);

        for (ZombieInstance zombie : snapshot) {
            if (zombie == null) continue;

            // Internal state
            zombie.tick(deltaTime);

            // Skip dead/dying zombies except for the final cleanup pass.
            if (zombie.isDead()) {
                continue;
            }

            // Behavior tick
            zombie.tickBehaviors(deltaTime, context);

            // Movement
            if (canMove(zombie)) {
                moveZombie(zombie, deltaTime, context);
            }

            // Cell-entry event
            handleEating(zombie, context, deltaTime);
        }

        // Death pass
        processDeadZombies(zombies, context);
    }

    // --- Movement ---

    /** Moves a zombie based on its current speed, status modifiers, and direction. */
    private void moveZombie(ZombieInstance zombie, float deltaTime, BehaviorContext context) {
        float effectiveSpeed = zombie.getCurrentSpeed();

        if (zombie.isChilled()) {
            effectiveSpeed *= 0.5f;
        }

        float deltaX = effectiveSpeed * deltaTime;
        if (zombie.isMovingBackward()) {
            deltaX = -deltaX;
        }

        float newX = zombie.getContinuousX() - deltaX;

        // End-of-lane handling
        if (!zombie.isMovingBackward() && newX < 0f) {
            onZombieReachedHouse(zombie, context);
            return;
        }
        if (zombie.isMovingBackward() && newX >= context.getColumnCount()) {
            killSilently(zombie);
            return;
        }

        // Terrain passability: don't let the zombie step into a cell
        // its terrain doesn't allow
        int newGridX = (int) Math.floor(newX);
        if (newGridX != zombie.getGridX()) {
            Cell targetCell = context.getCellAt(zombie.getGridY(), newGridX);
            if (targetCell != null && targetCell.getTerrainStrategy() != null
                    && !targetCell.getTerrainStrategy().isPassable(zombie, targetCell)) {
                return;
            }
        }

        zombie.setContinuousX(newX);
        if (newGridX != zombie.getGridX()) {
            zombie.setGridX(newGridX);
            onZombieEnteredCell(zombie, context);
        }
    }

    /**
     * @return true if the zombie is allowed to move this tick.
     */
    private boolean canMove(ZombieInstance zombie) {
        ZombieState state = zombie.getState();
        if (state == ZombieState.EATING
                || state == ZombieState.PUSHING
                || state == ZombieState.SPECIAL_ACTION
                || state == ZombieState.STUNNED
                || state == ZombieState.SPAWNING
                || state == ZombieState.DYING
                || state == ZombieState.DEAD) {
            return false;
        }
        return !zombie.isFrozen();
    }

    /** Called when a zombie enters a new grid cell. */
    private void onZombieEnteredCell(ZombieInstance zombie, BehaviorContext context) {
        int row = zombie.getGridY();
        int col = zombie.getGridX();
        if (row < 0 || col < 0
                || row >= context.getRowCount()
                || col >= context.getColumnCount()) {
            return;
        }

        // Apply terrain effects.
        Cell cell = context.getCellAt(row, col);
        if (cell != null && cell.getTerrainStrategy() != null) {
            cell.getTerrainStrategy().onZombieEnter(zombie, cell, context);
        }
    }

    /**
     * Handles a zombie reaching the end of its lane.
     * If lawn mowers are enabled and the lane still has a mower, it triggers
     * and the zombie waits for blade contact; otherwise the game is lost.
     */
    private void onZombieReachedHouse(ZombieInstance zombie, BehaviorContext context) {
        int row = zombie.getGridY();
        Lane lane = gameModel.getMap().getLane(row);
        if (lawnMowersEnabled() && lane != null && lane.hasActiveLawnMower()) {
            lane.triggerLawnMower();
            if (eventBus != null) {
                eventBus.dispatch(new GameEvent(GameEvent.Type.LAWN_MOWER_TRIGGERED));
            }
            // Hold here until the blade makes contact in LawnMowerSystem.
        } else if (lawnMowersEnabled() && lane != null && lane.isLawnMowerTriggered()) {
            // Already sweeping this lane — wait for contact, don't lose.
        } else {
            // No mower, the zombie got through.
            gameModel.markHouseBreached(row);
            if (eventBus != null) {
                eventBus.dispatch(new GameEvent(GameEvent.Type.ZOMBIE_REACHED_END));
                eventBus.dispatch(new GameEvent(GameEvent.Type.GAME_LOST));
            }
            zombie.setState(ZombieState.DYING);
        }
    }

    /** Lawn mowers only defend the house when the level's rules enable them. */
    private boolean lawnMowersEnabled() {
        return gameModel.getCurrentLevel() != null
                && gameModel.getCurrentLevel().getConfig() != null
                && gameModel.getCurrentLevel().getConfig().getRules() != null
                && gameModel.getCurrentLevel().getConfig().getRules().isLawnMowersEnabled();
    }

    // --- Eating ---

    /**
     * Each tick, if the zombie has stepped onto the facing border of a tile
     * that holds a live plant, and is not in a special-action, it eats.
     */
    private void handleEating(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        boolean hypnotized = zombie.isHypnotized();

        int row = zombie.getGridY();
        int col = zombie.getGridX();
        if (row < 0 || col < 0
                || row >= context.getRowCount()
                || col >= context.getColumnCount()) {
            return;
        }

        syncFlyBehavior(zombie, context.getPlantAt(row, col));

        if (!hypnotized && isEatingSuppressed(zombie)) {
            if (zombie.isEating()) {
                zombie.stopEating();
            }
            return;
        }

        if (fightOpposingZombieIfAny(zombie, context, row, deltaTime)) return;

        if (zombie.getCombatTargetZombie() != null) {
            zombie.stopEating();
        }

        if (hypnotized) {
            // Hypnotized zombies never eat plants.
            if (zombie.isEating() && zombie.getEatingTarget() != null) {
                zombie.stopEating();
            }
            return;
        }

        int eatCol = zombie.plantColumnAtFacingBorder();
        if (eatCol < 0 || eatCol >= context.getColumnCount()) {
            if (zombie.isEating()) {
                zombie.stopEating();
            }
            return;
        }
        eatPlantAt(zombie, context, row, eatCol, deltaTime);
    }

    /** Keeps {@link FlyBehavior} in sync with the plant under the zombie. */
    private void syncFlyBehavior(ZombieInstance zombie, PlantInstance plant) {
        if (!zombie.hasBehavior(ZombieBehaviorType.FLY)) return;
        FlyBehavior fly = (FlyBehavior) zombie.getBehavior(ZombieBehaviorType.FLY);
        if (fly != null) {
            fly.syncToPlant(zombie, plant);
        }
    }

    /** Special actions and movement modes during which a zombie cannot eat. */
    private boolean isEatingSuppressed(ZombieInstance zombie) {
        if (zombie.getState() == ZombieState.SPECIAL_ACTION) return true;
        if (zombie.hasBehavior(ZombieBehaviorType.FISH)) return true;
        if (zombie.hasBehavior(ZombieBehaviorType.BUFF)) return true;
        if (zombie.hasBehavior(ZombieBehaviorType.SMASH) && !isAllStar(zombie)) return true;
        if (zombie.hasBehavior(ZombieBehaviorType.TRANSFORM)) return true;
        return zombie.isFlying() || zombie.isSubmerged() || zombie.isPushing()
                || zombie.getPushableItem() instanceof Piano
                || zombie.getPushableItem() instanceof Barrel;
    }

    /**
     * Hypnotized-vs-normal zombie combat: if an opposing zombie is in bite
     * range, bite it instead of any plant.
     *
     * @return true if an opposing zombie was engaged this tick
     */
    private boolean fightOpposingZombieIfAny(ZombieInstance zombie, BehaviorContext context,
                                             int row, float deltaTime) {
        ZombieInstance enemy = findOpposingZombieNearby(zombie, context, row);
        if (enemy == null) return false;

        if (!zombie.isEating() || zombie.getCombatTargetZombie() != enemy) {
            zombie.startFightingZombie(enemy);
        }

        int damage = biteDamage(zombie, deltaTime);
        if (damage > 0) {
            context.damageZombie(enemy, damage);
        }

        if (enemy.isDead()) {
            zombie.stopEating();
        }
        return true;
    }

    /** Damage of one tick's worth of biting, scaled by status effects. */
    private int biteDamage(ZombieInstance zombie, float deltaTime) {
        float eatDPS = zombie.getDefinition().getEatDPS();
        eatDPS *= getEatDamageScale(zombie);
        eatDPS *= gameModel.difficultyBoost();
        return zombie.addEatDamage(eatDPS * deltaTime);
    }

    /** Bites the plant on the tile whose facing border the zombie has stepped onto. */
    private void eatPlantAt(ZombieInstance zombie, BehaviorContext context,
                            int row, int col, float deltaTime) {
        PlantInstance plant = context.getPlantAt(row, col);
        if (plant == null || plant.getCurrentHP() <= 0 || plant.isTransformed()) {
            if (zombie.isEating()) {
                zombie.stopEating();
            }
            return;
        }

        // Dodo (and other flyers): never chew plants marked as fly-over targets.
        if (zombie.hasBehavior(ZombieBehaviorType.FLY)) {
            FlyBehavior fly = (FlyBehavior) zombie.getBehavior(ZombieBehaviorType.FLY);
            if (fly != null && fly.shouldFlyOver(plant)) {
                if (zombie.isEating()) {
                    zombie.stopEating();
                }
                return;
            }
        }

        if (!zombie.isEating()) {
            zombie.startEating(plant);
        }

        int damage = biteDamage(zombie, deltaTime);
        if (damage > 0) {
            context.damagePlant(plant, damage);

            // Sun Bean: every bite the zombie takes drops sun next to
            // the plant. The drop only fires while the plant still has
            // HP left (i.e. it's still being eaten, not just destroyed).
            if (plant.getCurrentHP() > 0 && isSunBean(plant)) {
                WallAbility.onSunBeanBitten(plant, sunBeanContext());
            }
        }

        if (plant.getCurrentHP() <= 0) {
            if (isHypnoShroom(plant)) {
                hypnotise(zombie);
            }
            zombie.stopEating();
        }
    }

    /**
     * Searches the zombie's lane for the closest opposing zombie within
     * {@link #ZOMBIE_COMBAT_RANGE} grid units.
     */
    private ZombieInstance findOpposingZombieNearby(ZombieInstance zombie,
                                                    BehaviorContext context,
                                                    int row) {
        ZombieInstance best = null;
        float bestDist = Float.MAX_VALUE;
        boolean hypnotized = zombie.isHypnotized();

        for (ZombieInstance other : context.getZombiesInLane(row)) {
            if (other == null || other == zombie || other.isDead()) continue;
            if (other.isHypnotized() == hypnotized) continue;
            if (other.isFlying() || other.isSubmerged()) continue;
            if (other.getState() == ZombieState.SPAWNING) continue;

            float dist = Math.abs(other.getContinuousX() - zombie.getContinuousX());
            if (dist > ZOMBIE_COMBAT_RANGE) continue;

            if (dist < bestDist) {
                bestDist = dist;
                best = other;
            }
        }
        return best;
    }

    /** @return true if the given plant is a Sun Bean (WALL_NUT with SUN tag). */
    private boolean isSunBean(PlantInstance plant) {
        if (plant == null) return false;
        Plant def = plant.getDefinition();
        if (def == null) return false;
        if (def.getCategory() != PlantCategory.WALL_NUT) return false;
        return def.hasTag(PlantTags.SUN);
    }

    /**
     * Returns the {@link PlantAbilityContext} used
     * by the plant system. Because the zombie system does not own a
     * context, we build a thin adapter that forwards to the game model.
     */
    private PlantAbilityContext sunBeanContext() {
        return new GameModelAbilityContext(gameModel);
    }

    /** @return true if the given plant is a Hypno-shroom. */
    private boolean isHypnoShroom(PlantInstance plant) {
        if (plant == null) return false;
        model.plant.definition.Plant def = plant.getDefinition();
        if (def == null) return false;
        if (def.getCategory() != model.enums.PlantCategory.MODIFIER) return false;
        String name = def.getName();
        return name != null && name.toLowerCase().contains("hypno");
    }

    /** Hypnotizes a zombie. */
    private void hypnotise(ZombieInstance zombie) {
        if (zombie == null) return;
        zombie.setState(ZombieState.HYPNOTIZED);
        zombie.setMovingBackward(true);
        if (eventBus != null) {
            eventBus.dispatch(new GameEvent(GameEvent.Type.STATUS_APPLIED));
        }
    }

    /** @return the eat-damage multiplier. */
    private float getEatDamageScale(ZombieInstance zombie) {
        EnrageBehavior enrage = (EnrageBehavior) zombie.getBehavior(
                ZombieBehaviorType.ENRAGE);
        return enrage == null ? 1.0f : enrage.getEatDamageScale();
    }

    private boolean isAllStar(ZombieInstance zombie) {
        String name = zombie.getDefinition().getName();
        return name != null && name.toLowerCase().contains("allstar");
    }

    // --- Death pass ---

    /**
     * For every zombie that has just died, fires on-death behaviors,
     * drops plant food if glowing, removes the zombie from the field.
     */
    private void processDeadZombies(List<ZombieInstance> zombies, BehaviorContext context) {
        Iterator<ZombieInstance> iterator = zombies.iterator();
        while (iterator.hasNext()) {
            ZombieInstance zombie = iterator.next();
            if (zombie == null) continue;

            if (zombie.getState() == ZombieState.DYING) {
                zombie.fireOnDeathBehaviors(context);

                // Drop plant food on the death tile if glowing (click to collect).
                if (zombie.isGlowing()) {
                    var pos = zombie.getGridPosition();
                    if (pos != null) {
                        gameModel.spawnPlantFood(new PlantFoodPickup(pos.getX(), pos.getY()));
                    }
                }

                zombie.setState(ZombieState.DEAD);
                gameModel.recordZombieKilled(zombie);
                gameModel.notifyZombieKilledForScore(zombie);

                gameModel.recordZombieKilled(zombie);
                gameModel.notifyZombieKilledForScore(zombie);
                lastDeadZombiePos = zombie.getGridPosition();
                maybeDropLoot();
                lastDeadZombiePos = null;

                // Spec notification: "Zombie of type <type> is dead at (<x>, <y>)"
                var pos = zombie.getGridPosition();
                int deadX = pos != null ? pos.getX() : -1;
                int deadY = pos != null ? pos.getY() : -1;
                String typeName = zombie.getDefinition() != null
                        ? zombie.getDefinition().getName() : "Unknown";
                model.app.App.logToShell("Zombie of type " + typeName
                        + " is dead at (" + deadX + ", " + deadY + ")");

                if (eventBus != null) {
                    eventBus.dispatch(new GameEvent(GameEvent.Type.ZOMBIE_KILLED));
                }
            }

            if (zombie.getState() == ZombieState.DEAD) {
                iterator.remove();
                gameModel.removeZombie(zombie);
            }
        }
    }

    /** Kills the zombie without firing any on-death behaviors. */
    private void killSilently(ZombieInstance zombie) {
        zombie.setCurrentHP(0);
        zombie.setState(ZombieState.DEAD);
    }

    private void maybeDropLoot() {
        if (lootRandom.nextFloat() >= LOOT_DROP_CHANCE) {
            return;
        }
        var pos = lastDeadZombiePos;
        if (pos == null) {
            return;
        }
        int col = pos.getX();
        int row = pos.getY();
        int roll = lootRandom.nextInt(3);
        switch (roll) {
            case 0 -> gameModel.spawnLootPickup(new LootPickup(LootPickupKind.DIAMOND, 1, col, row));
            case 1 -> {
                LootPickupKind coinKind = lootRandom.nextBoolean()
                    ? LootPickupKind.COIN_GOLD
                    : LootPickupKind.COIN_SILVER;
                gameModel.spawnLootPickup(new LootPickup(coinKind, 50, col, row));
            }
            case 2 -> gameModel.spawnLootPickup(new LootPickup(LootPickupKind.FLOWER_POT, 1, col, row));
            default -> { }
        }
    }
}
