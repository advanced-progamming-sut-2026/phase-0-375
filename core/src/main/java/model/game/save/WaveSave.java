package model.game.save;

import model.enums.WaveState;

import java.util.ArrayList;
import java.util.List;

/** Snapshot of one wave's runtime. */
public class WaveSave {
    private WaveState state = WaveState.PENDING;
    private float waveClock;
    private List<EntryRuntimeSave> entries = new ArrayList<>();

    public WaveState getState() { return state; }
    public void setState(WaveState state) { this.state = state; }
    public float getWaveClock() { return waveClock; }
    public void setWaveClock(float waveClock) { this.waveClock = waveClock; }
    public List<EntryRuntimeSave> getEntries() { return entries; }
    public void setEntries(List<EntryRuntimeSave> entries) {
        this.entries = entries == null ? new ArrayList<>() : entries;
    }
}
