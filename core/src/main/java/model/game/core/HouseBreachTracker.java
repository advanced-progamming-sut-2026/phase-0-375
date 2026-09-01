package model.game.core;

import model.enums.ZombieState;
import model.game.map.FloatPoint;
import model.zombie.instance.ZombieInstance;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** House / brain-lane breach bookkeeping for one level. */
final class HouseBreachTracker {

    private boolean houseBreached;
    private final Set<Integer> breachedRows = new HashSet<>();
    private ZombieInstance breachingZombie;
    private float lastZombieDeathX = Float.NaN;
    private float lastZombieDeathY = Float.NaN;
    private int zombiesKilled;
    private int plantsLost;

    void restore(boolean breached, Set<Integer> breachedLaneRows, int killed, int lost) {
        houseBreached = breached;
        breachedRows.clear();
        if (breachedLaneRows != null) {
            breachedRows.addAll(breachedLaneRows);
        }
        zombiesKilled = Math.max(0, killed);
        plantsLost = Math.max(0, lost);
        breachingZombie = null;
    }

    boolean isHouseBreached() {
        return houseBreached;
    }

    void markHouseBreached() {
        houseBreached = true;
    }

    void markHouseBreached(int row) {
        houseBreached = true;
        breachedRows.add(row);
    }

    void markBrainEaten(int row) {
        breachedRows.add(row);
    }

    void applyHouseBreach(ZombieInstance zombie, int row) {
        markHouseBreached(row);
        breachingZombie = zombie;
        if (zombie == null) {
            return;
        }
        if (zombie.getContinuousPosition() == null) {
            zombie.setContinuousPosition(new FloatPoint(GameModel.HOUSE_CHEW_X, row));
        }
        zombie.setState(ZombieState.EATING);
    }

    Set<Integer> breachedRows() {
        return breachedRows;
    }

    void syncBreachedRows(Collection<Integer> rows) {
        breachedRows.clear();
        if (rows != null) {
            breachedRows.addAll(rows);
        }
    }

    ZombieInstance breachingZombie() {
        return breachingZombie;
    }

    void setBreachingZombie(ZombieInstance zombie) {
        breachingZombie = zombie;
    }

    float lastDeathX() {
        return lastZombieDeathX;
    }

    float lastDeathY() {
        return lastZombieDeathY;
    }

    void recordLastDeath(float continuousX, float row) {
        lastZombieDeathX = continuousX;
        lastZombieDeathY = row;
    }

    int zombiesKilled() {
        return zombiesKilled;
    }

    void incrementZombiesKilled() {
        zombiesKilled++;
    }

    int plantsLost() {
        return plantsLost;
    }

    void incrementPlantsLost() {
        plantsLost++;
    }
}
