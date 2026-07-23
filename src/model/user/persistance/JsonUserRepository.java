package model.user.persistance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import model.enums.Chapter;
import model.plant.PlantFactory;
import model.shop.Shop;
import model.user.User;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class JsonUserRepository implements UserRepository {

    private static final String DATA_DIR = "data";
    private static final String FILE_PATH = DATA_DIR + "/users.json";
    private static final String PLANTS_JSON = "/assets/data/plants/plants.json";

    private final ObjectMapper mapper;
    private List<User> users;

    public JsonUserRepository() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.users = new ArrayList<>();
    }

    @Override
    public void loadAll() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(FILE_PATH);
        if (!file.exists()) {
            users = new ArrayList<>();
            return;
        }
        try {
            users = mapper.readValue(file, new TypeReference<ArrayList<User>>() {});
        } catch (IOException e) {
            users = new ArrayList<>();
        }
        migrateStarterPlants();
    }

    /**
     * Accounts registered before the starter kit existed may miss (some of)
     * the starter plants; merge them in so the shop is usable for everyone.
     * Idempotent: already-unlocked plants are untouched.
     */
    private void migrateStarterPlants() {
        for (User u : users) {
            if (u.getUnlockedPlants() == null) {
                u.setUnlockedPlants(new HashSet<>());
            }
            u.getUnlockedPlants().addAll(User.STARTER_PLANTS);
        }
    }

    @Override
    public void flush() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) dir.mkdirs();

        try {
            mapper.writeValue(new File(FILE_PATH), users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(User user) {
        findByUsername(user.getUsername()).ifPresent(users::remove);
        users.add(user);
        flush();
    }

    @Override
    public void delete(User user) {
        users.remove(user);
        flush();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    @Override
    public boolean existsByUsername(String username) {
        return users.stream().anyMatch(u -> u.getUsername().equals(username));
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.stream().anyMatch(u -> email.equalsIgnoreCase(u.getEmail()));
    }

    @Override
    public Optional<User> authenticate(String username, String passwordHash) {
        return findByUsername(username)
                .filter(u -> u.getPasswordHash().equals(passwordHash));
    }

    @Override
    public Optional<User> findStayLoggedInUser() {
        return users.stream().filter(User::isStayLoggedIn).findFirst();
    }

    @Override
    public void clearStayLoggedIn(String username) {
        findByUsername(username).ifPresent(u -> u.setStayLoggedIn(false));
        flush();
    }

    @Override
    public boolean verifySecurityAnswer(String username, String answer) {
        return findByUsername(username)
                .map(u -> u.getSecurityAnswer() != null && u.getSecurityAnswer().equals(answer))
                .orElse(false);
    }

    @Override
    public void updatePassword(String username, String newPasswordHash) {
        findByUsername(username).ifPresent(u -> u.setPasswordHash(newPasswordHash));
        flush();
    }

    @Override
    public void addCoins(String username, int amount) {
        findByUsername(username).ifPresent(u -> u.setCoins(u.getCoins() + amount));
        flush();
    }

    @Override
    public boolean spendCoins(String username, int amount) {
        Optional<User> opt = findByUsername(username);
        if (opt.isPresent()) {
            User u = opt.get();
            if (u.getCoins() >= amount) {
                u.setCoins(u.getCoins() - amount);
                flush();
                return true;
            }
        }
        return false;
    }

    @Override
    public void addGems(String username, int amount) {
        findByUsername(username).ifPresent(u -> u.setGems(u.getGems() + amount));
        flush();
    }

    @Override
    public boolean spendGems(String username, int amount) {
        Optional<User> opt = findByUsername(username);
        if (opt.isPresent()) {
            User u = opt.get();
            if (u.getGems() >= amount) {
                u.setGems(u.getGems() - amount);
                flush();
                return true;
            }
        }
        return false;
    }

    @Override
    public void unlockPlant(String username, String plantName) {
        findByUsername(username).ifPresent(u -> {
            if (u.getUnlockedPlants() != null) u.getUnlockedPlants().add(plantName);
        });
        flush();
    }

    @Override
    public void unlockZombie(String username, String zombieName) {
        findByUsername(username).ifPresent(u -> {
            if (u.getUnlockedZombies() != null) u.getUnlockedZombies().add(zombieName);
        });
        flush();
    }

    @Override
    public void updateChapterProgress(String username, Chapter chapter, int level) {
        findByUsername(username).ifPresent(u -> {
            if (u.getChapterProgress() != null) u.getChapterProgress().put(chapter, level);
        });
        flush();
    }

    @Override
    public void updateHighestMyopoint(String username, int myopoint) {
        findByUsername(username).ifPresent(u -> {
            if (myopoint > u.getHighestMyopoint()) {
                u.setHighestMyopoint(myopoint);
                flush();
            }
        });
    }

    @Override
    public void incrementGamesPlayed(String username) {
        findByUsername(username).ifPresent(u -> u.setGamesPlayed(u.getGamesPlayed() + 1));
        flush();
    }

    @Override
    public void unlockMiniGame(String username, String miniGameId) {
        findByUsername(username).ifPresent(u -> {
            if (u.getUnlockedMiniGames() != null) u.getUnlockedMiniGames().add(miniGameId);
        });
        flush();
    }

    @Override
    public void updateDifficulty(String username, int difficultyLevel) {
        findByUsername(username).ifPresent(u -> u.setDifficultyLevel(difficultyLevel));
        flush();
    }

    @Override
    public void addSeedPackets(String username, String plantName, int count) {
        findByUsername(username).ifPresent(u -> {
            Map<String, Integer> packets = u.getSeedPackets();
            if (packets != null) {
                packets.put(plantName, packets.getOrDefault(plantName, 0) + count);
            }
        });
        flush();
    }

    @Override
    public boolean spendSeedPackets(String username, String plantName, int count) {
        Optional<User> opt = findByUsername(username);
        if (opt.isPresent()) {
            User u = opt.get();
            Map<String, Integer> packets = u.getSeedPackets();
            if (packets != null) {
                int current = packets.getOrDefault(plantName, 0);
                if (current >= count) {
                    packets.put(plantName, current - count);
                    flush();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns false when the user does not exist or the cap is already reached
     */
    @Override
    public boolean addPlantFood(String username) {
        Optional<User> opt = findByUsername(username);
        if (opt.isEmpty()) {
            return false;
        }
        User u = opt.get();
        int current = Math.max(0, u.getPlantFoodCount());
        if (current >= Shop.MAX_PLANT_FOOD) {
            return false;
        }
        u.setPlantFoodCount(current + 1);
        flush();
        return true;
    }

    @Override
    public boolean usePlantFood(String username) {
        Optional<User> opt = findByUsername(username);
        if (opt.isEmpty()) {
            return false;
        }
        User u = opt.get();
        int current = u.getPlantFoodCount();
        if (current <= 0) {
            if (current < 0) {
                u.setPlantFoodCount(0);
                flush();
            }
            return false;
        }
        u.setPlantFoodCount(current - 1);
        flush();
        return true;
    }

    @Override
    public void storePlantBoost(String username, String plantName) {
        String canonical = resolvePlantName(plantName);
        if (canonical == null) {
            return; // unknown plant — nothing to store
        }
        findByUsername(username).ifPresent(u -> {
            if (u.getPlantBoosts() == null) {
                u.setPlantBoosts(new HashMap<>());
            }
            u.getPlantBoosts().put(canonical, true);
            flush();
        });
    }

    @Override
    public boolean consumePlantBoost(String username, String plantName) {
        String canonical = resolvePlantName(plantName);
        if (canonical == null) {
            return false;
        }
        Optional<User> opt = findByUsername(username);
        if (opt.isPresent()) {
            User u = opt.get();
            Boolean boost = u.getPlantBoosts() != null ? u.getPlantBoosts().get(canonical) : null;
            if (Boolean.TRUE.equals(boost)) {
                u.getPlantBoosts().put(canonical, false);
                flush();
                return true;
            }
        }
        return false;
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
            // Definitions unavailable — treat every name as unknown.
        }
        return null;
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
        findByUsername(username).ifPresent(u -> {
            if (u.getUnlockedPots() < 20) {
                u.setUnlockedPots(u.getUnlockedPots() + 1);
            }
        });
        flush();
    }

    @Override
    public void plantInGreenhouse(String username, int x, int y, String plantName, long timestamp) {
        findByUsername(username).ifPresent(u -> {
            String key = x + "," + y;
            if (u.getGreenhousePots() != null) u.getGreenhousePots().put(key, plantName);
            if (u.getGreenhousePlantTimestamps() != null) u.getGreenhousePlantTimestamps().put(key, timestamp);
        });
        flush();
    }

    @Override
    public void harvestGreenhousePlant(String username, int x, int y) {
        findByUsername(username).ifPresent(u -> {
            String key = x + "," + y;
            if (u.getGreenhousePots() != null) u.getGreenhousePots().remove(key);
            if (u.getGreenhousePlantTimestamps() != null) u.getGreenhousePlantTimestamps().remove(key);
        });
        flush();
    }

    @Override
    public void markNewsAsRead(String username, String newsId) {
        findByUsername(username).ifPresent(u -> {
            if (u.getReadNews() != null) u.getReadNews().add(newsId);
        });
        flush();
    }

    @Override
    public void completeQuest(String username, String questId, boolean isDaily) {
        findByUsername(username).ifPresent(u -> {
            if (u.getQuestStatus() != null) u.getQuestStatus().put(questId, true);
            if (isDaily) {
                u.setCompletedDailyQuests(u.getCompletedDailyQuests() + 1);
            } else {
                u.setCompletedNonDailyQuests(u.getCompletedNonDailyQuests() + 1);
            }
        });
        flush();
    }

    @Override
    public void purchaseDailyDeal(String username, String dealId) {
        findByUsername(username).ifPresent(u -> {
            if (u.getPurchasedDailyDeals() != null) u.getPurchasedDailyDeals().put(dealId, true);
        });
        flush();
    }

    @Override
    public boolean hasPurchasedDailyDeal(String username, String dealId) {
        return findByUsername(username)
                .map(u -> u.getPurchasedDailyDeals() != null && u.getPurchasedDailyDeals().getOrDefault(dealId, false))
                .orElse(false);
    }

    @Override
    public List<User> findAllOrderByMyopointDesc() {
        return users.stream()
                .sorted((a, b) -> Integer.compare(b.getHighestMyopoint(), a.getHighestMyopoint()))
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findAllOrderByChapterProgressDesc() {
        return users.stream()
                .sorted((a, b) -> {
                    int aTotal = a.getChapterProgress() != null
                            ? a.getChapterProgress().values().stream().mapToInt(Integer::intValue).sum() : 0;
                    int bTotal = b.getChapterProgress() != null
                            ? b.getChapterProgress().values().stream().mapToInt(Integer::intValue).sum() : 0;
                    return Integer.compare(bTotal, aTotal);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findAllOrderByMiniGamesDesc() {
        return users.stream()
                .sorted((a, b) -> Integer.compare(b.getCompletedMiniGames(), a.getCompletedMiniGames()))
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findAllOrderByCompletedQuestsDesc() {
        return users.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getCompletedDailyQuests() + b.getCompletedNonDailyQuests(),
                        a.getCompletedDailyQuests() + a.getCompletedNonDailyQuests()))
                .collect(Collectors.toList());
    }
}
