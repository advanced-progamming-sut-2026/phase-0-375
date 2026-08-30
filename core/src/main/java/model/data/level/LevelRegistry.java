package model.data.level;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.enums.*;
import model.game.level.*;
import model.game.level.special.*;
import model.game.map.Point;
import model.game.rule.GameRules;
import model.game.wave.*;
import model.game.wave.spawn.*;
import model.zombie.ZombieFactory;
import model.zombie.definition.Zombie;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Loads level definitions from JSON resources/files and exposes them as ready-to-use
 * {@link Level} domain models.
 *
 * <p>The registry keeps the raw JSON DTOs and rebuilds a fresh {@link LevelConfig}
 * every time a level is requested. This is intentional: {@link Wave} and
 * {@link EntryRuntime} contain runtime state, so reusing a parsed config between
 * play sessions would leak wave progress from one game into the next.</p>
 */
public class LevelRegistry {
    private static LevelRegistry instance;

    private final Map<String, LevelDataEntry> entriesByKey;
    private final Map<Chapter, List<LevelDataEntry>> entriesByChapter;

    private LevelRegistry(List<LevelDataEntry> entries) {
        Map<String, LevelDataEntry> byKey = new HashMap<>();
        Map<Chapter, List<LevelDataEntry>> byChapter = new EnumMap<>(Chapter.class);
        for (LevelDataEntry entry : entries) {
            Chapter chapter = resolveEnum(Chapter.class, entry.getChapter(), null);
            if (chapter == null) {
                System.err.println("[LevelRegistry] Skipping level " + entry.getLevelId()
                        + " with unknown chapter: " + entry.getChapter());
                continue;
            }
            if (entry.getLevelId() <= 0) {
                System.err.println("[LevelRegistry] Skipping level with invalid id " + entry.getLevelId()
                        + " in chapter " + chapter);
                continue;
            }
            String key = key(chapter, entry.getLevelId());
            if (byKey.containsKey(key)) {
                System.err.println("[LevelRegistry] Skipping duplicate level definition: " + key);
                continue;
            }
            if (entry.getWaves() == null || entry.getWaves().isEmpty()) {
                System.err.println("[LevelRegistry] Warning: level " + key + " has no waves defined.");
            }
            byKey.put(key, entry);
            byChapter.computeIfAbsent(chapter, ignored -> new ArrayList<>()).add(entry);
        }
        for (List<LevelDataEntry> chapterEntries : byChapter.values()) {
            chapterEntries.sort(Comparator.comparingInt(LevelDataEntry::getLevelId));
        }
        this.entriesByKey = Collections.unmodifiableMap(byKey);
        Map<Chapter, List<LevelDataEntry>> frozen = new EnumMap<>(Chapter.class);
        for (Map.Entry<Chapter, List<LevelDataEntry>> entry : byChapter.entrySet()) {
            frozen.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        this.entriesByChapter = Collections.unmodifiableMap(frozen);
    }

    public static void init(String classpathPath) throws IOException {
        instance = load(classpathPath);
    }

    public static LevelRegistry getInstance() {
        if (instance == null) {
            throw new IllegalStateException("LevelRegistry is not initialized.");
        }
        return instance;
    }

    public static LevelRegistry load(String classpathPath) throws IOException {
        ensureZombieDefinitionsLoaded();
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = openLevelStream(classpathPath)) {
            List<LevelDataEntry> entries = mapper.readValue(inputStream, new TypeReference<List<LevelDataEntry>>() {});
            return new LevelRegistry(entries);
        }
    }

    public LevelConfig getConfig(Chapter chapter, int levelId) {
        LevelDataEntry entry = entriesByKey.get(key(chapter, levelId));
        return entry == null ? null : buildConfig(entry);
    }

    public Level createLevel(Chapter chapter, int levelId) {
        LevelConfig config = getConfig(chapter, levelId);
        if (config == null) return null;
        return LevelFactory.create(config);
    }

