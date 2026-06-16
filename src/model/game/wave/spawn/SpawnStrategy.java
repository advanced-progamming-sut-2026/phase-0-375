package model.game.wave.spawn;


import model.enums.SpawnPatternType;
import model.game.core.Tickable;
import model.game.wave.EntryRuntime;
import model.game.wave.Wave;
import model.game.wave.WaveZombieEntry;

public abstract class SpawnStrategy implements Tickable {
    protected EntryRuntime runtime;
    protected Wave wave;

    public SpawnStrategy(EntryRuntime runtime, Wave wave) {
        this.runtime = runtime;
        this.wave = wave;
    }

    abstract SpawnPatternType getType();
}
