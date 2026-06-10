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
    void execute(ZombieInstance zombie);

    /**
     * @return the type identifier for this behavior
     */
    ZombieBehaviorType getType();
}
