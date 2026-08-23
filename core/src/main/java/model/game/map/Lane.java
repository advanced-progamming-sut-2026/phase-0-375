package model.game.map;

public class Lane {
    private LawnMower lawnMower;

    public Lane() {
        this.lawnMower = new LawnMower();
    }

    public LawnMower getLawnMower() {
        return lawnMower;
    }

    /**
     * Triggers this lane's lawn mower. No-op if it has already fired.
     */
    public void triggerLawnMower() {
        if (lawnMower != null) {
            lawnMower.trigger();
        }
    }

    /**
     * @return true if this lane still has a lawn mower waiting to fire.
     */
    public boolean hasActiveLawnMower() {
        return lawnMower != null && lawnMower.isActive();
    }

    /**
     * @return true if this lane's lawn mower has been triggered and is
     *         currently sweeping the lane.
     */
    public boolean isLawnMowerTriggered() {
        return lawnMower != null && lawnMower.isTriggered();
    }

    public void clearLawnMower() {
        lawnMower = null;
    }
}
