package model.game.map;

public class LawnMower {
    private boolean active = true;
    private boolean isTriggered = false;
    private double xPosInTime = 0;

    public LawnMower() {

    }

    public boolean isActive() {
        return active;
    }

    public void trigger() { isTriggered = true; };

    public void tick() { };
}
