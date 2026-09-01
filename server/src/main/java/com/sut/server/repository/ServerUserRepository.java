package com.sut.server.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import model.enums.Chapter;
import model.user.User;
import model.user.persistance.UserRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Thread-safe, crash-resilient server-side User repository implementing UserRepository.
 * Uses ReentrantReadWriteLock, dual in-memory indexing, and atomic temporary file staging.
 */
public class ServerUserRepository implements UserRepository {

    public static final String DEFAULT_FILE_PATH = "server/data/users.json";

    private final ServerUserFileStore files;
    private final ServerUserMutator mutator;

    public ServerUserRepository() {
        this(resolveStoragePath());
    }

    public ServerUserRepository(Path storagePath) {
        this(storagePath, createDefaultMapper());
    }

    public ServerUserRepository(Path storagePath, ObjectMapper mapper) {
        Path path = Objects.requireNonNull(storagePath, "storagePath cannot be null");
        ObjectMapper objectMapper = Objects.requireNonNull(mapper, "mapper cannot be null");
        ServerUserTables tables = new ServerUserTables();
        this.files = new ServerUserFileStore(path, objectMapper, tables);
        this.mutator = new ServerUserMutator(tables, files);
        files.loadAll();
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
        return files.getStoragePath();
    }

    @Override
    public void loadAll() {
        files.loadAll();
    }

    @Override
    public void flush() {
        files.flush();
    }

    @Override
    public void save(User user) {
        files.save(user);
    }

    @Override
    public void delete(User user) {
        files.delete(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return files.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return files.findByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return files.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return files.existsByEmail(email);
    }

    @Override
    public Optional<User> authenticate(String username, String passwordHash) {
        return files.authenticate(username, passwordHash);
    }

    @Override
    public Optional<User> findStayLoggedInUser() {
        return files.findStayLoggedInUser();
    }

    @Override
    public void clearStayLoggedIn(String username) {
        files.clearStayLoggedIn(username);
    }

    @Override
    public boolean verifySecurityAnswer(String username, String answer) {
        return files.verifySecurityAnswer(username, answer);
    }

    @Override
    public void updatePassword(String username, String newPasswordHash) {
        files.updatePassword(username, newPasswordHash);
    }

    @Override
    public void addCoins(String username, int amount) {
        mutator.addCoins(username, amount);
    }

    @Override
    public boolean spendCoins(String username, int amount) {
        return mutator.spendCoins(username, amount);
    }

    @Override
    public void addGems(String username, int amount) {
        mutator.addGems(username, amount);
    }

    @Override
    public boolean spendGems(String username, int amount) {
        return mutator.spendGems(username, amount);
    }

    @Override
    public void unlockPlant(String username, String plantName) {
        mutator.unlockPlant(username, plantName);
    }

    @Override
    public void unlockZombie(String username, String zombieName) {
        mutator.unlockZombie(username, zombieName);
    }

    @Override
    public void updateChapterProgress(String username, Chapter chapter, int level) {
        mutator.updateChapterProgress(username, chapter, level);
    }

    @Override
    public void updateHighestMyopoint(String username, int myopoint) {
        mutator.updateHighestMyopoint(username, myopoint);
    }

    @Override
    public void incrementGamesPlayed(String username) {
        mutator.incrementGamesPlayed(username);
    }

    @Override
    public void unlockMiniGame(String username, String miniGameId) {
        mutator.unlockMiniGame(username, miniGameId);
    }

    @Override
    public void updateDifficulty(String username, int difficultyLevel) {
        mutator.updateDifficulty(username, difficultyLevel);
    }

    @Override
    public void addSeedPackets(String username, String plantName, int count) {
        mutator.addSeedPackets(username, plantName, count);
    }

    @Override
    public boolean spendSeedPackets(String username, String plantName, int count) {
        return mutator.spendSeedPackets(username, plantName, count);
    }

    @Override
    public boolean addPlantFood(String username) {
        return mutator.addPlantFood(username);
    }

    @Override
    public boolean usePlantFood(String username) {
        return mutator.usePlantFood(username);
    }

    @Override
    public void storePlantBoost(String username, String plantName) {
        mutator.storePlantBoost(username, plantName);
    }

    @Override
    public boolean consumePlantBoost(String username, String plantName) {
        return mutator.consumePlantBoost(username, plantName);
    }

    @Override
    public void unlockGreenhousePot(String username, int x, int y) {
        mutator.unlockGreenhousePot(username, x, y);
    }

    @Override
    public void plantInGreenhouse(String username, int x, int y, String plantName, long timestamp) {
        mutator.plantInGreenhouse(username, x, y, plantName, timestamp);
    }

    @Override
    public void harvestGreenhousePlant(String username, int x, int y) {
        mutator.harvestGreenhousePlant(username, x, y);
    }

    @Override
    public void markNewsAsRead(String username, String newsId) {
        mutator.markNewsAsRead(username, newsId);
    }

    @Override
    public void completeQuest(String username, String questId, boolean isDaily) {
        mutator.completeQuest(username, questId, isDaily);
    }

    @Override
    public void purchaseDailyDeal(String username, String dealId) {
        mutator.purchaseDailyDeal(username, dealId);
    }

    @Override
    public boolean hasPurchasedDailyDeal(String username, String dealId) {
        return mutator.hasPurchasedDailyDeal(username, dealId);
    }

    @Override
    public List<User> findAll() {
        return files.findAll();
    }

    @Override
    public List<User> findAllOrderByMyopointDesc() {
        return files.findAllOrderByMyopointDesc();
    }

    @Override
    public List<User> findAllOrderByChapterProgressDesc() {
        return files.findAllOrderByChapterProgressDesc();
    }

    @Override
    public List<User> findAllOrderByMiniGamesDesc() {
        return files.findAllOrderByMiniGamesDesc();
    }

    @Override
    public List<User> findAllOrderByCompletedQuestsDesc() {
        return files.findAllOrderByCompletedQuestsDesc();
    }
}