    public List<LevelConfig> getConfigsForChapter(Chapter chapter) {
        List<LevelDataEntry> entries = entriesByChapter.getOrDefault(chapter, Collections.emptyList());
        List<LevelConfig> configs = new ArrayList<>(entries.size());
        for (LevelDataEntry entry : entries) configs.add(buildConfig(entry));
        return Collections.unmodifiableList(configs);
    }

    public List<LevelConfig> getAllConfigs() {
        List<LevelConfig> configs = new ArrayList<>(entriesByKey.size());
        for (Chapter chapter : Chapter.values()) {
            for (LevelDataEntry entry : entriesByChapter.getOrDefault(chapter, Collections.emptyList())) {
                configs.add(buildConfig(entry));
            }
        }
        return Collections.unmodifiableList(configs);
    }

    public boolean hasLevel(Chapter chapter, int levelId) {
        return entriesByKey.containsKey(key(chapter, levelId));
    }

    /** Default classpath locations used to bootstrap zombie definitions on demand. */
    private static final String DEFAULT_ZOMBIES_JSON = "/assets/data/zombies/zombies.json";
    private static final String DEFAULT_ARMOR_JSON = "/assets/data/armor/ArmorTypeData.json";

    /**
     * Wave entries reference zombie definitions by name, so the zombie
     * registry must be initialized before level configs can be built.
     * Without this bootstrap, {@link #buildWaveEntry} would fail on the
     * first {@link ZombieFactory#getDefinition} call.
     */
    private static void ensureZombieDefinitionsLoaded() throws IOException {
        ZombieFactory.init(DEFAULT_ZOMBIES_JSON, DEFAULT_ARMOR_JSON);
    }

    private static InputStream openLevelStream(String path) throws IOException {
        InputStream inputStream = LevelRegistry.class.getResourceAsStream(path);
        if (inputStream != null) return inputStream;

        String filePath = path.startsWith("/") ? path.substring(1) : path;
        return new FileInputStream(filePath);
    }

    /**
     * Builds a LevelConfig from a raw data entry. Exposed so the mini-game
     * registry can reuse the exact same rule/wave building pipeline as
     * normal levels (including the zombie definition bootstrap).
     */
    public static LevelConfig toConfig(LevelDataEntry entry) throws IOException {
        ensureZombieDefinitionsLoaded();
        return buildConfig(entry);
    }

    private static LevelConfig buildConfig(LevelDataEntry entry) {
        LevelConfig config = new LevelConfig();
        config.setChapter(resolveEnum(Chapter.class, entry.getChapter(), Chapter.ANCIENT_EGYPT));
        config.setLevelId(entry.getLevelId());
        config.setRows(entry.getRows());
        config.setColumns(entry.getColumns());
        LevelType levelType = resolveEnum(LevelType.class, entry.getLevelType(), LevelType.NORMAL);
        config.setLevelType(levelType);
        config.setRules(buildRules(entry.getRules(), entry, levelType));
        config.setWaves(buildWaves(entry.getWaves()));
        config.setInitialGraves(points(entry.getInitialGraves()));
        config.setInitialIceBlocks(points(entry.getInitialIceBlocks()));
        config.setSlideTiles(slideTiles(entry.getSlideTiles()));
        config.setNecromancyTiles(points(entry.getNecromancyTiles()));
        List<ProtectedPlantTile> protectedPlants = protectedPlants(entry);
        config.setProtectedPlants(protectedPlants);
        config.setProtectedPlantPositions(protectedPlants.stream().map(ProtectedPlantTile::getPosition).toList());
        config.setProtectedPlantName(entry.getProtectedPlantName());
        config.setForcedPlants(entry.getForcedPlants() == null
                ? Collections.emptyList()
                : List.copyOf(entry.getForcedPlants()));
        config.setAllFamiliesRestricted(entry.isAllFamiliesRestricted());
        config.setRestrictedFamilies(restrictedFamilies(entry));
        config.setConveyorPlants(entry.getConveyorPlants() == null
                ? Collections.emptyList()
                : List.copyOf(entry.getConveyorPlants()));
        config.setConveyorIntervalSeconds(entry.getConveyorIntervalSeconds());
        config.setConveyorCapacity(entry.getConveyorCapacity());
        config.setWaterTiles(points(entry.getWaterTiles()));
        // Prefer wave-authored low-tide cells; fall back to legacy level field.
        List<Point> levelLowTide = points(entry.getLowTideTiles());
        if ((levelLowTide == null || levelLowTide.isEmpty()) && entry.getWaves() != null) {
            for (LevelDataEntry.WaveData w : entry.getWaves()) {
                List<Point> fromWave = points(w.getLowTideTiles());
                if (fromWave != null && !fromWave.isEmpty()) {
                    levelLowTide = fromWave;
                    break;
                }
            }
        }
        config.setLowTideTiles(levelLowTide);
        config.setTideLimitColumn(entry.getTideLimitColumn());
        config.setDeadLineColumn(entry.getDeadLineColumn());
        config.setHasNightEffect(entry.isHasNightEffect());
        config.setZombossDefinition(entry.getZomboss());
        return config;
    }

