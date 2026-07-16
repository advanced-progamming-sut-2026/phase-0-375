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
        config.setDeadLineColumn(entry.getDeadLineColumn());
        config.setHasNightEffect(entry.isHasNightEffect());
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
        applyLevelTypeDefaults(rules, levelType, entry);
        return rules;
    }

    private static void applyLevelTypeDefaults(GameRules rules, LevelType type, LevelDataEntry entry) {
        switch (type) {
            case CONVEYOR_BELT:
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
        for (LevelDataEntry.WaveData rawWave : waveData) {
            List<EntryRuntime> runtimes = new ArrayList<>();
            List<SpawnPatternType> patternTypes = new ArrayList<>();
            if (rawWave.getEntries() != null) {
                for (LevelDataEntry.WaveEntryData rawEntry : rawWave.getEntries()) {
                    SpawnPatternType patternType = resolveEnum(SpawnPatternType.class, rawEntry.getPattern(), SpawnPatternType.SINGLE);
                    WaveZombieEntry entry = buildWaveEntry(rawEntry, patternType);
                    if (entry != null) {
                        runtimes.add(new EntryRuntime(entry));
                        patternTypes.add(patternType);
                    }
                }
            }
            Wave wave = new Wave(rawWave.getWaveNumber(), runtimes, rawWave.getStartDelay(), rawWave.isHugeWave(), rawWave.isFinalWave());
            for (int i = 0; i < runtimes.size(); i++) {
                EntryRuntime runtime = runtimes.get(i);
                runtime.getWaveZombieEntry().setPattern(spawnStrategy(patternTypes.get(i), runtime, wave));
            }
            waves.add(wave);
        }
        return Collections.unmodifiableList(waves);
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

    private static Map<Point, SlideDirection> slideTiles(List<LevelDataEntry.SlideTileData> rawTiles) {
        if (rawTiles == null) return Collections.emptyMap();
        Map<Point, SlideDirection> result = new HashMap<>();
        for (LevelDataEntry.SlideTileData tile : rawTiles) {
            result.put(new Point(tile.getX(), tile.getY()), resolveEnum(SlideDirection.class, tile.getDirection(), SlideDirection.UP));
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
