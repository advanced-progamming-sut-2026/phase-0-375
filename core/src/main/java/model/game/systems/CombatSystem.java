package model.game.systems;

import model.event.EventBus;
import model.event.GameEvent;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;

public class CombatSystem implements Tickable {

    private final GameModel gameModel;
    private final EventBus eventBus;

    public CombatSystem(GameModel gameModel, EventBus eventBus) {
        this.gameModel = gameModel;
        this.eventBus = eventBus;
    }

    @Override
    public void tick(float deltaTime) {
        handleStatusEffectDamage(deltaTime);
    }

    /**
     * Advances every zombie's status-effect timers by {@code deltaTime}
     * and applies the per-tick damage (poison, burn). Zombies that drop
     * to 0 HP from status damage are transitioned to {@code DYING} so
     * the {@link ZombieSystem} can finish the death pass.
     */
    private void handleStatusEffectDamage(float deltaTime) {
        List<ZombieInstance> zombies = gameModel.getZombies();
        if (zombies == null || zombies.isEmpty()) return;

        List<ZombieInstance> snapshot = new ArrayList<>(zombies);
        boolean anyStatusApplied = false;

        for (ZombieInstance zombie : snapshot) {
            if (zombie == null || zombie.isDead()) continue;

            int beforeHP = zombie.getCurrentHP();
            zombie.tickStatusEffects(deltaTime);
            int afterHP = zombie.getCurrentHP();

            if (afterHP < beforeHP) {
                anyStatusApplied = true;
                if (afterHP <= 0
                        && zombie.getState() != model.enums.ZombieState.DYING
                        && zombie.getState() != model.enums.ZombieState.DEAD) {
                    zombie.setState(model.enums.ZombieState.DYING);
                }
            }
        }

        if (anyStatusApplied && eventBus != null) {
            eventBus.dispatch(new GameEvent(GameEvent.Type.STATUS_APPLIED));
        }
    }
}
