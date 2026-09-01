package com.sut.server.repository;

import model.enums.Chapter;
import model.news.NewsFactory;
import model.plant.PlantFactory;
import model.shop.Shop;
import model.user.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Economy and progress mutations for {@link ServerUserRepository}.
 */
final class ServerUserMutator {

    private static final String PLANTS_JSON = "/assets/data/plants/plants.json";

    private final ServerUserTables tables;
    private final ServerUserFileStore files;

    ServerUserMutator(ServerUserTables tables, ServerUserFileStore files) {
        this.tables = tables;
        this.files = files;
    }

    void addCoins(String username, int amount) {
        mutate(username, u -> u.setCoins(u.getCoins() + amount));
    }

    boolean spendCoins(String username, int amount) {
        return mutateIf(username, u -> u.getCoins() >= amount, u -> u.setCoins(u.getCoins() - amount));
    }

    void addGems(String username, int amount) {
        mutate(username, u -> u.setGems(u.getGems() + amount));
    }

    boolean spendGems(String username, int amount) {
        return mutateIf(username, u -> u.getGems() >= amount, u -> u.setGems(u.getGems() - amount));
    }

    void unlockPlant(String username, String plantName) {
        if (plantName == null) {
            return;
        }
        mutate(username, u -> {
            if (u.getUnlockedPlants() == null) {
                u.setUnlockedPlants(new HashSet<>());
            }
            if (u.getUnlockedPlants().add(plantName)) {
                u.rememberNewsPublishDate(NewsFactory.plantNewsId(plantName));
            }
        });
    }

    void unlockZombie(String username, String zombieName) {
        if (zombieName == null) {
            return;
        }
        mutate(username, u -> {
            if (u.getUnlockedZombies() == null) {
                u.setUnlockedZombies(new HashSet<>());
            }
            if (u.getUnlockedZombies().add(zombieName)) {
                u.rememberNewsPublishDate(NewsFactory.zombieNewsId(zombieName));
            }
        });
    }

    void updateChapterProgress(String username, Chapter chapter, int level) {
        if (chapter == null) {
            return;
        }
        mutate(username, u -> {
            if (u.getChapterProgress() == null) {
                u.setChapterProgress(new HashMap<>());
            }
            u.getChapterProgress().put(chapter, level);
        });
    }

    void updateHighestMyopoint(String username, int myopoint) {
        mutateIf(username, u -> myopoint > u.getHighestMyopoint(), u -> u.setHighestMyopoint(myopoint));
    }

    void incrementGamesPlayed(String username) {
        mutate(username, u -> u.setGamesPlayed(u.getGamesPlayed() + 1));
    }

    void unlockMiniGame(String username, String miniGameId) {
        if (miniGameId == null) {
            return;
        }
        mutate(username, u -> {
            if (u.getUnlockedMiniGames() == null) {
                u.setUnlockedMiniGames(new HashSet<>());
            }
            if (u.getUnlockedMiniGames().add(miniGameId)) {
                u.rememberNewsPublishDate(NewsFactory.miniGameNewsId(miniGameId));
            }
        });
    }

    void updateDifficulty(String username, int difficultyLevel) {
        mutate(username, u -> u.setDifficultyLevel(difficultyLevel));
    }

    void addSeedPackets(String username, String plantName, int count) {
        if (plantName == null) {
            return;
        }
        mutate(username, u -> {
            if (u.getSeedPackets() == null) {
                u.setSeedPackets(new HashMap<>());
            }
            u.getSeedPackets().put(plantName, u.getSeedPackets().getOrDefault(plantName, 0) + count);
        });
    }

    boolean spendSeedPackets(String username, String plantName, int count) {
        if (username == null || plantName == null) {
            return false;
        }
        tables.rwLock.writeLock().lock();
        try {
            User u = tables.getByUsername(username);
            if (u != null && u.getSeedPackets() != null) {
                int current = u.getSeedPackets().getOrDefault(plantName, 0);
                if (current >= count) {
                    u.getSeedPackets().put(plantName, current - count);
                    files.flushInternal();
                    return true;
                }
            }
            return false;
        } finally {
            tables.rwLock.writeLock().unlock();
        }
    }

    boolean addPlantFood(String username) {
        if (username == null) {
            return false;
        }
        tables.rwLock.writeLock().lock();
        try {
            User u = tables.getByUsername(username);
            if (u == null) {
                return false;
            }
            int current = Math.max(0, u.getPlantFoodCount());
            if (current >= Shop.MAX_PLANT_FOOD) {
                return false;
            }
            u.setPlantFoodCount(current + 1);
            files.flushInternal();
            return true;
        } finally {
            tables.rwLock.writeLock().unlock();
        }
    }

