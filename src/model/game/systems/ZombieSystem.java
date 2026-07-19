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
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ZombieSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

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
     * If the lane has a lawn mower waiting, it triggers
     * the mower and dies; otherwise the game is lost.
     */
    private void onZombieReachedHouse(ZombieInstance zombie, BehaviorContext context) {
        int row = zombie.getGridY();
        Lane lane = gameModel.getMap().getLane(row);
        if (lane != null && lane.hasActiveLawnMower()) {
            lane.triggerLawnMower();
            if (eventBus != null) {
                eventBus.dispatch(new GameEvent(GameEvent.Type.LAWN_MOWER_TRIGGERED));
            }
            // The mower immediately kills the triggering zombie.
            killSilently(zombie);
        } else {
            // No mower, the zombie got through.
            gameModel.markHouseBreached();
            if (eventBus != null) {
                eventBus.dispatch(new GameEvent(GameEvent.Type.ZOMBIE_REACHED_END));
                eventBus.dispatch(new GameEvent(GameEvent.Type.GAME_LOST));
            }
            zombie.setState(ZombieState.DYING);
        }
    }

    // --- Eating ---

    /**
     * Each tick, if the zombie is on a cell with a live plant and the zombie
     * is not currently in a special-action, the zombie eats the plant.
     */
    private void handleEating(ZombieInstance zombie, BehaviorContext context, float deltaTime) {
        if (zombie.hasBehavior(ZombieBehaviorType.SMASH)
                && !isAllStar(zombie)) {
            return;
        }
        if (zombie.hasBehavior(ZombieBehaviorType.TRANSFORM)) {
            return;
        }
        if (zombie.isFlying() || zombie.isSubmerged() || zombie.isPushing()) {
            return;
        }

        int row = zombie.getGridY();
        int col = zombie.getGridX();
        if (row < 0 || col < 0
                || row >= context.getRowCount()
                || col >= context.getColumnCount()) {
            return;
        }

        PlantInstance plant = context.getPlantAt(row, col);
        if (plant == null || plant.getCurrentHP() <= 0 || plant.isTransformed()) {
            if (zombie.isEating()) {
                zombie.stopEating();
            }
            return;
        }

        if (!zombie.isEating()) {
            zombie.startEating(plant);
        }

        float eatDPS = zombie.getDefinition().getEatDPS();
        eatDPS *= getEatDamageScale(zombie);

        int damage = (int) (eatDPS * deltaTime);
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
        return new PlantAbilityContext() {
            @Override public int getSunAmount() { return gameModel.getSunAmount(); }
            @Override public int getRowCount() { return gameModel.getRowCount(); }
            @Override public int getColumnCount() { return gameModel.getColumnCount(); }
            @Override public PlantInstance getPlantAt(int row, int col) { return gameModel.getPlantAt(row, col); }
            @Override public List<PlantInstance> getPlantsInLane(int lane) { return gameModel.getPlantsInLane(lane); }
            @Override public List<PlantInstance> getAllPlants() { return gameModel.getAllPlants(); }
            @Override public List<ZombieInstance> getZombiesInLane(int lane) { return gameModel.getZombiesInLane(lane); }
            @Override public List<ZombieInstance> getZombiesInArea(int row, int col, int rowRadius, int colRadius) {
                return gameModel.getZombiesInArea(row, col, rowRadius, colRadius);
            }
            @Override public boolean hasZombieInLane(int lane) {
                return !gameModel.getZombiesInLane(lane).isEmpty();
            }
            @Override public boolean hasAdjacentZombie(int row, int col) {
                for (int rowDist = -1; rowDist <= 1; rowDist++) {
                    for (int colDist = -1; colDist <= 1; colDist++) {
                        if (rowDist == 0 && colDist == 0) continue;
                        if (!gameModel.getZombiesInArea(row + rowDist, col + colDist, 0, 0).isEmpty()) {
                            return true;
                        }
                    }
                }
                return false;
            }
            @Override public boolean isNightLevel() { return gameModel.isNightLevel(); }
            @Override public boolean isWaterTile(int row, int col) { return gameModel.isWaterTile(row, col); }
            @Override public model.projectile.Projectile spawnProjectile(model.projectile.Projectile p, float x, float y) {
                gameModel.spawnProjectile(p, (int) x, (int) y);
                return p;
            }
            @Override public void spawnSun(model.item.Sun sun) { gameModel.spawnSun(sun); }
            @Override public void addSun(int amount) { gameModel.addSun(amount); }
            @Override public void damageZombie(ZombieInstance zombie, int damage) { gameModel.damageZombie(zombie, damage); }
            @Override public void damagePlant(PlantInstance plant, int damage) { gameModel.damagePlant(plant, damage); }
            @Override public void destroyPlant(PlantInstance plant) { gameModel.destroyPlant(plant); }
            @Override public boolean placePlant(PlantInstance plant, int row, int col) { return gameModel.placePlant(plant, row, col); }
            @Override public boolean moveZombieToLane(ZombieInstance zombie, int newRow) { return gameModel.moveZombieToLane(zombie, newRow); }
            @Override public void pushZombieBack(ZombieInstance zombie, float tiles) { gameModel.pushZombieBack(zombie, tiles); }
            @Override public void triggerFamilyPlantFood(model.enums.PlantCategory family) {
                for (PlantInstance plant : new ArrayList<>(gameModel.getAllPlants())) {
                    if (plant.getDefinition().getCategory() == family) {
                        plant.activatePlantFood();
                    }
                }
            }
            @Override public void damageIceInArea(int row, int col, int rowRadius, int colRadius, int damage) {
                gameModel.damageIceInArea(row, col, rowRadius, colRadius, damage);
            }
        };
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

                // Drop plant food if glowing.
                if (zombie.isGlowing()) {
                    gameModel.addPlantFood();
                }

                zombie.setState(ZombieState.DEAD);
                gameModel.recordZombieKilled(zombie);

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
}