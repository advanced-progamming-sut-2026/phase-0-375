package model.game.map;

public class LawnMower {
    private int row;
    private boolean active = true;
    private boolean isTriggered = false;
    private double xPosInTime;

    public LawnMower(int row) {
        this.row = row;
    }

    public boolean isActive() {
        return active;
    }

    public void trigger() { isTriggered = true; };

    public void tick() { };
}
