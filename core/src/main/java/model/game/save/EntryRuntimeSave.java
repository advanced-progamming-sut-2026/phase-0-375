package model.game.save;

/** Snapshot of one wave-entry spawn runtime. */
public class EntryRuntimeSave {
    private boolean activated;
    private float firstSpawnAt;
    private int remainingSpawns;
    private float nextSpawnAt;
    private boolean exhausted;
    private boolean groupVolleyFired;

    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) { this.activated = activated; }
    public float getFirstSpawnAt() { return firstSpawnAt; }
    public void setFirstSpawnAt(float firstSpawnAt) { this.firstSpawnAt = firstSpawnAt; }
    public int getRemainingSpawns() { return remainingSpawns; }
    public void setRemainingSpawns(int remainingSpawns) { this.remainingSpawns = remainingSpawns; }
    public float getNextSpawnAt() { return nextSpawnAt; }
    public void setNextSpawnAt(float nextSpawnAt) { this.nextSpawnAt = nextSpawnAt; }
    public boolean isExhausted() { return exhausted; }
    public void setExhausted(boolean exhausted) { this.exhausted = exhausted; }
    public boolean isGroupVolleyFired() { return groupVolleyFired; }
    public void setGroupVolleyFired(boolean groupVolleyFired) { this.groupVolleyFired = groupVolleyFired; }
}