    private static GameRules buildRules(LevelDataEntry.RuleData data, LevelDataEntry entry, LevelType levelType) {
        if (data == null) data = new LevelDataEntry.RuleData();
        Set<PlantCategory> allowedCategories = new HashSet<>();
        if (data.getAllowedPlantCategories() != null) {
            for (String raw : data.getAllowedPlantCategories()) {
                allowedCategories.add(resolveEnum(PlantCategory.class, raw, null));
            }
            allowedCategories.remove(null);
        }
        GameRules rules = new GameRules(
                data.isSkyDropEnabled(), data.isZombiesFreezable(), data.getInitialSun(),
                data.getSunDropRateModifier(), data.getMinPlantsRequired(), data.getMaxPlantsAllowed(),
                allowedCategories, intSet(data.getForbiddenColumns()), intSet(data.getForbiddenRows())
        );
        rules.setSunFallsFromSky(data.isSunFallsFromSky());
        rules.setSunProducingPlantsAllowed(data.isSunProducingPlantsAllowed());
        rules.setPlantFoodDrops(data.isPlantFoodDrops());
        rules.setLawnMowersEnabled(data.isLawnMowersEnabled());
        rules.setShovelEnabled(data.isShovelEnabled());
        rules.setAllowsChoosingPlants(data.isAllowsChoosingPlants());
        rules.setPlantRechargeInSetup(data.isPlantRechargeInSetup());
        rules.setDeadLineColumn(entry.getDeadLineColumn() >= 0 ? entry.getDeadLineColumn() : data.getDeadLineColumn());
        rules.setMaxPlantDeaths(data.getMaxPlantDeaths());
        rules.setTimedWarLimit(data.getTimedWarLimit());
        rules.setTimedWarTargetKills(data.getTimedWarTargetKills());
        rules.setTimedWarDecayInterval(data.getTimedWarDecayInterval());
        applyLevelTypeDefaults(rules, levelType, entry);
        return rules;
    }

    private static void applyLevelTypeDefaults(GameRules rules, LevelType type, LevelDataEntry entry) {
        switch (type) {
            case CONVEYOR_BELT:
            case ZOMBOSS:
                rules.setAllowsChoosingPlants(false); break;
            case LOCKED_PLANTS: {
                // Family-pick variant still lets the player choose (constrained per
                // family); the forced-set variant locks the whole selection.
                boolean familyPickVariant = entry != null
                        && (entry.isAllFamiliesRestricted()
                                || (entry.getRestrictedFamilies() != null && !entry.getRestrictedFamilies().isEmpty()));
                rules.setAllowsChoosingPlants(familyPickVariant);
                break;
            }
            case NIGHT_OPS:
                rules.setSunFallsFromSky(false); break;
            case PLANT_WHAT_YOU_GET:
                rules.setSunFallsFromSky(false);
                rules.setSunProducingPlantsAllowed(false);
                rules.setPlantRechargeInSetup(false);
                break;
            default: break;
        }
    }