    boolean usePlantFood(String username) {
        if (username == null) {
            return false;
        }
        tables.rwLock.writeLock().lock();
        try {
            User u = tables.getByUsername(username);
            if (u == null) {
                return false;
            }
            int current = u.getPlantFoodCount();
            if (current <= 0) {
                if (current < 0) {
                    u.setPlantFoodCount(0);
                    files.flushInternal();
                }
                return false;
            }
            u.setPlantFoodCount(current - 1);
            files.flushInternal();
            return true;
        } finally {
            tables.rwLock.writeLock().unlock();
        }
    }

    void storePlantBoost(String username, String plantName) {
        String canonical = resolvePlantName(plantName);
        if (canonical == null || username == null) {
            return;
        }
        mutate(username, u -> {
            if (u.getPlantBoosts() == null) {
                u.setPlantBoosts(new HashMap<>());
            }
            u.getPlantBoosts().put(canonical, true);
        });
    }

    boolean consumePlantBoost(String username, String plantName) {
        String canonical = resolvePlantName(plantName);
        if (canonical == null || username == null) {
            return false;
        }
        tables.rwLock.writeLock().lock();
        try {
            User u = tables.getByUsername(username);
            if (u != null && u.getPlantBoosts() != null) {
                Boolean boost = u.getPlantBoosts().get(canonical);
                if (Boolean.TRUE.equals(boost)) {
                    u.getPlantBoosts().put(canonical, false);
                    files.flushInternal();
                    return true;
                }
            }
            return false;
        } finally {
            tables.rwLock.writeLock().unlock();
        }
    }

    void unlockGreenhousePot(String username, int x, int y) {
        mutateIf(username, u -> u.getUnlockedPots() < 20, u -> u.setUnlockedPots(u.getUnlockedPots() + 1));
    }

    void plantInGreenhouse(String username, int x, int y, String plantName, long timestamp) {
        if (plantName == null) {
            return;
        }
        mutate(username, u -> {
            String key = x + "," + y;
            if (u.getGreenhousePots() == null) {
                u.setGreenhousePots(new HashMap<>());
            }
            if (u.getGreenhousePlantTimestamps() == null) {
                u.setGreenhousePlantTimestamps(new HashMap<>());
            }
            u.getGreenhousePots().put(key, plantName);
            u.getGreenhousePlantTimestamps().put(key, timestamp);
        });
    }

    void harvestGreenhousePlant(String username, int x, int y) {
        mutate(username, u -> {
            String key = x + "," + y;
            if (u.getGreenhousePots() != null) {
                u.getGreenhousePots().remove(key);
            }
            if (u.getGreenhousePlantTimestamps() != null) {
                u.getGreenhousePlantTimestamps().remove(key);
            }
        });
    }

    void markNewsAsRead(String username, String newsId) {
        if (newsId == null) {
            return;
        }
        mutate(username, u -> {
            if (u.getReadNews() == null) {
                u.setReadNews(new ArrayList<>());
            }
            u.getReadNews().add(newsId);
        });
    }

    void completeQuest(String username, String questId, boolean isDaily) {
        if (questId == null) {
            return;
        }
        mutate(username, u -> {
            if (u.getQuestStatus() == null) {
                u.setQuestStatus(new HashMap<>());
            }
            u.getQuestStatus().put(questId, true);
            if (isDaily) {
                u.setCompletedDailyQuests(u.getCompletedDailyQuests() + 1);
            } else {
                u.setCompletedNonDailyQuests(u.getCompletedNonDailyQuests() + 1);
            }
        });
    }

    void purchaseDailyDeal(String username, String dealId) {
        if (dealId == null) {
            return;
        }
        mutate(username, u -> {
            if (u.getPurchasedDailyDeals() == null) {
                u.setPurchasedDailyDeals(new HashMap<>());
            }
            u.getPurchasedDailyDeals().put(dealId, true);
        });
    }

    boolean hasPurchasedDailyDeal(String username, String dealId) {
        if (username == null || dealId == null) {
            return false;
        }
        tables.rwLock.readLock().lock();
        try {
            User u = tables.getByUsername(username);
            return u != null && u.getPurchasedDailyDeals() != null
                    && u.getPurchasedDailyDeals().getOrDefault(dealId, false);
        } finally {
            tables.rwLock.readLock().unlock();
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

    private void mutate(String username, java.util.function.Consumer<User> action) {
        if (username == null) {
            return;
        }
        tables.rwLock.writeLock().lock();
        try {
            User u = tables.getByUsername(username);
            if (u != null) {
                action.accept(u);
                files.flushInternal();
            }
        } finally {
            tables.rwLock.writeLock().unlock();
        }
    }

    private boolean mutateIf(
            String username,
            java.util.function.Predicate<User> predicate,
            java.util.function.Consumer<User> action
    ) {
        if (username == null) {
            return false;
        }
        tables.rwLock.writeLock().lock();
        try {
            User u = tables.getByUsername(username);
            if (u != null && predicate.test(u)) {
                action.accept(u);
                files.flushInternal();
                return true;
            }
            return false;
        } finally {
            tables.rwLock.writeLock().unlock();
        }
    }
}
