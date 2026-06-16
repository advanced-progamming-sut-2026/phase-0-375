package model.game.wave;

public class EntryRuntime {
    private WaveZombieEntry waveZombieEntry;

    private boolean activated; // has the entry's first-spawn delay been rolled?
    private float firstSpawnAt; // random generated first-spawn time (seconds since wave start)
    private int remainingSpawns; // random generated count, decremented per spawn
    private float nextSpawnAt; // random generated time for the next spawn
    private boolean exhausted; // remainingSpawns == 0
    private boolean groupVolleyFired; // For AMBUSH/

    public EntryRuntime(WaveZombieEntry waveZombieEntry) {
        this.waveZombieEntry = waveZombieEntry;
    }

    public WaveZombieEntry getWaveZombieEntry() {
        return waveZombieEntry;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public float getFirstSpawnAt() {
        return firstSpawnAt;
    }

    public void setFirstSpawnAt(float firstSpawnAt) {
        this.firstSpawnAt = firstSpawnAt;
    }

    public int getRemainingSpawns() {
        return remainingSpawns;
    }

    public void setRemainingSpawns(int remainingSpawns) {
        this.remainingSpawns = remainingSpawns;
    }

    public float getNextSpawnAt() {
        return nextSpawnAt;
    }

    public void setNextSpawnAt(float nextSpawnAt) {
        this.nextSpawnAt = nextSpawnAt;
    }

    public boolean isExhausted() {
        return exhausted;
    }

    public void setExhausted(boolean exhausted) {
        this.exhausted = exhausted;
    }

    public boolean isGroupVolleyFired() {
        return groupVolleyFired;
    }

    public void setGroupVolleyFired(boolean groupVolleyFired) {
        this.groupVolleyFired = groupVolleyFired;
    }

}