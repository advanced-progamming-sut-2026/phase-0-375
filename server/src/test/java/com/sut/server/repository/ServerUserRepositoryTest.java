package com.sut.server.repository;

import model.enums.Chapter;
import model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ServerUserRepositoryTest {

    @TempDir
    Path tempDir;

    private Path storagePath;
    private ServerUserRepository repository;

    @BeforeEach
    void setUp() {
        storagePath = tempDir.resolve("test-users.json");
        repository = new ServerUserRepository(storagePath);
    }

    private User createSampleUser(String username, String email, String passwordHash) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setNickname("Nick_" + username);
        user.setGender("male");
        user.setSecurityQuestionNumber(1);
        user.setSecurityAnswer("Answer");
        user.setCoins(100);
        user.setGems(10);
        return user;
    }

    @Test
    @DisplayName("Should save, find, and persist users to disk atomically")
    void testBasicCrudAndPersistence() {
        User u1 = createSampleUser("player1", "p1@example.com", "hash1");
        repository.save(u1);

        assertTrue(repository.existsByUsername("player1"));
        assertTrue(repository.existsByEmail("p1@example.com"));
        assertTrue(repository.existsByEmail("P1@EXAMPLE.COM")); // Case-insensitive check

        Optional<User> found = repository.findByUsername("player1");
        assertTrue(found.isPresent());
        assertEquals("player1", found.get().getUsername());
        assertEquals("p1@example.com", found.get().getEmail());

        // Verify disk file exists and is readable by a new repository instance
        assertTrue(Files.exists(storagePath));
        ServerUserRepository secondRepo = new ServerUserRepository(storagePath);
        assertTrue(secondRepo.existsByUsername("player1"));
        assertEquals(1, secondRepo.findAll().size());

        // Delete user
        repository.delete(u1);
        assertFalse(repository.existsByUsername("player1"));
        assertFalse(repository.existsByEmail("p1@example.com"));

        ServerUserRepository thirdRepo = new ServerUserRepository(storagePath);
        assertFalse(thirdRepo.existsByUsername("player1"));
        assertEquals(0, thirdRepo.findAll().size());
    }

    @Test
    @DisplayName("Should authenticate valid credentials and reject invalid ones")
    void testAuthenticate() {
        User user = createSampleUser("alice", "alice@example.com", "secretHash");
        repository.save(user);

        Optional<User> authSuccess = repository.authenticate("alice", "secretHash");
        assertTrue(authSuccess.isPresent());
        assertEquals("alice", authSuccess.get().getUsername());

        Optional<User> authWrongPass = repository.authenticate("alice", "wrongHash");
        assertTrue(authWrongPass.isEmpty());

        Optional<User> authWrongUser = repository.authenticate("bob", "secretHash");
        assertTrue(authWrongUser.isEmpty());
    }

    @Test
    @DisplayName("Should handle economy, currencies, and plant food transactions")
    void testEconomyAndPlantFood() {
        User user = createSampleUser("bob", "bob@example.com", "hash");
        user.setCoins(50);
        user.setGems(5);
        user.setPlantFoodCount(1);
        repository.save(user);

        repository.addCoins("bob", 25);
        assertEquals(75, repository.findByUsername("bob").get().getCoins());

        assertTrue(repository.spendCoins("bob", 50));
        assertEquals(25, repository.findByUsername("bob").get().getCoins());

        assertFalse(repository.spendCoins("bob", 100)); // Insufficient coins
        assertEquals(25, repository.findByUsername("bob").get().getCoins());

        repository.addGems("bob", 10);
        assertEquals(15, repository.findByUsername("bob").get().getGems());

        assertTrue(repository.spendGems("bob", 15));
        assertEquals(0, repository.findByUsername("bob").get().getGems());
        assertFalse(repository.spendGems("bob", 1));

        // Plant food cap test (Max 3)
        assertTrue(repository.addPlantFood("bob")); // 2
        assertTrue(repository.addPlantFood("bob")); // 3
        assertFalse(repository.addPlantFood("bob")); // cap reached (3)
        assertEquals(3, repository.findByUsername("bob").get().getPlantFoodCount());

        assertTrue(repository.usePlantFood("bob")); // 2
        assertTrue(repository.usePlantFood("bob")); // 1
        assertTrue(repository.usePlantFood("bob")); // 0
        assertFalse(repository.usePlantFood("bob")); // no food left
    }

    @Test
    @DisplayName("Should update progress, unlocks, greenhouse, and seed packets")
    void testProgressAndUnlocks() {
        User user = createSampleUser("charlie", "charlie@example.com", "hash");
        repository.save(user);

        repository.unlockPlant("charlie", "Repeater");
        assertTrue(repository.findByUsername("charlie").get().getUnlockedPlants().contains("Repeater"));

        repository.unlockZombie("charlie", "Buckethead");
        assertTrue(repository.findByUsername("charlie").get().getUnlockedZombies().contains("Buckethead"));

        repository.unlockMiniGame("charlie", "SlotMachine");
        assertTrue(repository.findByUsername("charlie").get().getUnlockedMiniGames().contains("SlotMachine"));

        repository.updateChapterProgress("charlie", Chapter.ANCIENT_EGYPT, 5);
        assertEquals(5, repository.findByUsername("charlie").get().getChapterProgress().get(Chapter.ANCIENT_EGYPT));

        repository.updateHighestMyopoint("charlie", 1500);
        assertEquals(1500, repository.findByUsername("charlie").get().getHighestMyopoint());

        repository.incrementGamesPlayed("charlie");
        assertEquals(1, repository.findByUsername("charlie").get().getGamesPlayed());

        repository.addSeedPackets("charlie", "Peashooter", 20);
        assertEquals(20, repository.findByUsername("charlie").get().getSeedPackets().get("Peashooter"));
        assertTrue(repository.spendSeedPackets("charlie", "Peashooter", 15));
        assertEquals(5, repository.findByUsername("charlie").get().getSeedPackets().get("Peashooter"));
        assertFalse(repository.spendSeedPackets("charlie", "Peashooter", 10));

        // Greenhouse & Quests
        repository.plantInGreenhouse("charlie", 0, 0, "Sunflower", 1234567L);
        assertEquals("Sunflower", repository.findByUsername("charlie").get().getGreenhousePots().get("0,0"));
        repository.harvestGreenhousePlant("charlie", 0, 0);
        assertNull(repository.findByUsername("charlie").get().getGreenhousePots().get("0,0"));

        repository.completeQuest("charlie", "daily_q1", true);
        repository.completeQuest("charlie", "quest_story_1", false);
        assertEquals(1, repository.findByUsername("charlie").get().getCompletedDailyQuests());
        assertEquals(1, repository.findByUsername("charlie").get().getCompletedNonDailyQuests());
    }

    @Test
    @DisplayName("Should sort leaderboards correctly")
    void testLeaderboards() {
        User u1 = createSampleUser("u1", "u1@pvz.com", "h1");
        u1.setHighestMyopoint(100);
        u1.setCompletedMiniGames(2);
        u1.setCompletedDailyQuests(1);
        u1.setCompletedNonDailyQuests(1);

        User u2 = createSampleUser("u2", "u2@pvz.com", "h2");
        u2.setHighestMyopoint(500);
        u2.setCompletedMiniGames(5);
        u2.setCompletedDailyQuests(3);
        u2.setCompletedNonDailyQuests(2);

        User u3 = createSampleUser("u3", "u3@pvz.com", "h3");
        u3.setHighestMyopoint(300);
        u3.setCompletedMiniGames(1);
        u3.setCompletedDailyQuests(0);
        u3.setCompletedNonDailyQuests(1);

        repository.save(u1);
        repository.save(u2);
        repository.save(u3);

        List<User> myopointBoard = repository.findAllOrderByMyopointDesc();
        assertEquals("u2", myopointBoard.get(0).getUsername());
        assertEquals("u3", myopointBoard.get(1).getUsername());
        assertEquals("u1", myopointBoard.get(2).getUsername());

        List<User> miniGamesBoard = repository.findAllOrderByMiniGamesDesc();
        assertEquals("u2", miniGamesBoard.get(0).getUsername());
        assertEquals("u1", miniGamesBoard.get(1).getUsername());
        assertEquals("u3", miniGamesBoard.get(2).getUsername());

        List<User> questsBoard = repository.findAllOrderByCompletedQuestsDesc();
        assertEquals("u2", questsBoard.get(0).getUsername()); // 5 total
        assertEquals("u1", questsBoard.get(1).getUsername()); // 2 total
        assertEquals("u3", questsBoard.get(2).getUsername()); // 1 total
    }

    @Test
    @DisplayName("Should handle high concurrency with multi-threaded writes without corruption")
    void testConcurrentWrites() throws InterruptedException {
        int threadCount = 20;
        int operationsPerThread = 25;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        String username = "threadUser_" + threadId + "_" + j;
                        String email = "email_" + threadId + "_" + j + "@pvz.com";
                        User u = createSampleUser(username, email, "hash_" + j);
                        repository.save(u);

                        repository.addCoins(username, 10);
                        repository.findByUsername(username);
                        repository.existsByEmail(email);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Concurrent operations timed out");
        assertEquals(threadCount * operationsPerThread, successCount.get());

        // Validate repository consistency
        ServerUserRepository reloadRepo = new ServerUserRepository(storagePath);
        assertEquals(threadCount * operationsPerThread, reloadRepo.findAll().size());
    }
}
