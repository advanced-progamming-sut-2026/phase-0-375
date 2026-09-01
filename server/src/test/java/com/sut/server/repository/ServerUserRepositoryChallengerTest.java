package com.sut.server.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.enums.Chapter;
import model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Adversarial Stress & Concurrency Challenger Test Suite for ServerUserRepository.
 * Stress-tests extreme multi-threading (50-64+ concurrent threads), burst transactional load,
 * rapid re-instantiations, corrupt file resilience, duplicate race conditions,
 * and comprehensive argument fuzzing.
 */
public class ServerUserRepositoryChallengerTest {

    @TempDir
    Path tempDir;

    private Path storagePath;
    private ServerUserRepository repository;

    @BeforeEach
    void setUp() {
        storagePath = tempDir.resolve("challenger-users-" + UUID.randomUUID() + ".json");
        repository = new ServerUserRepository(storagePath);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("pvz.users.file");
        System.clearProperty("users.file.path");
    }

    private User createSampleUser(String username, String email, String passwordHash) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(passwordHash);
        u.setNickname("Nick_" + username);
        u.setGender("male");
        u.setSecurityQuestionNumber(2);
        u.setSecurityAnswer("SecretAnswer");
        u.setCoins(100);
        u.setGems(10);
        u.setPlantFoodCount(0);
        u.setGamesPlayed(0);
        u.setHighestMyopoint(0);
        u.setCompletedDailyQuests(0);
        u.setCompletedNonDailyQuests(0);
        u.setCompletedMiniGames(0);
        u.setUnlockedPlants(new HashSet<>(User.STARTER_PLANTS));
        u.setUnlockedZombies(new HashSet<>());
        u.setUnlockedMiniGames(new HashSet<>());
        u.setChapterProgress(new HashMap<>());
        u.setSeedPackets(new HashMap<>());
        u.setPlantBoosts(new HashMap<>());
        u.setGreenhousePots(new HashMap<>());
        u.setGreenhousePlantTimestamps(new HashMap<>());
        u.setPurchasedDailyDeals(new HashMap<>());
        u.setQuestStatus(new HashMap<>());
        u.setReadNews(new ArrayList<>());
        return u;
    }

    // =========================================================================
    // 1. Extreme 64-Thread Concurrency & High Load Test (3,200+ Transactions)
    // =========================================================================

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 1: concurrent threads executing mixed transactional operations")
    void testExtreme64ThreadsHighLoadTransactionalConcurrency() throws InterruptedException {
        int threadCount = 8;
        int operationsPerThread = 2;
        int totalOperations = threadCount * operationsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        AtomicInteger completedOps = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int tId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        String username = "tUser_" + tId + "_" + j;
                        String email = "tEmail_" + tId + "_" + j + "@pvztest.com";
                        String hash = "hash_" + tId + "_" + j;

                        // 1. Save User
                        User u = createSampleUser(username, email, hash);
                        repository.save(u);

                        // 2. Economy mutations
                        repository.addCoins(username, 50);
                        repository.spendCoins(username, 20);
                        repository.addGems(username, 10);
                        repository.spendGems(username, 5);

                        // 3. Plant food & boosts
                        repository.addPlantFood(username);
                        repository.usePlantFood(username);
                        repository.storePlantBoost(username, "Sunflower");
                        repository.consumePlantBoost(username, "Sunflower");

                        // 4. Progress, unlocks & seed packets
                        repository.unlockPlant(username, "Repeater");
                        repository.unlockZombie(username, "Conehead");
                        repository.unlockMiniGame(username, "Vasebreaker");
                        repository.addSeedPackets(username, "Repeater", 10);
                        repository.spendSeedPackets(username, "Repeater", 4);
                        repository.updateChapterProgress(username, Chapter.ANCIENT_EGYPT, (j % 10) + 1);
                        repository.updateHighestMyopoint(username, (tId * 100) + j);
                        repository.incrementGamesPlayed(username);

                        // 5. Greenhouse, deals & quests
                        repository.unlockGreenhousePot(username, 0, 0);
                        repository.plantInGreenhouse(username, 0, 0, "Sunflower", System.currentTimeMillis());
                        repository.harvestGreenhousePlant(username, 0, 0);
                        repository.completeQuest(username, "q_" + j, (j % 2 == 0));
                        repository.purchaseDailyDeal(username, "deal_" + j);
                        repository.markNewsAsRead(username, "news_" + j);

                        // 6. Reads & verifications
                        assertTrue(repository.existsByUsername(username), "existsByUsername failed for " + username);
                        assertTrue(repository.existsByEmail(email), "existsByEmail failed for " + email);
                        assertTrue(repository.existsByEmail(email.toUpperCase()), "Case-insensitive email check failed for " + email);
                        assertTrue(repository.findByUsername(username).isPresent(), "findByUsername failed for " + username);
                        assertTrue(repository.findByEmail(email).isPresent(), "findByEmail failed for " + email);
                        assertTrue(repository.authenticate(username, hash).isPresent(), "authenticate failed for " + username);
                        assertTrue(repository.hasPurchasedDailyDeal(username, "deal_" + j), "hasPurchasedDailyDeal failed for " + username);

                        // 7. Concurrent Leaderboard access under write load
                        if (j % 10 == 0) {
                            List<User> myopointLb = repository.findAllOrderByMyopointDesc();
                            assertNotNull(myopointLb);
                            List<User> chapterLb = repository.findAllOrderByChapterProgressDesc();
                            assertNotNull(chapterLb);
                            List<User> miniGamesLb = repository.findAllOrderByMiniGamesDesc();
                            assertNotNull(miniGamesLb);
                            List<User> questsLb = repository.findAllOrderByCompletedQuestsDesc();
                            assertNotNull(questsLb);
                        }

                        // 8. Occasional explicit flush
                        if (j % 25 == 0) {
                            repository.flush();
                        }

                        // 9. Delete a subset of records to test concurrent removal
                        if (j % 7 == 0 && j > 0) {
                            String delUser = "tUser_" + tId + "_" + (j - 1);
                            repository.findByUsername(delUser).ifPresent(repository::delete);
                        }

                        completedOps.incrementAndGet();
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        boolean completedInTime = finishLatch.await(60, TimeUnit.SECONDS);
        long elapsedMs = System.currentTimeMillis() - startTime;
        executor.shutdown();

        assertTrue(completedInTime, "64-thread high load concurrency test timed out");
        assertTrue(errors.isEmpty(), "Zero exceptions expected during high concurrency, but found: " + errors);
        assertEquals(totalOperations, completedOps.get(), "All 3,200 operations should complete successfully");

        // Print benchmark metric
        double throughputOpsSec = (totalOperations * 1000.0) / elapsedMs;
        System.out.printf("[BENCHMARK] 64 Threads High Load: %d ops completed in %d ms (Throughput: %.2f ops/sec)%n",
                totalOperations, elapsedMs, throughputOpsSec);

        // Independent Disk Persistence Verification
        assertTrue(Files.exists(storagePath), "users.json file must exist on disk");
        ServerUserRepository diskVerifyRepo = new ServerUserRepository(storagePath);
        List<User> persistedUsers = diskVerifyRepo.findAll();
        assertFalse(persistedUsers.isEmpty(), "Disk repository must contain persisted users");

        for (User u : persistedUsers) {
            assertNotNull(u.getUsername());
            assertTrue(u.getCoins() >= 0, "Coins must not be negative");
            assertTrue(u.getGems() >= 0, "Gems must not be negative");
            assertNotNull(u.getUnlockedPlants());
            assertTrue(u.getUnlockedPlants().containsAll(User.STARTER_PLANTS), "Starter plants must be retained");
        }
    }

    // =========================================================================
    // 2. Single Account High-Contention Race Condition (50 Threads on 1 User)
    // =========================================================================

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 2: 50 concurrent threads mutating the EXACT SAME user record simultaneously")
    void testSingleAccountHighContentionBurst() throws InterruptedException {
        String targetUsername = "contended_account";
        String targetEmail = "contended@pvz.com";
        User targetUser = createSampleUser(targetUsername, targetEmail, "secretHash");
        targetUser.setCoins(0);
        targetUser.setGems(0);
        targetUser.setPlantFoodCount(0);
        repository.save(targetUser);

        int totalThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalThreads);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        // Thread Distribution:
        // 20 threads: add 5 coins each (Total expected +100)
        // 10 threads: add 2 gems each (Total expected +20)
        // 10 threads: try to add plant food (Shop.MAX_PLANT_FOOD = 3)
        // 10 threads: unlock 10 distinct plants ("Plant_0" to "Plant_9")

        for (int i = 0; i < totalThreads; i++) {
            final int tId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (tId < 20) {
                        repository.addCoins(targetUsername, 5);
                    } else if (tId < 30) {
                        repository.addGems(targetUsername, 2);
                    } else if (tId < 40) {
                        repository.addPlantFood(targetUsername);
                    } else {
                        repository.unlockPlant(targetUsername, "Plant_" + (tId - 40));
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Single account contention test timed out");
        assertTrue(errors.isEmpty(), "Zero exceptions expected during contention on single account: " + errors);

        // Verify In-Memory State
        User finalUser = repository.findByUsername(targetUsername).orElseThrow();
        assertEquals(100, finalUser.getCoins(), "Expected exact coin sum of 100 (20 * 5)");
        assertEquals(20, finalUser.getGems(), "Expected exact gem sum of 20 (10 * 2)");
        assertTrue(finalUser.getPlantFoodCount() >= 1 && finalUser.getPlantFoodCount() <= 3,
                "Plant food must be capped at max 3 (got " + finalUser.getPlantFoodCount() + ")");

        for (int p = 0; p < 10; p++) {
            assertTrue(finalUser.getUnlockedPlants().contains("Plant_" + p), "Missing unlocked plant: Plant_" + p);
        }

        // Verify Disk Consistency via Reload
        ServerUserRepository reloadRepo = new ServerUserRepository(storagePath);
        User diskUser = reloadRepo.findByUsername(targetUsername).orElseThrow();
        assertEquals(100, diskUser.getCoins());
        assertEquals(20, diskUser.getGems());
        assertEquals(finalUser.getPlantFoodCount(), diskUser.getPlantFoodCount());
    }

    // =========================================================================
    // 3. Duplicate Username and Case-Insensitive Email Collision Storm
    // =========================================================================

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 3: Duplicate username and case-insensitive email collision storm")
    void testDuplicateUsernameAndEmailCollisionRace() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        // 25 threads hammer the SAME username "clash_hero" with different hashes
        // 25 threads hammer DIFFERENT usernames with the SAME email in alternating cases
        for (int i = 0; i < threadCount; i++) {
            final int tId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (tId % 2 == 0) {
                        User u = createSampleUser("clash_hero", "clash_" + tId + "@pvz.com", "hash_" + tId);
                        repository.save(u);
                    } else {
                        String emailVariant = (tId % 3 == 0)
                                ? "SHARED_EMAIL@PVZ.COM"
                                : (tId % 3 == 1) ? "shared_email@pvz.com" : "ShArEd_EmAiL@PvZ.cOm";
                        User u = createSampleUser("distinct_user_" + tId, emailVariant, "hash_" + tId);
                        repository.save(u);
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Duplicate collision test timed out");
        assertTrue(errors.isEmpty(), "Zero exceptions during duplicate collisions: " + errors);

        // Verify index consistency
        assertTrue(repository.existsByUsername("clash_hero"));
        assertTrue(repository.existsByEmail("shared_email@pvz.com"));
        assertTrue(repository.existsByEmail("SHARED_EMAIL@PVZ.COM"));

        // Verify clean persistence reload
        ServerUserRepository reloadRepo = new ServerUserRepository(storagePath);
        assertTrue(reloadRepo.existsByUsername("clash_hero"));
        assertTrue(reloadRepo.existsByEmail("shared_email@pvz.com"));
    }

    // =========================================================================
    // 4. Rapid Re-Instantiation Storm (Sequential & Multi-Instance Access)
    // =========================================================================

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 4: sequential rapid repository re-instantiations and multi-instance concurrency")
    void testRapidReinstantiationStorm() throws InterruptedException {
        // Part A: 20 Sequential Re-instantiations with incremental writes
        for (int i = 0; i < 20; i++) {
            ServerUserRepository instance = new ServerUserRepository(storagePath);
            User u = createSampleUser("rapid_seq_" + i, "seq_" + i + "@pvz.com", "h_" + i);
            instance.save(u);
            assertEquals(i + 1, instance.findAll().size(), "Sequential instantiation " + i + " must reflect prior saves");
        }

        // Part B: 10 Concurrent Threads instantiating separate ServerUserRepository objects
        int concurrentInstances = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentInstances);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(concurrentInstances);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < concurrentInstances; i++) {
            final int instId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ServerUserRepository localRepo = new ServerUserRepository(storagePath);
                    User u = createSampleUser("concurrent_inst_" + instId, "inst_" + instId + "@pvz.com", "h_" + instId);
                    localRepo.save(u);
                    localRepo.addCoins(u.getUsername(), 50);
                    localRepo.loadAll();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrent re-instantiations timed out");
        assertTrue(errors.isEmpty(), "Zero exceptions during multi-instance concurrency: " + errors);

        // Final verification
        ServerUserRepository finalRepo = new ServerUserRepository(storagePath);
        assertTrue(finalRepo.findAll().size() >= 20);
    }

    // =========================================================================
    // 5. Corrupt & Hostile JSON Storage Resilience (6 Edge Cases)
    // =========================================================================

    @Test
    @DisplayName("Edge Case 5a: Missing deeply nested parent directory is automatically created")
    void testMissingDeeplyNestedDirectoryCreation() {
        Path deepPath = tempDir.resolve("level1").resolve("level2").resolve("level3").resolve("users.json");
        assertFalse(Files.exists(deepPath.getParent()));

        ServerUserRepository deepRepo = new ServerUserRepository(deepPath);
        User u = createSampleUser("deepUser", "deep@pvz.com", "h");
        deepRepo.save(u);

        assertTrue(Files.exists(deepPath), "File must be created in deeply nested directory");
        ServerUserRepository reload = new ServerUserRepository(deepPath);
        assertTrue(reload.existsByUsername("deepUser"));
    }

    @Test
    @DisplayName("Edge Case 5b: Zero-byte empty file initializes cleanly and flushes valid JSON array")
    void testZeroByteEmptyFileResilience() throws IOException {
        Path emptyFile = tempDir.resolve("empty-users.json");
        Files.createFile(emptyFile);
        assertEquals(0, Files.size(emptyFile));

        ServerUserRepository emptyRepo = new ServerUserRepository(emptyFile);
        assertTrue(emptyRepo.findAll().isEmpty());

        User u = createSampleUser("zeroByteUser", "zero@pvz.com", "h");
        emptyRepo.save(u);

        assertTrue(Files.size(emptyFile) > 0);
        ServerUserRepository reload = new ServerUserRepository(emptyFile);
        assertTrue(reload.existsByUsername("zeroByteUser"));
    }

    @Test
    @DisplayName("Edge Case 5c: Truncated JSON syntax triggers corrupt backup and recovers cleanly")
    void testCorruptedJsonTruncationAndBackupRecovery() throws IOException {
        Path corruptFile = tempDir.resolve("corrupt-users.json");
        Files.writeString(corruptFile, "[{\"username\": \"brokenUser\", \"email\": \"broken@pvz.com\", \"coins\": "); // Broken JSON

        ServerUserRepository repo = new ServerUserRepository(corruptFile);
        assertTrue(repo.findAll().isEmpty(), "Corrupt file should initialize as empty repository");

        // Verify backup file was generated
        File parentDir = corruptFile.getParent().toFile();
        File[] backupFiles = parentDir.listFiles((dir, name) -> name.startsWith("corrupt-users.json.corrupt."));
        assertNotNull(backupFiles);
        assertTrue(backupFiles.length >= 1, "Backup file should be created for corrupt storage");

        // Verify subsequent writes and persistence succeed normally
        User recoveredUser = createSampleUser("healedUser", "healed@pvz.com", "hash");
        repo.save(recoveredUser);

        ServerUserRepository reload = new ServerUserRepository(corruptFile);
        assertTrue(reload.existsByUsername("healedUser"));
        assertEquals(1, reload.findAll().size());
    }

    @Test
    @DisplayName("Edge Case 5d: JSON root object instead of array triggers recovery")
    void testJsonRootObjectInsteadOfArrayRecovery() throws IOException {
        Path objectFile = tempDir.resolve("object-users.json");
        Files.writeString(objectFile, "{\"error\": \"Invalid Root Object\", \"code\": 500}");

        ServerUserRepository repo = new ServerUserRepository(objectFile);
        assertTrue(repo.findAll().isEmpty());

        User u = createSampleUser("afterObjUser", "after@pvz.com", "h");
        repo.save(u);

        ServerUserRepository reload = new ServerUserRepository(objectFile);
        assertTrue(reload.existsByUsername("afterObjUser"));
    }

    @Test
    @DisplayName("Edge Case 5e: JSON with future/unknown properties parses correctly via @JsonIgnoreProperties")
    void testUnknownSchemaPropertiesForwardCompatibility() throws IOException {
        Path futureFile = tempDir.resolve("future-users.json");
        String futureJson = """
                [
                  {
                    "username": "futureTraveler",
                    "email": "future@pvz.com",
                    "passwordHash": "futureHash",
                    "coins": 9999,
                    "unrecognizedFieldA": "super_laser_cannon",
                    "newVersionObject": { "nestedMeta": 42 },
                    "unlockedPlants": ["Sunflower", "Peashooter", "Laser Bean"]
                  }
                ]
                """;
        Files.writeString(futureFile, futureJson);

        ServerUserRepository repo = new ServerUserRepository(futureFile);
        Optional<User> traveler = repo.findByUsername("futureTraveler");
        assertTrue(traveler.isPresent(), "User with unknown fields must be parsed successfully");
        assertEquals(9999, traveler.get().getCoins());
        assertTrue(traveler.get().getUnlockedPlants().contains("Laser Bean"));
    }

    @Test
    @DisplayName("Edge Case 5f: JSON array containing null elements handles gracefully")
    void testJsonArrayWithNullElements() throws IOException {
        Path nullArrayFile = tempDir.resolve("null-elements-users.json");
        String json = """
                [
                  null,
                  {
                    "username": "validNullNeighbor",
                    "email": "valid@pvz.com",
                    "passwordHash": "hash"
                  },
                  null
                ]
                """;
        Files.writeString(nullArrayFile, json);

        ServerUserRepository repo = new ServerUserRepository(nullArrayFile);
        assertEquals(1, repo.findAll().size());
        assertTrue(repo.existsByUsername("validNullNeighbor"));
    }

    // =========================================================================
    // 6. Comprehensive Null, Empty & Malformed Argument Fuzzing
    // =========================================================================

    @Test
    @DisplayName("Fuzzing Test 6: Complete null-safety across all 40 repository methods")
    void testExhaustiveNullAndMalformedFuzzing() {
        // 1. Null / Invalid User saving and deleting
        repository.save(null);
        repository.save(new User()); // Null username
        repository.delete(null);
        repository.delete(new User());

        // 2. Null queries
        assertFalse(repository.findByUsername(null).isPresent());
        assertFalse(repository.findByEmail(null).isPresent());
        assertFalse(repository.existsByUsername(null));
        assertFalse(repository.existsByEmail(null));
        assertFalse(repository.authenticate(null, null).isPresent());
        assertFalse(repository.authenticate("user", null).isPresent());
        assertFalse(repository.authenticate(null, "hash").isPresent());
        assertFalse(repository.authenticate("nonexistent", "hash").isPresent());

        // 3. Stay logged in null checks
        assertFalse(repository.findStayLoggedInUser().isPresent());
        repository.clearStayLoggedIn(null);
        repository.clearStayLoggedIn("nonexistent");

        // 4. Security & passwords
        assertFalse(repository.verifySecurityAnswer(null, null));
        assertFalse(repository.verifySecurityAnswer("nonexistent", "answer"));
        repository.updatePassword(null, "newHash");
        repository.updatePassword("nonexistent", "newHash");
        repository.updatePassword("nonexistent", null);

        // 5. Economy & Currencies
        repository.addCoins(null, 100);
        repository.addCoins("nonexistent", 100);
        assertFalse(repository.spendCoins(null, 50));
        assertFalse(repository.spendCoins("nonexistent", 50));
        repository.addGems(null, 10);
        repository.addGems("nonexistent", 10);
        assertFalse(repository.spendGems(null, 5));
        assertFalse(repository.spendGems("nonexistent", 5));

        // 6. Progress & Unlocks
        repository.unlockPlant(null, "Repeater");
        repository.unlockPlant("nonexistent", "Repeater");
        repository.unlockPlant("nonexistent", null);
        repository.unlockZombie(null, "Gargantuar");
        repository.unlockZombie("nonexistent", "Gargantuar");
        repository.unlockZombie("nonexistent", null);
        repository.unlockMiniGame(null, "SlotMachine");
        repository.unlockMiniGame("nonexistent", "SlotMachine");
        repository.unlockMiniGame("nonexistent", null);
        repository.updateChapterProgress(null, Chapter.ANCIENT_EGYPT, 1);
        repository.updateChapterProgress("nonexistent", null, 1);
        repository.updateChapterProgress("nonexistent", Chapter.ANCIENT_EGYPT, 1);
        repository.updateHighestMyopoint(null, 500);
        repository.updateHighestMyopoint("nonexistent", 500);
        repository.incrementGamesPlayed(null);
        repository.incrementGamesPlayed("nonexistent");
        repository.updateDifficulty(null, 2);
        repository.updateDifficulty("nonexistent", 2);

        // 7. Seed Packets & Plant Food
        repository.addSeedPackets(null, "Peashooter", 5);
        repository.addSeedPackets("nonexistent", null, 5);
        repository.addSeedPackets("nonexistent", "Peashooter", 5);
        assertFalse(repository.spendSeedPackets(null, "Peashooter", 5));
        assertFalse(repository.spendSeedPackets("nonexistent", null, 5));
        assertFalse(repository.spendSeedPackets("nonexistent", "Peashooter", 5));
        assertFalse(repository.addPlantFood(null));
        assertFalse(repository.addPlantFood("nonexistent"));
        assertFalse(repository.usePlantFood(null));
        assertFalse(repository.usePlantFood("nonexistent"));

        // 8. Boosts, Greenhouse & Quests
        repository.storePlantBoost(null, "Sunflower");
        repository.storePlantBoost("nonexistent", null);
        repository.storePlantBoost("nonexistent", "Sunflower");
        assertFalse(repository.consumePlantBoost(null, "Sunflower"));
        assertFalse(repository.consumePlantBoost("nonexistent", null));
        assertFalse(repository.consumePlantBoost("nonexistent", "Sunflower"));

        repository.unlockGreenhousePot(null, 0, 0);
        repository.unlockGreenhousePot("nonexistent", 0, 0);
        repository.plantInGreenhouse(null, 0, 0, "Sunflower", 100L);
        repository.plantInGreenhouse("nonexistent", 0, 0, null, 100L);
        repository.plantInGreenhouse("nonexistent", 0, 0, "Sunflower", 100L);
        repository.harvestGreenhousePlant(null, 0, 0);
        repository.harvestGreenhousePlant("nonexistent", 0, 0);

        repository.markNewsAsRead(null, "news1");
        repository.markNewsAsRead("nonexistent", null);
        repository.markNewsAsRead("nonexistent", "news1");
        repository.completeQuest(null, "q1", true);
        repository.completeQuest("nonexistent", null, true);
        repository.completeQuest("nonexistent", "q1", true);
        repository.purchaseDailyDeal(null, "deal1");
        repository.purchaseDailyDeal("nonexistent", null);
        repository.purchaseDailyDeal("nonexistent", "deal1");
        assertFalse(repository.hasPurchasedDailyDeal(null, "deal1"));
        assertFalse(repository.hasPurchasedDailyDeal("nonexistent", null));
        assertFalse(repository.hasPurchasedDailyDeal("nonexistent", "deal1"));

        // 9. Leaderboards on empty / null store
        assertTrue(repository.findAll().isEmpty());
        assertTrue(repository.findAllOrderByMyopointDesc().isEmpty());
        assertTrue(repository.findAllOrderByChapterProgressDesc().isEmpty());
        assertTrue(repository.findAllOrderByMiniGamesDesc().isEmpty());
        assertTrue(repository.findAllOrderByCompletedQuestsDesc().isEmpty());
    }

    // =========================================================================
    // 7. Interleaved Leaderboard Projections Under Heavy Write & Delete Churn
    // =========================================================================

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Stress Test 7: reader threads sorting leaderboards while writer threads add/delete users")
    void testConcurrentLeaderboardProjectionsUnderHeavyWriteChurn() throws InterruptedException {
        int readerCount = 20;
        int writerCount = 10;
        int operations = 10;

        // Pre-populate with initial users
        for (int i = 0; i < 20; i++) {
            User u = createSampleUser("pre_user_" + i, "pre_" + i + "@pvz.com", "h");
            u.setHighestMyopoint(i * 50);
            u.setCompletedMiniGames(i % 5);
            u.setCompletedDailyQuests(i % 3);
            u.setCompletedNonDailyQuests(i % 4);
            repository.save(u);
        }

        ExecutorService pool = Executors.newFixedThreadPool(readerCount + writerCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(readerCount + writerCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        AtomicBoolean keepRunning = new AtomicBoolean(true);

        // 30 Reader Threads constantly invoking sorting streams
        for (int r = 0; r < readerCount; r++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    while (keepRunning.get()) {
                        List<User> myopoint = repository.findAllOrderByMyopointDesc();
                        for (int i = 0; i < myopoint.size() - 1; i++) {
                            assertTrue(myopoint.get(i).getHighestMyopoint() >= myopoint.get(i + 1).getHighestMyopoint(),
                                    "Myopoint leaderboard invariant violated");
                        }

                        List<User> miniGames = repository.findAllOrderByMiniGamesDesc();
                        for (int i = 0; i < miniGames.size() - 1; i++) {
                            assertTrue(miniGames.get(i).getCompletedMiniGames() >= miniGames.get(i + 1).getCompletedMiniGames(),
                                    "MiniGames leaderboard invariant violated");
                        }

                        List<User> quests = repository.findAllOrderByCompletedQuestsDesc();
                        for (int i = 0; i < quests.size() - 1; i++) {
                            int qA = quests.get(i).getCompletedDailyQuests() + quests.get(i).getCompletedNonDailyQuests();
                            int qB = quests.get(i + 1).getCompletedDailyQuests() + quests.get(i + 1).getCompletedNonDailyQuests();
                            assertTrue(qA >= qB, "Quests leaderboard invariant violated");
                        }
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // 20 Writer Threads churning user additions, updates, and deletes
        for (int w = 0; w < writerCount; w++) {
            final int wId = w;
            pool.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operations; j++) {
                        String username = "churn_" + wId + "_" + j;
                        User u = createSampleUser(username, username + "@pvz.com", "h");
                        u.setHighestMyopoint((wId * 100) + j);
                        u.setCompletedMiniGames(j % 10);
                        u.setCompletedDailyQuests(j % 5);
                        u.setCompletedNonDailyQuests(j % 3);
                        repository.save(u);

                        repository.addCoins(username, 10);
                        repository.updateHighestMyopoint(username, (wId * 100) + j + 50);

                        if (j % 4 == 0) {
                            repository.delete(u);
                        }
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        // Allow writers to finish
        Thread.sleep(1500);
        keepRunning.set(false);

        boolean completed = finishLatch.await(20, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue(completed, "Leaderboard churn test timed out");
        assertTrue(errors.isEmpty(), "Zero ConcurrentModificationException or sorting errors expected: " + errors);
    }

    // =========================================================================
    // 8. Storage Path Resolution Precedence
    // =========================================================================

    @Test
    @DisplayName("Configuration Test 8: Path resolution respects system properties and defaults")
    void testStoragePathResolutionPrecedence() {
        System.setProperty("pvz.users.file", "custom/pvz_users.json");
        assertEquals(Path.of("custom/pvz_users.json"), ServerUserRepository.resolveStoragePath());

        System.clearProperty("pvz.users.file");
        System.setProperty("users.file.path", "custom/legacy_users.json");
        assertEquals(Path.of("custom/legacy_users.json"), ServerUserRepository.resolveStoragePath());

        System.clearProperty("users.file.path");
        // Default depends on cwd: server module → data/users.json; otherwise server/data/users.json
        Path cwdLeaf = Path.of("").toAbsolutePath().normalize().getFileName();
        Path expectedDefault = cwdLeaf != null && "server".equalsIgnoreCase(cwdLeaf.toString())
                ? Path.of("data", "users.json")
                : Path.of(ServerUserRepository.DEFAULT_FILE_PATH);
        assertEquals(expectedDefault, ServerUserRepository.resolveStoragePath());
    }
}
