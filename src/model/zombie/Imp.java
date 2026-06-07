package model.zombie;

public class Imp extends Zombie {
    public Imp(String name, int baseHP, float speed, float eatDPS) {
        super(name, baseHP, speed, eatDPS);
    }

    public boolean isThrown() {
        return false;
    }
}
