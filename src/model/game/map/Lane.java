package model.game.map;

public class Lane {
    private LawnMower lawnMower;

    public Lane() {
        this.lawnMower = new LawnMower();
    }

    public void triggerLawnMower() {}

    public boolean hasActiveLawnMower() {
        return lawnMower.isActive();
    }
}
