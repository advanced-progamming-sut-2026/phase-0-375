package model.game.wave.spawn;

import model.enums.SpawnPatternType;
import model.game.wave.EntryRuntime;
import model.game.wave.Wave;
import model.game.wave.WaveZombieEntry;

/**
 * Zombies appear one at a time.
 */
public class SingleSpawnStrategy extends SpawnStrategy {
    private final SpawnPatternType type = SpawnPatternType.SINGLE;

    public SingleSpawnStrategy(EntryRuntime runtime, Wave wave) {
        super(runtime, wave);
    }

    @Override
    public void tick(float deltaTime) {
        while (runtime.getRemainingSpawns() > 0 && wave.getWaveClock() >= runtime.getNextSpawnAt()) {
            WaveZombieEntry entry = runtime.getWaveZombieEntry();

            wave.spawnOne(runtime.getWaveZombieEntry());
            runtime.setRemainingSpawns(runtime.getRemainingSpawns() - 1);
            if (runtime.getRemainingSpawns() > 0) {
                runtime.setNextSpawnAt(
                        runtime.getNextSpawnAt() + wave.getRng().nextFloat(
                                entry.getMinSpawnInterval(), entry.getMaxSpawnInterval()
                        )
                );
            }
        }
        if (runtime.getRemainingSpawns() <= 0) runtime.setExhausted(true);
    }

    public SpawnPatternType getType() {
        return type;
    }
}
