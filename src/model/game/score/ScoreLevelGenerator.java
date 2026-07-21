package model.game.score;

import model.data.level.LevelDataEntry;
import model.data.level.LevelRegistry;
import model.game.level.LevelConfig;
import model.game.level.special.ScoreLevel;
import model.zombie.ZombieFactory;
import model.zombie.definition.Zombie;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Builds the daily Myopoint score level.
 *
 * <p>The layout (waves, zombie pools, counts and timings) is generated from a
 * {@link Random} seeded with the current date ({@link LocalDate#toEpochDay()}),
 * so every player faces the same level on a given day and a fresh one appears
 * at midnight. Difficulty ramps up wave by wave.
 *
 * <p>The candidate pool is built from <em>every</em> zombie definition in
 * {@code zombies.json} that can be wave-spawned. Definitions are sorted from
 * easiest to toughest (wave-point cost, then base HP); early waves may only
 * draw from the easy end of that list while the final wave can draw from the
 * whole roster, Gargantuar included. Minigame-only definitions (Zombotany
 * plant-heads, the I,Zombie sun producer) and stationary or zero-weight
 * definitions are excluded because the wave engine cannot meaningfully spawn
 * them on a regular lawn.
 */
public final class ScoreLevelGenerator {

    /** Level id used for the generated score level (outside normal chapters). */
    public static final int SCORE_LEVEL_ID = 9001;

    /** Number of waves in every daily score level. */
    public static final int WAVE_COUNT = 5;

    /** Cheap filler zombie that anchors every wave's pool. */
    private static final String FILLER_ZOMBIE = "ZombieDefault";

    /** Definitions that exist in zombies.json but must never be wave-spawned. */
    private static final Set<String> EXCLUDED_NAMES = Set.of("ZombieIZombieSun");

    /** Minigame-only definitions excluded by prefix (Zombotany plant-heads). */
    private static final String EXCLUDED_PREFIX = "Zombotany";

    /** Zombies at or above this base HP are treated as rare heavies. */
    private static final int HEAVY_HP_THRESHOLD = 1000;

    private static final String ZOMBIES_JSON = "/assets/data/zombies/zombies.json";
    private static final String ARMOR_JSON = "/assets/data/armor/ArmorTypeData.json";

    private ScoreLevelGenerator() {}

    /** Builds today's score level (identical for every player on the same date). */
    public static ScoreLevel createDailyLevel() throws IOException {
        return createLevel(LocalDate.now().toEpochDay());
    }

    /** Builds the score level for an explicit seed (used by the daily variant). */
    public static ScoreLevel createLevel(long seed) throws IOException {
        List<Zombie> roster = spawnableRoster();
        LevelConfig config = LevelRegistry.toConfig(buildEntry(new Random(seed), roster));
        return new ScoreLevel(config);
    }

    /**
     * Every wave-spawnable zombie definition, sorted deterministically from
     * easiest to toughest.
     */
    private static List<Zombie> spawnableRoster() throws IOException {
        ensureDefinitionsLoaded();

        List<Zombie> roster = new ArrayList<>();
        for (Zombie zombie : ZombieFactory.getAllDefinitions()) {
            String name = zombie.getName();
            if (name == null || name.startsWith(EXCLUDED_PREFIX)) continue;
            if (EXCLUDED_NAMES.contains(name)) continue;
            // Stationary or zero-weight definitions are not wave material.
            if (zombie.getSpeed() <= 0f || zombie.getWeight() <= 0) continue;
            roster.add(zombie);
        }
        if (roster.isEmpty()) {
            throw new IOException("No spawnable zombie definitions found in " + ZOMBIES_JSON);
        }
        roster.sort(Comparator.comparingInt(Zombie::getWavePointCost)
                .thenComparingInt(Zombie::getBaseHP)
                .thenComparing(Zombie::getName));
        return roster;
    }

    /** Loads the zombie definitions if no one has initialised the factory yet. */
    private static void ensureDefinitionsLoaded() throws IOException {
        try {
            ZombieFactory.getAllDefinitions();
        } catch (RuntimeException notLoadedYet) {
            ZombieFactory.init(ZOMBIES_JSON, ARMOR_JSON);
        }
    }

    private static LevelDataEntry buildEntry(Random random, List<Zombie> roster) {
        LevelDataEntry entry = new LevelDataEntry();
        entry.setChapter("ANCIENT_EGYPT");
        entry.setLevelId(SCORE_LEVEL_ID);
        entry.setLevelType("NORMAL");
        entry.setRows(5);
        entry.setColumns(9);

        List<LevelDataEntry.WaveData> waves = new ArrayList<>();
        for (int waveNumber = 1; waveNumber <= WAVE_COUNT; waveNumber++) {
            waves.add(buildWave(random, waveNumber, roster));
        }
        entry.setWaves(waves);
        return entry;
    }

    private static LevelDataEntry.WaveData buildWave(Random random, int waveNumber, List<Zombie> roster) {
        LevelDataEntry.WaveData wave = new LevelDataEntry.WaveData();
        wave.setWaveNumber(waveNumber);
        wave.setStartDelay(waveNumber == 1 ? 10f : 12f);
        wave.setHugeWave(waveNumber == WAVE_COUNT);
        wave.setFinalWave(waveNumber == WAVE_COUNT);

        // Difficulty ramp: wave n may draw from the easiest n/WAVE_COUNT
        // portion of the roster, so the toughest zombies (Gargantuar class)
        // only become reachable in the last waves.
        int reachable = Math.max(3, roster.size() * waveNumber / WAVE_COUNT);
        reachable = Math.min(reachable, roster.size());

        List<Zombie> candidates = new ArrayList<>(roster.subList(0, reachable));
        Collections.shuffle(candidates, random);

        List<LevelDataEntry.ZombieCandidateData> pool = new ArrayList<>();
        pool.add(candidate(FILLER_ZOMBIE, 3.0));
        int picks = Math.min(2 + waveNumber, candidates.size());
        for (int i = 0; i < picks; i++) {
            Zombie zombie = candidates.get(i);
            if (FILLER_ZOMBIE.equals(zombie.getName())) continue; // already the filler
            // Heavies stay rare spice; regular picks grow more common later.
            double weight = zombie.getBaseHP() >= HEAVY_HP_THRESHOLD
                    ? 0.5 + waveNumber * 0.1
                    : 1.0 + waveNumber * 0.25;
            pool.add(candidate(zombie.getName(), weight));
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