    private static List<Wave> buildWaves(List<LevelDataEntry.WaveData> waveData) {
        if (waveData == null) return Collections.emptyList();
        List<Wave> waves = new ArrayList<>();
        int waveIndex = 0;
        for (LevelDataEntry.WaveData rawWave : waveData) {
            waveIndex++;
            waves.add(buildWave(rawWave, waveIndex));
        }
        return Collections.unmodifiableList(waves);
    }

    /** Builds a single Wave: resolves its budget, its entries, and their spawn strategies. */
    private static Wave buildWave(LevelDataEntry.WaveData rawWave, int waveIndex) {
        int waveNumber = rawWave.getWaveNumber() > 0 ? rawWave.getWaveNumber() : waveIndex;
        boolean isFinal = rawWave.isFinalWave();

        // Phase-2 budget-based wave: explicit waveBudget OR fallback when
        // the wave has candidate pools but no scripted entries.
        int budget = rawWave.getWaveBudget();
        if (budget <= 0) {
            budget = computeDefaultWaveBudget(waveNumber, isFinal, rawWave.getEntries());
        }

        List<EntryRuntime> runtimes = new ArrayList<>();
        List<SpawnPatternType> patternTypes = new ArrayList<>();
        buildWaveRuntimes(rawWave, budget, runtimes, patternTypes);

        Wave wave = new Wave(
                waveNumber, runtimes, rawWave.getStartDelay(),
                rawWave.isHugeWave(), isFinal
        );
        wave.setWaveBudget(budget);
        wave.setLowTideTiles(points(rawWave.getLowTideTiles()));
        assignSpawnStrategies(wave, runtimes, patternTypes, budget);
        return wave;
    }

    /**
     * Populates {@code runtimes}/{@code patternTypes} for one wave: either a single
     * pooled entry driven by the budget, or the legacy scripted-entry list.
     */
    private static void buildWaveRuntimes(LevelDataEntry.WaveData rawWave, int budget,
                                          List<EntryRuntime> runtimes, List<SpawnPatternType> patternTypes) {
        if (budget > 0) {
            // Build a single pooled entry + BudgetSpawnStrategy.
            List<LevelDataEntry.ZombieCandidateData> flatPool = new ArrayList<>();
            if (rawWave.getEntries() != null) {
                for (LevelDataEntry.WaveEntryData e : rawWave.getEntries()) {
                    if (e.getPool() != null) flatPool.addAll(e.getPool());
                }
            }
            WaveZombieEntry pooledEntry = buildBudgetWaveEntry(flatPool);
            if (pooledEntry != null) {
                runtimes.add(new EntryRuntime(pooledEntry));
                patternTypes.add(SpawnPatternType.STREAM);
            }
        } else if (rawWave.getEntries() != null) {
            // Legacy scripted-entry model.
            for (LevelDataEntry.WaveEntryData rawEntry : rawWave.getEntries()) {
                SpawnPatternType patternType = resolveEnum(
                        SpawnPatternType.class, rawEntry.getPattern(), SpawnPatternType.SINGLE
                );
                WaveZombieEntry entry = buildWaveEntry(rawEntry, patternType);
                if (entry != null) {
                    runtimes.add(new EntryRuntime(entry));
                    patternTypes.add(patternType);
                }
            }
        }
    }

