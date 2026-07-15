package model.game.systems;

import model.enums.ZombieBehaviorType;
import model.event.GameEvent;
import model.event.EventBus;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.game.map.Cell;
import model.game.map.Lane;
import model.enums.ZombieState;
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

        zombie.setContinuousX(newX);

        int newGridX = (int) Math.floor(newX);
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
            cell.getTerrainStrategy().onZombieEnter(zombie.getDefinition(), cell);
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
        }

        if (plant.getCurrentHP() <= 0) {
            if (isHypnoShroom(plant)) {
                hypnotise(zombie);
            }
            zombie.stopEating();
        }
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