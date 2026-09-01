package com.sut.server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.news.NewsFactory;
import model.user.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence, CRUD, and leaderboard projections for {@link ServerUserRepository}.
 */
final class ServerUserFileStore {

    private final Path storagePath;
    private final ObjectMapper mapper;
    private final ServerUserTables tables;

    ServerUserFileStore(Path storagePath, ObjectMapper mapper, ServerUserTables tables) {
        this.storagePath = storagePath;
        this.mapper = mapper;
        this.tables = tables;
    }

    Path getStoragePath() {
        return storagePath;
    }

    void loadAll() {
        tables.rwLock.writeLock().lock();
        try {
            tables.users.clear();
            tables.usersByUsername.clear();
            tables.usersByEmail.clear();

            File file = storagePath.toFile();
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            if (!file.exists() || file.length() == 0) {
                flushInternal();
                return;
            }

            readUsersOrBackup(file);
            migrateStarterPlantsInternal();
        } finally {
            tables.rwLock.writeLock().unlock();
        }
    }

    private void readUsersOrBackup(File file) {
        try {
            List<User> loaded = mapper.readValue(file, new TypeReference<ArrayList<User>>() {});
            if (loaded == null) {
                return;
            }
            for (User u : loaded) {
                if (u != null && u.getUsername() != null) {
                    tables.users.add(u);
                    tables.usersByUsername.put(u.getUsername(), u);
                    if (u.getEmail() != null) {
                        tables.usersByEmail.put(u.getEmail().toLowerCase(), u);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[ServerUserRepository] Error parsing " + storagePath
                    + ": " + e.getMessage());
            backupCorruptFile(file);
        }
    }

    private void backupCorruptFile(File corruptFile) {
        try {
            File backup = new File(corruptFile.getParentFile(),
                    corruptFile.getName() + ".corrupt." + System.currentTimeMillis());
            Files.copy(corruptFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.err.println("[ServerUserRepository] Backed up corrupt file to: "
                    + backup.getAbsolutePath());
        } catch (IOException ignored) {
        }
    }

    private void migrateStarterPlantsInternal() {
        boolean stampedDates = false;
        for (User u : tables.users) {
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

    void flush() {
        tables.rwLock.writeLock().lock();
        try {
            flushInternal();
        } finally {
            tables.rwLock.writeLock().unlock();
        }
    }

    void flushInternal() {
        File targetFile = storagePath.toFile();
        File parentDir = targetFile.getParentFile();
        ensureParent(parentDir);

        File tempFile = new File(parentDir != null ? parentDir : new File("."),
                targetFile.getName() + ".tmp." + UUID.randomUUID());
        try {
            writeThenMove(targetFile, tempFile);
        } catch (IOException e) {
            System.err.println("[ServerUserRepository] Failed to flush users to "
                    + storagePath + ": " + e.getMessage());
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private void ensureParent(File parentDir) {
        if (parentDir == null) {
            return;
        }
        try {
            Files.createDirectories(parentDir.toPath());
        } catch (IOException ignored) {
            parentDir.mkdirs();
        }
    }

    private void writeThenMove(File targetFile, File tempFile) throws IOException {
        try (java.io.OutputStream os = new java.io.BufferedOutputStream(
                new java.io.FileOutputStream(tempFile), 65536)) {
            mapper.writeValue(os, tables.users);
        }
        try {
            Files.move(tempFile.toPath(), targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFile.toPath(), targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    void save(User user) {
        if (user == null || user.getUsername() == null) {
            return;
        }
        tables.rwLock.writeLock().lock();
        try {
            User existing = tables.usersByUsername.get(user.getUsername());
            if (existing != null) {
                tables.users.remove(existing);
                if (existing.getEmail() != null) {
                    tables.usersByEmail.remove(existing.getEmail().toLowerCase());
                }
            }
            tables.users.add(user);
            tables.usersByUsername.put(user.getUsername(), user);
            if (user.getEmail() != null) {
                tables.usersByEmail.put(user.getEmail().toLowerCase(), user);
            }
            flushInternal();
        } finally {
            tables.rwLock.writeLock().unlock();
        }
    }

    void delete(User user) {
        if (user == null || user.getUsername() == null) {
            return;
        }
        tables.rwLock.writeLock().lock();
        try {
            User existing = tables.usersByUsername.remove(user.getUsername());
            if (existing != null) {
                tables.users.remove(existing);
                if (existing.getEmail() != null) {
                    tables.usersByEmail.remove(existing.getEmail().toLowerCase());
                }
                flushInternal();
            }
        } finally {
            tables.rwLock.writeLock().unlock();
        }
    }

    Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        tables.rwLock.readLock().lock();
        try {
            return Optional.ofNullable(tables.usersByUsername.get(username));
        } finally {
            tables.rwLock.readLock().unlock();
        }
    }

    Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        tables.rwLock.readLock().lock();
        try {
            return Optional.ofNullable(tables.usersByEmail.get(email.toLowerCase()));
        } finally {
            tables.rwLock.readLock().unlock();
        }
    }

    boolean existsByUsername(String username) {
        if (username == null) {
            return false;
        }
        tables.rwLock.readLock().lock();
        try {
            return tables.usersByUsername.containsKey(username);
        } finally {
            tables.rwLock.readLock().unlock();
        }
    }

    boolean existsByEmail(String email) {
        if (email == null) {
            return false;
        }
        tables.rwLock.readLock().lock();
        try {
            return tables.usersByEmail.containsKey(email.toLowerCase());
        } finally {
            tables.rwLock.readLock().unlock();
        }
    }

    Optional<User> authenticate(String username, String passwordHash) {
        if (username == null || passwordHash == null) {
            return Optional.empty();
        }
        tables.rwLock.readLock().lock();
        try {
            User u = tables.usersByUsername.get(username);
            if (u != null && passwordHash.equals(u.getPasswordHash())) {
                return Optional.of(u);
            }
            return Optional.empty();
        } finally {
            tables.rwLock.readLock().unlock();
        }
    }

    Optional<User> findStayLoggedInUser() {
        tables.rwLock.readLock().lock();
        try {
            return tables.users.stream().filter(User::isStayLoggedIn).findFirst();
        } finally {
            tables.rwLock.readLock().unlock();
        }
    }

    void clearStayLoggedIn(String username) {
        if (username == null) {
            return;
        }
        tables.rwLock.writeLock().lock();
        try {
            User u = tables.usersByUsername.get(username);
            if (u != null) {
                u.setStayLoggedIn(false);
                flushInternal();
            }
        } finally {
            tables.rwLock.writeLock().unlock();
        }
    }

    boolean verifySecurityAnswer(String username, String answer) {
        if (username == null || answer == null) {
            return false;
        }
        tables.rwLock.readLock().lock();
        try {
            User u = tables.usersByUsername.get(username);
            return u != null && u.getSecurityAnswer() != null && u.getSecurityAnswer().equals(answer);
        } finally {
            tables.rwLock.readLock().unlock();
        }
    }

    void updatePassword(String username, String newPasswordHash) {
        if (username == null || newPasswordHash == null) {
            return;
        }
        tables.rwLock.writeLock().lock();
        try {
            User u = tables.usersByUsername.get(username);
            if (u != null) {
                u.setPasswordHash(newPasswordHash);
                flushInternal();
            }
        } finally {
            tables.rwLock.writeLock().unlock();
        }
    }

    List<User> findAll() {
        tables.rwLock.readLock().lock();
        try {
            return new ArrayList<>(tables.users);
        } finally {
            tables.rwLock.readLock().unlock();
        }
    }

    List<User> findAllOrderByMyopointDesc() {
        tables.rwLock.readLock().lock();
        try {
            return tables.users.stream()
                    .map(this::copyUser)
                    .sorted((a, b) -> Integer.compare(b.getHighestMyopoint(), a.getHighestMyopoint()))
                    .collect(Collectors.toList());
        } finally {
            tables.rwLock.readLock().unlock();
        }
    }

    List<User> findAllOrderByChapterProgressDesc() {
        tables.rwLock.readLock().lock();
        try {
            return tables.users.stream()
                    .map(this::copyUser)
                    .sorted((a, b) -> Integer.compare(chapterTotal(b), chapterTotal(a)))
                    .collect(Collectors.toList());
        } finally {
            tables.rwLock.readLock().unlock();
        }
    }

    List<User> findAllOrderByMiniGamesDesc() {
        tables.rwLock.readLock().lock();
        try {
            return tables.users.stream()
                    .map(this::copyUser)
                    .sorted((a, b) -> Integer.compare(b.getCompletedMiniGames(), a.getCompletedMiniGames()))
                    .collect(Collectors.toList());
        } finally {
            tables.rwLock.readLock().unlock();
        }
    }

    List<User> findAllOrderByCompletedQuestsDesc() {
        tables.rwLock.readLock().lock();
        try {
            return tables.users.stream()
                    .map(this::copyUser)
                    .sorted((a, b) -> Integer.compare(questTotal(b), questTotal(a)))
                    .collect(Collectors.toList());
        } finally {
            tables.rwLock.readLock().unlock();
        }
    }

    private static int chapterTotal(User u) {
        if (u.getChapterProgress() == null) {
            return 0;
        }
        return u.getChapterProgress().values().stream().mapToInt(Integer::intValue).sum();
    }

    private static int questTotal(User u) {
        return u.getCompletedDailyQuests() + u.getCompletedNonDailyQuests();
    }

    private User copyUser(User source) {
        if (source == null) {
            return null;
        }
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
}
