package model.data.minigame;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.data.level.LevelRegistry;
import model.enums.MiniGameType;
import model.game.level.LevelConfig;
import model.game.level.minigame.MiniGameFactory;
import model.game.level.minigame.MiniGameLevel;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.game.level.minigame.vasebreaker.VaseBreakerSettings;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads mini-game stage definitions from JSON and builds MiniGameLevel
 * instances. Config building (rules, waves, ...) is delegated to
 * LevelRegistry#toConfig so mini-games reuse the exact same pipeline as
 * normal levels.
 */
public final class MiniGameRegistry {

    private static MiniGameRegistry instance;

    private final Map<String, MiniGameDataEntry> entriesByKey;

    private MiniGameRegistry(List<MiniGameDataEntry> entries) {
        Map<String, MiniGameDataEntry> byKey = new HashMap<>();
        for (MiniGameDataEntry entry : entries) {
            if (entry == null || entry.getMiniGameType() == null) {
                continue;
            }
            byKey.put(key(entry.getMiniGameType(), entry.getStage()), entry);
        }
        this.entriesByKey = Collections.unmodifiableMap(byKey);
    }

    public static void init(String classpathPath) throws IOException {
        instance = load(classpathPath);
    }

    public static MiniGameRegistry getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MiniGameRegistry is not initialized.");
        }
        return instance;
    }

    public static MiniGameRegistry load(String classpathPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = openStream(classpathPath)) {
            List<MiniGameDataEntry> entries =
                    mapper.readValue(inputStream, new TypeReference<List<MiniGameDataEntry>>() {});
            return new MiniGameRegistry(entries);
        }
    }

    /**
     * Builds the mini-game level for the given type and stage.
     *
     * @return the built level, or null when no entry matches.
     */
    public MiniGameLevel createMiniGame(MiniGameType type, int stage) throws IOException {
        if (type == null) {
            return null;
        }
        MiniGameDataEntry entry = entriesByKey.get(key(type.name(), stage));
        if (entry == null) {
            return null;
        }
        LevelConfig config = LevelRegistry.toConfig(entry);
        MiniGameLevel level = MiniGameFactory.create(type, config, entry.getDifficultyTier());
        if (level != null) {
            level.setCoinReward(entry.getCoinReward());
        }
        if (level instanceof VaseBreakerLevel vaseBreaker) {
            vaseBreaker.setSettings(buildVaseSettings(entry));
        }
        return level;
    }

    /** Copies the vase-specific JSON keys into the level's settings. */
    private static VaseBreakerSettings buildVaseSettings(MiniGameDataEntry entry) {
        VaseBreakerSettings settings = new VaseBreakerSettings();
        settings.setVaseColumns(entry.getVaseColumns());
        settings.setRandomVaseCount(entry.getRandomVaseCount());
        settings.setSeedVaseCount(entry.getSeedVaseCount());
        settings.setGiantVaseCount(entry.getGiantVaseCount());
        settings.setRandomEmptyWeight(entry.getRandomEmptyWeight());
        settings.setRandomZombieWeight(entry.getRandomZombieWeight());
        settings.setRandomSeedWeight(entry.getRandomSeedWeight());
        settings.setSeedPacketExpirySeconds(entry.getSeedPacketExpirySeconds());
        settings.setGiantVaseZombie(entry.getGiantVaseZombie());
        settings.setZombiePool(entry.getVaseZombies());
        settings.setPlantPool(entry.getVasePlants());
        return settings;
    }

    public boolean hasStage(MiniGameType type, int stage) {
        return type != null && entriesByKey.containsKey(key(type.name(), stage));
    }

    private static String key(String type, int stage) {
        return type.trim().toUpperCase(Locale.ROOT) + ":" + stage;
    }

    private static InputStream openStream(String path) throws IOException {
        InputStream inputStream = MiniGameRegistry.class.getResourceAsStream(path);
        if (inputStream != null) return inputStream;

        String filePath = path.startsWith("/") ? path.substring(1) : path;
        return new FileInputStream(filePath);
    }
}
