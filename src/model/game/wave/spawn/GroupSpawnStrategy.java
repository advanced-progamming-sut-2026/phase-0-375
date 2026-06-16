package model.game.wave.spawn;

import model.enums.SpawnPatternType;
import model.game.wave.EntryRuntime;
import model.game.wave.Wave;
import model.game.wave.WaveRandomGenerator;
import model.game.wave.WaveZombieEntry;

public class GroupSpawnStrategy extends SpawnStrategy {
    private final SpawnPatternType pattern = SpawnPatternType.GROUP;

    public GroupSpawnStrategy(EntryRuntime runtime, Wave wave) {
        super(runtime, wave);
    }

    @Override
    public void tick(float deltaTime) {
        WaveZombieEntry entry = runtime.getWaveZombieEntry();

        if (runtime.isGroupVolleyFired()) {
            runtime.setExhausted(true);
            return;
        }
        // Fire the whole group at once. Cap by rowCount so we don't try
        // to place 10 zombies on a 5-row map simultaneously; the surplus
        // is deferred to a second volley after a short cooldown.
        int volley = Math.min(runtime.getRemainingSpawns(), wave.getRowCount());
        for (int k = 0; k < volley; k++) {
            wave.spawnOne(entry);
            runtime.setRemainingSpawns(runtime.getRemainingSpawns() - 1);
        }
        if (runtime.getRemainingSpawns() > 0) {
            runtime.setGroupVolleyFired(false);
            float waveClock = wave.getWaveClock();
            WaveRandomGenerator rng = wave.getRng();
            runtime.setNextSpawnAt(waveClock + rng.nextFloat(entry.getMinSpawnInterval(), entry.getMaxSpawnInterval()));
        } else {
            runtime.setGroupVolleyFired(true);
            runtime.setExhausted(true);
        }
    }

    public SpawnPatternType getType() {
        return pattern;
    }
}