    /** Attaches the appropriate SpawnStrategy to each entry runtime for the wave. */
    private static void assignSpawnStrategies(Wave wave, List<EntryRuntime> runtimes,
                                              List<SpawnPatternType> patternTypes, int budget) {
        for (int i = 0; i < runtimes.size(); i++) {
            EntryRuntime runtime = runtimes.get(i);
            SpawnStrategy strategy = budget > 0
                    ? new BudgetSpawnStrategy(runtime, wave, budget)
                    : spawnStrategy(patternTypes.get(i), runtime, wave);
            runtime.getWaveZombieEntry().setPattern(strategy);
        }
    }

    /**
     * Default wave budget: base × 1.25^(waveNumber-1), final wave ×2.
     * Only kicks in when the wave has at least one pool candidate; otherwise
     * returns 0 (falls back to scripted entries).
     */
    private static int computeDefaultWaveBudget(int waveNumber, boolean isFinal,
                                                 List<LevelDataEntry.WaveEntryData> entries) {
        if (entries == null || entries.isEmpty()) return 0;
        boolean hasPool = false;
        for (LevelDataEntry.WaveEntryData e : entries) {
            if (e.getPool() != null && !e.getPool().isEmpty()) { hasPool = true; break; }
        }
        if (!hasPool) return 0;

        final int BASE = 100;
        double scaled = BASE * Math.pow(1.25, Math.max(0, waveNumber - 1));
        if (isFinal) scaled *= 2.0;
        return (int) Math.round(scaled);
    }

    /** Builds a single WaveZombieEntry whose pool is the union of all candidates. */
    private static WaveZombieEntry buildBudgetWaveEntry(List<LevelDataEntry.ZombieCandidateData> flatPool) {
        if (flatPool == null || flatPool.isEmpty()) return null;
        WaveZombieEntry.Builder builder = new WaveZombieEntry.Builder()
                .setCountRange(1, 1)
                .setSpawnDelayRange(0f, 0f)
                .setSpawnIntervalRange(1.5f, 1.5f)
                .setStreamDurationSeconds(60f);
        for (LevelDataEntry.ZombieCandidateData candidate : flatPool) {
            Zombie zombie = ZombieFactory.getDefinition(candidate.getZombie());
            if (zombie == null) {
                System.err.println("[LevelRegistry] Unknown zombie in wave pool: " + candidate.getZombie());
                continue;
            }
            builder.addCandidate(zombie, 1.0); // weight ignored by BudgetSpawnStrategy
        }
        EntryRuntime placeholderRuntime = new EntryRuntime(new WaveZombieEntry());
        Wave placeholderWave = new Wave(1, Collections.emptyList(), 0f, false, false);
        builder.setPattern(new SingleSpawnStrategy(placeholderRuntime, placeholderWave));
        try {
            return builder.build();
        } catch (RuntimeException e) {
            System.err.println("[LevelRegistry] Skipping invalid budget wave entry: " + e.getMessage());
            return null;
        }
    }

    private static WaveZombieEntry buildWaveEntry(LevelDataEntry.WaveEntryData raw, SpawnPatternType patternType) {
        WaveZombieEntry.Builder builder = new WaveZombieEntry.Builder()
                .setCountRange(raw.getMinCount(), raw.getMaxCount())
                .setSpawnDelayRange(raw.getMinSpawnDelay(), raw.getMaxSpawnDelay())
                .setSpawnIntervalRange(raw.getMinSpawnInterval(), raw.getMaxSpawnInterval())
                .setStreamDurationSeconds(raw.getStreamDurationSeconds());
        if (raw.getAllowedLanes() != null) builder.setAllowedLanes(raw.getAllowedLanes());
        if (raw.getPool() != null) {
            for (LevelDataEntry.ZombieCandidateData candidate : raw.getPool()) {
                Zombie zombie = ZombieFactory.getDefinition(candidate.getZombie());
                if (zombie == null) {
                    System.err.println("[LevelRegistry] Unknown zombie in wave pool: " + candidate.getZombie());
                    continue;
                }
                builder.addCandidate(zombie, candidate.getWeight());
            }
        }
        EntryRuntime placeholderRuntime = new EntryRuntime(new WaveZombieEntry());
        Wave placeholderWave = new Wave(1, Collections.emptyList(), 0f, false, false);
        builder.setPattern(spawnStrategy(patternType, placeholderRuntime, placeholderWave));
        try {
            return builder.build();
        } catch (RuntimeException e) {
            System.err.println("[LevelRegistry] Skipping invalid wave entry: " + e.getMessage());
            return null;
        }
    }

