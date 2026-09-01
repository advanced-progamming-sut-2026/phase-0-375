package com.sut.server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import model.enums.Chapter;
import model.news.NewsFactory;
import model.plant.PlantFactory;
import model.shop.Shop;
import model.user.User;
import model.user.persistance.UserRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Thread-safe, crash-resilient server-side User repository implementing UserRepository.
 * Uses ReentrantReadWriteLock, dual in-memory indexing, and atomic temporary file staging.
 */
public class ServerUserRepository implements UserRepository {

    public static final String DEFAULT_FILE_PATH = "server/data/users.json";
    private static final String PLANTS_JSON = "/assets/data/plants/plants.json";

    private final Path storagePath;
    private final ObjectMapper mapper;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(false); // non-fair lock for high throughput
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();
    private final List<User> users = new ArrayList<>();

    public ServerUserRepository() {
        this(resolveStoragePath());
    }

    public ServerUserRepository(Path storagePath) {
        this(storagePath, createDefaultMapper());
    }

    public ServerUserRepository(Path storagePath, ObjectMapper mapper) {
        this.storagePath = Objects.requireNonNull(storagePath, "storagePath cannot be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
        loadAll();
    }

    public static Path resolveStoragePath() {
        String sysProp = System.getProperty("pvz.users.file");
        if (sysProp == null || sysProp.isBlank()) {
            sysProp = System.getProperty("users.file.path");
        }
        if (sysProp != null && !sysProp.isBlank()) {
            return Paths.get(sysProp.trim());
        }
        String env = System.getenv("PVZ_USERS_FILE");
        if (env != null && !env.isBlank()) {
            return Paths.get(env.trim());
        }
        // Running from the :server module cwd → data/users.json under server/
        // Running from repo root (gradle run) → server/data/users.json
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        String leaf = cwd.getFileName() != null ? cwd.getFileName().toString() : "";
        if ("server".equalsIgnoreCase(leaf)) {
            return Paths.get("data", "users.json");
        }
        return Paths.get("server", "data", "users.json");
    }

    public static ObjectMapper createDefaultMapper() {
        ObjectMapper m = new ObjectMapper();
        m.enable(SerializationFeature.INDENT_OUTPUT);
        return m;
    }

    public Path getStoragePath() {
        return storagePath;
    }

    // ==========================================
    // Lifecycle & Persistence (Atomic I/O)
    // ==========================================

    @Override
    public void loadAll() {
        rwLock.writeLock().lock();
        try {
            users.clear();
            usersByUsername.clear();
            usersByEmail.clear();

            File file = storagePath.toFile();
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            if (!file.exists() || file.length() == 0) {
                // Initialize clean empty store and persist empty JSON array
                flushInternal();
                return;
            }

            try {
                List<User> loaded = mapper.readValue(file, new TypeReference<ArrayList<User>>() {});
                if (loaded != null) {
                    for (User u : loaded) {
                        if (u != null && u.getUsername() != null) {
                            users.add(u);
                            usersByUsername.put(u.getUsername(), u);
                            if (u.getEmail() != null) {
                                usersByEmail.put(u.getEmail().toLowerCase(), u);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("[ServerUserRepository] Error parsing " + storagePath + ": " + e.getMessage());
                backupCorruptFile(file);
            }

            migrateStarterPlantsInternal();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void backupCorruptFile(File corruptFile) {
        try {
            File backup = new File(corruptFile.getParentFile(),
                    corruptFile.getName() + ".corrupt." + System.currentTimeMillis());
            Files.copy(corruptFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.err.println("[ServerUserRepository] Backed up corrupt file to: " + backup.getAbsolutePath());
        } catch (IOException ignored) {}
    }

    private void migrateStarterPlantsInternal() {
        boolean stampedDates = false;
        for (User u : users) {
            if (u.getUnlockedPlants() == null) {
                u.setUnlockedPlants(new HashSet<>());
            }
            u.getUnlockedPlants().addAll(User.STARTER_PLANTS);
            int before = u.getNewsPublishDates() == null ? 0 : u.getNewsPublishDates().size();
            for (String plant : User.STARTER_PLANTS) {
                u.rememberNewsPublishDate(NewsFactory.plantNewsId(plant));
            }
            int after = u.getNewsPublishDates() == null ? 0 : u.getNewsPublishDates().size();
            if (after > before) {
                stampedDates = true;
            }
        }
        if (stampedDates) {
            flushInternal();
        }
    }

    @Override
    public void flush() {
        rwLock.writeLock().lock();
        try {
            flushInternal();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void flushInternal() {
        File targetFile = storagePath.toFile();
        File parentDir = targetFile.getParentFile();
        if (parentDir != null) {
            try {
                Files.createDirectories(parentDir.toPath());
            } catch (IOException ignored) {
                parentDir.mkdirs();
            }
        }

        File tempFile = new File(parentDir != null ? parentDir : new File("."),
                targetFile.getName() + ".tmp." + UUID.randomUUID());
        try {
            try (java.io.OutputStream os = new java.io.BufferedOutputStream(new java.io.FileOutputStream(tempFile), 65536)) {
                mapper.writeValue(os, users);
            }
            try {
                Files.move(tempFile.toPath(), targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile.toPath(), targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[ServerUserRepository] Failed to flush users to " + storagePath + ": " + e.getMessage());
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    // ==========================================
    // CRUD & Queries (Thread-Safe)
    // ==========================================

    @Override
    public void save(User user) {
        if (user == null || user.getUsername() == null) return;
        rwLock.writeLock().lock();
        try {
            User existing = usersByUsername.get(user.getUsername());
            if (existing != null) {
                users.remove(existing);
                if (existing.getEmail() != null) {
                    usersByEmail.remove(existing.getEmail().toLowerCase());
                }
            }
            users.add(user);
            usersByUsername.put(user.getUsername(), user);
            if (user.getEmail() != null) {
                usersByEmail.put(user.getEmail().toLowerCase(), user);
            }
            flushInternal();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void delete(User user) {
        if (user == null || user.getUsername() == null) return;
        rwLock.writeLock().lock();
        try {
            User existing = usersByUsername.remove(user.getUsername());
            if (existing != null) {
                users.remove(existing);
                if (existing.getEmail() != null) {
                    usersByEmail.remove(existing.getEmail().toLowerCase());
                }
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        rwLock.readLock().lock();
        try {
            return Optional.ofNullable(usersByUsername.get(username));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        rwLock.readLock().lock();
        try {
            return Optional.ofNullable(usersByEmail.get(email.toLowerCase()));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        if (username == null) return false;
        rwLock.readLock().lock();
        try {
            return usersByUsername.containsKey(username);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null) return false;
        rwLock.readLock().lock();
        try {
            return usersByEmail.containsKey(email.toLowerCase());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Optional<User> authenticate(String username, String passwordHash) {
        if (username == null || passwordHash == null) return Optional.empty();
        rwLock.readLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null && passwordHash.equals(u.getPasswordHash())) {
                return Optional.of(u);
            }
            return Optional.empty();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Optional<User> findStayLoggedInUser() {
        rwLock.readLock().lock();
        try {
            return users.stream().filter(User::isStayLoggedIn).findFirst();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void clearStayLoggedIn(String username) {
        if (username == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                u.setStayLoggedIn(false);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean verifySecurityAnswer(String username, String answer) {
        if (username == null || answer == null) return false;
        rwLock.readLock().lock();
        try {
            User u = usersByUsername.get(username);
            return u != null && u.getSecurityAnswer() != null && u.getSecurityAnswer().equals(answer);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void updatePassword(String username, String newPasswordHash) {
        if (username == null || newPasswordHash == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                u.setPasswordHash(newPasswordHash);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void addCoins(String username, int amount) {
        if (username == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                u.setCoins(u.getCoins() + amount);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean spendCoins(String username, int amount) {
        if (username == null) return false;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null && u.getCoins() >= amount) {
                u.setCoins(u.getCoins() - amount);
                flushInternal();
                return true;
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void addGems(String username, int amount) {
        if (username == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                u.setGems(u.getGems() + amount);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean spendGems(String username, int amount) {
        if (username == null) return false;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null && u.getGems() >= amount) {
                u.setGems(u.getGems() - amount);
                flushInternal();
                return true;
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void unlockPlant(String username, String plantName) {
        if (username == null || plantName == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                if (u.getUnlockedPlants() == null) {
                    u.setUnlockedPlants(new HashSet<>());
                }
                if (u.getUnlockedPlants().add(plantName)) {
                    u.rememberNewsPublishDate(NewsFactory.plantNewsId(plantName));
                }
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void unlockZombie(String username, String zombieName) {
        if (username == null || zombieName == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                if (u.getUnlockedZombies() == null) {
                    u.setUnlockedZombies(new HashSet<>());
                }
                if (u.getUnlockedZombies().add(zombieName)) {
                    u.rememberNewsPublishDate(NewsFactory.zombieNewsId(zombieName));
                }
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void updateChapterProgress(String username, Chapter chapter, int level) {
        if (username == null || chapter == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                if (u.getChapterProgress() == null) {
                    u.setChapterProgress(new HashMap<>());
                }
                u.getChapterProgress().put(chapter, level);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void updateHighestMyopoint(String username, int myopoint) {
        if (username == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null && myopoint > u.getHighestMyopoint()) {
                u.setHighestMyopoint(myopoint);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void incrementGamesPlayed(String username) {
        if (username == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                u.setGamesPlayed(u.getGamesPlayed() + 1);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void unlockMiniGame(String username, String miniGameId) {
        if (username == null || miniGameId == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                if (u.getUnlockedMiniGames() == null) {
                    u.setUnlockedMiniGames(new HashSet<>());
                }
                if (u.getUnlockedMiniGames().add(miniGameId)) {
                    u.rememberNewsPublishDate(NewsFactory.miniGameNewsId(miniGameId));
                }
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void updateDifficulty(String username, int difficultyLevel) {
        if (username == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                u.setDifficultyLevel(difficultyLevel);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void addSeedPackets(String username, String plantName, int count) {
        if (username == null || plantName == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                if (u.getSeedPackets() == null) {
                    u.setSeedPackets(new HashMap<>());
                }
                u.getSeedPackets().put(plantName, u.getSeedPackets().getOrDefault(plantName, 0) + count);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean spendSeedPackets(String username, String plantName, int count) {
        if (username == null || plantName == null) return false;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null && u.getSeedPackets() != null) {
                int current = u.getSeedPackets().getOrDefault(plantName, 0);
                if (current >= count) {
                    u.getSeedPackets().put(plantName, current - count);
                    flushInternal();
                    return true;
                }
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean addPlantFood(String username) {
        if (username == null) return false;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u == null) {
                return false;
            }
            int current = Math.max(0, u.getPlantFoodCount());
            if (current >= Shop.MAX_PLANT_FOOD) {
                return false;
            }
            u.setPlantFoodCount(current + 1);
            flushInternal();
            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean usePlantFood(String username) {
        if (username == null) return false;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u == null) {
                return false;
            }
            int current = u.getPlantFoodCount();
            if (current <= 0) {
                if (current < 0) {
                    u.setPlantFoodCount(0);
                    flushInternal();
                }
                return false;
            }
            u.setPlantFoodCount(current - 1);
            flushInternal();
            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void storePlantBoost(String username, String plantName) {
        String canonical = resolvePlantName(plantName);
        if (canonical == null || username == null) {
            return;
        }
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                if (u.getPlantBoosts() == null) {
                    u.setPlantBoosts(new HashMap<>());
                }
                u.getPlantBoosts().put(canonical, true);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean consumePlantBoost(String username, String plantName) {
        String canonical = resolvePlantName(plantName);
        if (canonical == null || username == null) {
            return false;
        }
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null && u.getPlantBoosts() != null) {
                Boolean boost = u.getPlantBoosts().get(canonical);
                if (Boolean.TRUE.equals(boost)) {
                    u.getPlantBoosts().put(canonical, false);
                    flushInternal();
                    return true;
                }
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private String resolvePlantName(String plantName) {
        if (plantName == null || plantName.trim().isEmpty()) {
            return null;
        }
        String trimmed = plantName.trim();
        try {
            ensurePlantDefinitionsLoaded();
            if (PlantFactory.hasDefinition(trimmed)) {
                return trimmed;
            }
            for (var definition : PlantFactory.getAllDefinitions()) {
                if (definition.getName().equalsIgnoreCase(trimmed)) {
                    return definition.getName();
                }
            }
        } catch (IOException | RuntimeException e) {
            // Definitions unavailable on headless server or missing asset — fallback to trimmed name
            return trimmed;
        }
        return trimmed;
    }

    private void ensurePlantDefinitionsLoaded() throws IOException {
        try {
            PlantFactory.getAllDefinitions();
        } catch (RuntimeException notLoaded) {
            PlantFactory.init(PLANTS_JSON);
        }
    }

    @Override
    public void unlockGreenhousePot(String username, int x, int y) {
        if (username == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null && u.getUnlockedPots() < 20) {
                u.setUnlockedPots(u.getUnlockedPots() + 1);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void plantInGreenhouse(String username, int x, int y, String plantName, long timestamp) {
        if (username == null || plantName == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                String key = x + "," + y;
                if (u.getGreenhousePots() == null) {
                    u.setGreenhousePots(new HashMap<>());
                }
                if (u.getGreenhousePlantTimestamps() == null) {
                    u.setGreenhousePlantTimestamps(new HashMap<>());
                }
                u.getGreenhousePots().put(key, plantName);
                u.getGreenhousePlantTimestamps().put(key, timestamp);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void harvestGreenhousePlant(String username, int x, int y) {
        if (username == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                String key = x + "," + y;
                if (u.getGreenhousePots() != null) {
                    u.getGreenhousePots().remove(key);
                }
                if (u.getGreenhousePlantTimestamps() != null) {
                    u.getGreenhousePlantTimestamps().remove(key);
                }
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void markNewsAsRead(String username, String newsId) {
        if (username == null || newsId == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                if (u.getReadNews() == null) {
                    u.setReadNews(new ArrayList<>());
                }
                u.getReadNews().add(newsId);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void completeQuest(String username, String questId, boolean isDaily) {
        if (username == null || questId == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                if (u.getQuestStatus() == null) {
                    u.setQuestStatus(new HashMap<>());
                }
                u.getQuestStatus().put(questId, true);
                if (isDaily) {
                    u.setCompletedDailyQuests(u.getCompletedDailyQuests() + 1);
                } else {
                    u.setCompletedNonDailyQuests(u.getCompletedNonDailyQuests() + 1);
                }
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void purchaseDailyDeal(String username, String dealId) {
        if (username == null || dealId == null) return;
        rwLock.writeLock().lock();
        try {
            User u = usersByUsername.get(username);
            if (u != null) {
                if (u.getPurchasedDailyDeals() == null) {
                    u.setPurchasedDailyDeals(new HashMap<>());
                }
                u.getPurchasedDailyDeals().put(dealId, true);
                flushInternal();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean hasPurchasedDailyDeal(String username, String dealId) {
        if (username == null || dealId == null) return false;
        rwLock.readLock().lock();
        try {
            User u = usersByUsername.get(username);
            return u != null && u.getPurchasedDailyDeals() != null && u.getPurchasedDailyDeals().getOrDefault(dealId, false);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<User> findAll() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(users);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    // ==========================================
    // Leaderboards (Read Lock Sorted Projections)
    // ==========================================

    private User copyUser(User source) {
        if (source == null) return null;
        User u = new User();
        u.setUsername(source.getUsername());
        u.setNickname(source.getNickname());
        u.setEmail(source.getEmail());
        u.setGender(source.getGender());
        u.setPasswordHash(source.getPasswordHash());
        u.setCoins(source.getCoins());
        u.setGems(source.getGems());
        u.setHighestMyopoint(source.getHighestMyopoint());
        u.setGamesPlayed(source.getGamesPlayed());
        u.setCompletedDailyQuests(source.getCompletedDailyQuests());
        u.setCompletedNonDailyQuests(source.getCompletedNonDailyQuests());
        u.setCompletedMiniGames(source.getCompletedMiniGames());
        if (source.getChapterProgress() != null) {
            u.setChapterProgress(new HashMap<>(source.getChapterProgress()));
        }
        return u;
    }

    @Override
    public List<User> findAllOrderByMyopointDesc() {
        rwLock.readLock().lock();
        try {
            return users.stream()
                    .map(this::copyUser)
                    .sorted((a, b) -> Integer.compare(b.getHighestMyopoint(), a.getHighestMyopoint()))
                    .collect(Collectors.toList());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<User> findAllOrderByChapterProgressDesc() {
        rwLock.readLock().lock();
        try {
            return users.stream()
                    .map(this::copyUser)
                    .sorted((a, b) -> {
                        int aTotal = a.getChapterProgress() != null
                                ? a.getChapterProgress().values().stream().mapToInt(Integer::intValue).sum() : 0;
                        int bTotal = b.getChapterProgress() != null
                                ? b.getChapterProgress().values().stream().mapToInt(Integer::intValue).sum() : 0;
                        return Integer.compare(bTotal, aTotal);
                    })
                    .collect(Collectors.toList());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<User> findAllOrderByMiniGamesDesc() {
        rwLock.readLock().lock();
        try {
            return users.stream()
                    .map(this::copyUser)
                    .sorted((a, b) -> Integer.compare(b.getCompletedMiniGames(), a.getCompletedMiniGames()))
                    .collect(Collectors.toList());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<User> findAllOrderByCompletedQuestsDesc() {
        rwLock.readLock().lock();
        try {
            return users.stream()
                    .map(this::copyUser)
                    .sorted((a, b) -> Integer.compare(
                            b.getCompletedDailyQuests() + b.getCompletedNonDailyQuests(),
                            a.getCompletedDailyQuests() + a.getCompletedNonDailyQuests()))
                    .collect(Collectors.toList());
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
