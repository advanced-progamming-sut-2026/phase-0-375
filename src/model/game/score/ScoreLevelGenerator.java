package model.game.score;

import model.data.level.LevelDataEntry;
import model.data.level.LevelRegistry;
import model.game.level.LevelConfig;
import model.game.level.special.ScoreLevel;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds the daily Myopoint score level.
 *
 * <p>The layout (waves, zombie pools, counts and timings) is generated from a
 * {@link Random} seeded with the current date ({@link LocalDate#toEpochDay()}),
 * so every player faces the same level on a given day and a fresh one appears
 * at midnight. Difficulty ramps up wave by wave.
 */
public final class ScoreLevelGenerator {

    /** Level id used for the generated score level (outside normal chapters). */
    public static final int SCORE_LEVEL_ID = 9001;

    /** Number of waves in every daily score level. */
    public static final int WAVE_COUNT = 5;

    /** Zombies that may appear; index 0 is the filler, the rest ramp in later. */
    private static final String[] ZOMBIE_POOL = {
            "ZombieDefault", "ZombieArmor1", "ZombieRa", "ZombieExplorer", "ZombieTombRaiser"
    };

    private ScoreLevelGenerator() {}

    /** Builds today's score level (identical for every player on the same date). */
    public static ScoreLevel createDailyLevel() throws IOException {
        return createLevel(LocalDate.now().toEpochDay());
    }

    /** Builds the score level for an explicit seed (used by the daily variant). */
    public static ScoreLevel createLevel(long seed) throws IOException {
        LevelConfig config = LevelRegistry.toConfig(buildEntry(new Random(seed)));
        return new ScoreLevel(config);
    }

    private static LevelDataEntry buildEntry(Random random) {
        LevelDataEntry entry = new LevelDataEntry();
        entry.setChapter("ANCIENT_EGYPT");
        entry.setLevelId(SCORE_LEVEL_ID);
        entry.setLevelType("NORMAL");
        entry.setRows(5);
        entry.setColumns(9);

        List<LevelDataEntry.WaveData> waves = new ArrayList<>();
        for (int waveNumber = 1; waveNumber <= WAVE_COUNT; waveNumber++) {
            waves.add(buildWave(random, waveNumber));
        }
        entry.setWaves(waves);
        return entry;
    }

    private static LevelDataEntry.WaveData buildWave(Random random, int waveNumber) {
        LevelDataEntry.WaveData wave = new LevelDataEntry.WaveData();
        wave.setWaveNumber(waveNumber);
        wave.setStartDelay(waveNumber == 1 ? 10f : 12f);
        wave.setHugeWave(waveNumber == WAVE_COUNT);
        wave.setFinalWave(waveNumber == WAVE_COUNT);

        // Difficulty ramp: later waves pull more tough zombies into the pool.
        List<LevelDataEntry.ZombieCandidateData> pool = new ArrayList<>();
        pool.add(candidate(ZOMBIE_POOL[0], 3.0));
        int extraPicks = Math.min(waveNumber, ZOMBIE_POOL.length - 1);
        for (int i = 0; i < extraPicks; i++) {
            String name = ZOMBIE_POOL[1 + random.nextInt(ZOMBIE_POOL.length - 1)];
            pool.add(candidate(name, 1.0 + waveNumber * 0.25));
        }

        LevelDataEntry.WaveEntryData entryData = new LevelDataEntry.WaveEntryData();
        entryData.setPool(pool);
        int count = 3 + waveNumber + random.nextInt(2);
        entryData.setMinCount(count);
        entryData.setMaxCount(count);
        entryData.setMinSpawnDelay(1f);
        entryData.setMaxSpawnDelay(3f);
        entryData.setMinSpawnInterval(Math.max(2f, 8f - waveNumber));
        entryData.setMaxSpawnInterval(Math.max(4f, 12f - waveNumber));
        entryData.setPattern(waveNumber == WAVE_COUNT ? "GROUP" : "SINGLE");

        List<LevelDataEntry.WaveEntryData> entries = new ArrayList<>();
        entries.add(entryData);
        wave.setEntries(entries);
        return wave;
    }

    private static LevelDataEntry.ZombieCandidateData candidate(String zombie, double weight) {
        LevelDataEntry.ZombieCandidateData data = new LevelDataEntry.ZombieCandidateData();
        data.setZombie(zombie);
        data.setWeight(weight);
        return data;
    }
}
