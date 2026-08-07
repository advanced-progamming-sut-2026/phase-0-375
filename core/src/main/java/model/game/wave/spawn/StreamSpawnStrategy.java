package model.game.wave.spawn;

import model.enums.SpawnPatternType;
import model.game.wave.EntryRuntime;
import model.game.wave.Wave;
import model.game.wave.WaveRandomGenerator;
import model.game.wave.WaveZombieEntry;

public class StreamSpawnStrategy extends SpawnStrategy {
    private SpawnPatternType type = SpawnPatternType.STREAM;

    public StreamSpawnStrategy(EntryRuntime runtime, Wave wave) {
        super(runtime, wave);
    }

    @Override
    public void tick(float deltaTime) {
        WaveZombieEntry entry = runtime.getWaveZombieEntry();
        float waveClock = wave.getWaveClock();
        WaveRandomGenerator rng = wave.getRng();

        // Evenly spread remaining spawns over the remaining stream duration
        float streamEnd = runtime.getFirstSpawnAt() + entry.getStreamDurationSeconds();
        float remainingDuration = streamEnd - waveClock;
        if (remainingDuration <= 0f) {
            // Stream ended, dump whatever's left immediately
            while (runtime.getRemainingSpawns() > 0) {
                wave.spawnOne(entry);
                runtime.setRemainingSpawns(runtime.getRemainingSpawns() - 1);
            }
            runtime.setExhausted(true);
            return;
        }
        float interval = remainingDuration / Math.max(1, runtime.getRemainingSpawns());
        while (runtime.getRemainingSpawns() > 0 && waveClock >= runtime.getNextSpawnAt()) {
            wave.spawnOne(entry);
            runtime.setRemainingSpawns(runtime.getRemainingSpawns() - 1);
            runtime.setNextSpawnAt((runtime.getNextSpawnAt() + interval) * rng.nextFloat(0.7f, 1.3f));
            // the above line of code adds some randomness to each interval
        }
        if (runtime.getRemainingSpawns() <= 0) runtime.setExhausted(true);
    }

    public SpawnPatternType getType() {
        return type;
    }
}
