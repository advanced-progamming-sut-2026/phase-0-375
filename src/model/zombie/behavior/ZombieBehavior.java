package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.zombie.instance.ZombieInstance;

/**
 * Strategy interface for zombie special behaviors
 */
public interface ZombieBehavior {
    /**
     * Performs the behavior's regular action during game flow.
     * Called by the zombie instance on each relevant tick/event.
     *
     * @param zombie the runtime zombie instance providing context
     */
    void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime);

    /**
     * @return the type identifier for this behavior
     */
    ZombieBehaviorType getType();

    /**
     * Called by the ZombieSystem once when the owning zombie dies.
     * Default implementation is a no-op.
     */
    default void onZombieDeath(ZombieInstance zombie, BehaviorContext context) {
        // no-op by default
    }
}