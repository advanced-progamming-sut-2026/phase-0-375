package model.game.map;

public class Lane {
    private int rowIndex;
    private LawnMower lawnMower;

    public Lane(int rowIndex) {
        this.rowIndex = rowIndex;
        this.lawnMower = new LawnMower(rowIndex);
    }

    public void triggerLawnMower() {}

    public boolean hasActiveLawnMower() {
        return lawnMower.isActive();
    }
}
