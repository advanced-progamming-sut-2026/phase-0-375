package model.game.wave.spawn;

import model.enums.SpawnPatternType;
import model.game.wave.EntryRuntime;
import model.game.wave.Wave;
import model.game.wave.WaveRandomGenerator;
import model.game.wave.WaveZombieEntry;

public class AmbushSpawnStrategy extends SpawnStrategy {
    private final SpawnPatternType type = SpawnPatternType.AMBUSH;

    public AmbushSpawnStrategy(EntryRuntime runtime, Wave wave) {
        super(runtime, wave);
    }

    @Override
    public void tick(float deltaTime) {
        WaveRandomGenerator rng = wave.getRng();
        WaveZombieEntry entry = runtime.getWaveZombieEntry();
        int[] lanes = entry.resolveAllowedLanes(wave.getRowCount());
        // Shuffle a copy so each volley uses a fresh lane order.
        int[] shuffled = lanes.clone();
        wave.shuffle(shuffled);

        int volley = Math.min(runtime.getRemainingSpawns(), shuffled.length);
        for (int k = 0; k < volley; k++) {
            wave.spawnOnLane(entry, shuffled[k]);
            runtime.setRemainingSpawns(runtime.getRemainingSpawns() - 1);
        }
        if (runtime.getRemainingSpawns() > 0) {
            float waveClock = wave.getWaveClock();
            runtime.setNextSpawnAt(waveClock + rng.nextFloat(entry.getMinSpawnInterval(), entry.getMaxSpawnInterval()));
        } else {
            runtime.setExhausted(true);
        }
    }

    public SpawnPatternType getType() {
        return type;
    }
}
