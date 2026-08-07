package model.user.persistance;

import model.enums.Chapter;
import model.user.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    void save(User user);

    void delete(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> authenticate(String username, String passwordHash);

    Optional<User> findStayLoggedInUser();

    void clearStayLoggedIn(String username);

    boolean verifySecurityAnswer(String username, String answer);

    void updatePassword(String username, String newPasswordHash);
    
    void addCoins(String username, int amount);

    boolean spendCoins(String username, int amount);

    void addGems(String username, int amount);

    boolean spendGems(String username, int amount);

    void unlockPlant(String username, String plantName);

    void unlockZombie(String username, String zombieName);

    void updateChapterProgress(String username, Chapter chapter, int level);

    void updateHighestMyopoint(String username, int myopoint);

    void incrementGamesPlayed(String username);

    void unlockMiniGame(String username, String miniGameId);

    void updateDifficulty(String username, int difficultyLevel);

    void addSeedPackets(String username, String plantName, int count);

    boolean spendSeedPackets(String username, String plantName, int count);

    boolean addPlantFood(String username);

    boolean usePlantFood(String username);

    void storePlantBoost(String username, String plantName);

    boolean consumePlantBoost(String username, String plantName);

    void unlockGreenhousePot(String username, int x, int y);

    void plantInGreenhouse(String username, int x, int y, String plantName, long timestamp);

    void harvestGreenhousePlant(String username, int x, int y);

    void markNewsAsRead(String username, String newsId);

    void completeQuest(String username, String questId, boolean isDaily);

    void purchaseDailyDeal(String username, String dealId);

    boolean hasPurchasedDailyDeal(String username, String dealId);

    List<User> findAllOrderByMyopointDesc();

    List<User> findAllOrderByChapterProgressDesc();

    List<User> findAllOrderByMiniGamesDesc();

    List<User> findAllOrderByCompletedQuestsDesc();

    void loadAll();

    void flush();
}