    private static SpawnStrategy spawnStrategy(SpawnPatternType type, EntryRuntime runtime, Wave wave) {
        switch (type) {
            case GROUP: return new GroupSpawnStrategy(runtime, wave);
            case STREAM: return new StreamSpawnStrategy(runtime, wave);
            case AMBUSH: return new AmbushSpawnStrategy(runtime, wave);
            case SINGLE:
            default: return new SingleSpawnStrategy(runtime, wave);
        }
    }

    private static List<Point> points(List<? extends LevelDataEntry.PointData> rawPoints) {
        if (rawPoints == null) return Collections.emptyList();
        List<Point> result = new ArrayList<>();
        for (LevelDataEntry.PointData point : rawPoints) result.add(new Point(point.getX(), point.getY()));
        return Collections.unmodifiableList(result);
    }

    /**
     * Normalises the two JSON shapes for Save Our Seeds tiles into one list:
     * {@code protectedPlants} ({x, y, plant}) wins when present; otherwise the
     * legacy {@code protectedPlantPositions} points are used with no per-tile
     * plant (the level falls back to its default plant).
     */
    private static List<ProtectedPlantTile> protectedPlants(LevelDataEntry entry) {
        List<ProtectedPlantTile> result = new ArrayList<>();
        if (entry.getProtectedPlants() != null) {
            for (LevelDataEntry.ProtectedPlantData data : entry.getProtectedPlants()) {
                result.add(new ProtectedPlantTile(new Point(data.getX(), data.getY()), data.getPlant()));
            }
        } else if (entry.getProtectedPlantPositions() != null) {
            for (LevelDataEntry.PointData point : entry.getProtectedPlantPositions()) {
                result.add(new ProtectedPlantTile(new Point(point.getX(), point.getY()), null));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Locked Plants family-pick variant: the restricted families
     * ({@code PlantCategory} names, stored uppercase) from which the player
     * may select only one plant.
     */
    private static Set<String> restrictedFamilies(LevelDataEntry entry) {
        if (entry.getRestrictedFamilies() == null) return Collections.emptySet();
        Set<String> result = new LinkedHashSet<>();
        for (String family : entry.getRestrictedFamilies()) {
            if (family != null) result.add(family.trim().toUpperCase());
        }
        return Collections.unmodifiableSet(result);
    }

    private static Map<Point, SlideDirection> slideTiles(
            List<LevelDataEntry.SlideTileData> rawTiles
    ) {
        if (rawTiles == null) return Collections.emptyMap();
        Map<Point, SlideDirection> result = new HashMap<>();
        for (LevelDataEntry.SlideTileData tile : rawTiles) {
            result.put(new Point(
                    tile.getX(), tile.getY()), resolveEnum(SlideDirection.class, tile.getDirection(), SlideDirection.UP)
            );
        }
        return Collections.unmodifiableMap(result);
    }

    private static Set<Integer> intSet(List<Integer> values) {
        return values == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(values));
    }

    private static <T extends Enum<T>> T resolveEnum(Class<T> enumType, String raw, T fallback) {
        if (raw == null) return fallback;
        try {
            return Enum.valueOf(enumType, raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException e) {
            System.err.println("[LevelRegistry] Unknown " + enumType.getSimpleName() + ": " + raw);
            return fallback;
        }
    }

    private static String key(Chapter chapter, int levelId) {
        return chapter.name() + "#" + levelId;
    }
}
