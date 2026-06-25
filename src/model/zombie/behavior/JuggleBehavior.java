package model.zombie.behavior;

import model.enums.ZombieBehaviorType;
import model.zombie.instance.ZombieInstance;

public class JuggleBehavior implements ZombieBehavior {
    @Override
    public void execute(ZombieInstance zombie, BehaviorContext context, float deltaTime) {

    }

    @Override
    public ZombieBehaviorType getType() {
        return null;
    }
}
