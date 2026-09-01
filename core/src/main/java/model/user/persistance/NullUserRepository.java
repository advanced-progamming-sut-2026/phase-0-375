package model.user.persistance;

import model.app.App;
import model.enums.Chapter;
import model.user.User;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Client-side placeholder that never touches disk. Accounts live on the server;
 * after login the app swaps in {@link RemoteUserRepository}.
 */
public final class NullUserRepository implements UserRepository {

    @Override
    public void save(User user) {}

    @Override
    public void delete(User user) {}

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        return Collections.emptyList();
    }

    @Override
    public boolean existsByUsername(String username) {
        return false;
    }

    @Override
    public boolean existsByEmail(String email) {
        return false;
    }

    @Override
    public Optional<User> authenticate(String username, String passwordHash) {
        return Optional.empty();
    }

    @Override
    public Optional<User> findStayLoggedInUser() {
        return Optional.empty();
    }

    @Override
    public void clearStayLoggedIn(String username) {}

    @Override
    public boolean verifySecurityAnswer(String username, String answer) {
        return false;
    }

    @Override
    public void updatePassword(String username, String newPasswordHash) {}

    @Override
    public void addCoins(String username, int amount) {
        withCurrentUser(username, u -> u.setCoins(u.getCoins() + amount));
    }

    @Override
    public boolean spendCoins(String username, int amount) {
        return false;
    }

    @Override
    public void addGems(String username, int amount) {
        withCurrentUser(username, u -> u.setGems(u.getGems() + amount));
    }

    @Override
    public boolean spendGems(String username, int amount) {
        return false;
    }

    @Override
    public void unlockPlant(String username, String plantName) {}

    @Override
    public void unlockZombie(String username, String zombieName) {}

    @Override
    public void updateChapterProgress(String username, Chapter chapter, int level) {}

    @Override
    public void updateHighestMyopoint(String username, int myopoint) {}

    @Override
    public void incrementGamesPlayed(String username) {}

    @Override
    public void unlockMiniGame(String username, String miniGameId) {}

    @Override
    public void updateDifficulty(String username, int difficultyLevel) {}

    @Override
    public void addSeedPackets(String username, String plantName, int count) {}

    @Override
    public boolean spendSeedPackets(String username, String plantName, int count) {
        return false;
    }

    @Override
    public boolean addPlantFood(String username) {
        return false;
    }

    @Override
    public boolean usePlantFood(String username) {
        return false;
    }

    @Override
    public void storePlantBoost(String username, String plantName) {}

    @Override
    public boolean consumePlantBoost(String username, String plantName) {
        return false;
    }

    @Override
    public void unlockGreenhousePot(String username, int x, int y) {}

    @Override
    public void plantInGreenhouse(String username, int x, int y, String plantName, long timestamp) {}

    @Override
    public void harvestGreenhousePlant(String username, int x, int y) {}

    @Override
    public void markNewsAsRead(String username, String newsId) {}

    @Override
    public void completeQuest(String username, String questId, boolean isDaily) {
        withCurrentUser(username, u -> {
            if (u.getQuestStatus() == null) {
                u.setQuestStatus(new java.util.HashMap<>());
            }
            u.getQuestStatus().put(questId, true);
            if (isDaily) {
                u.setCompletedDailyQuests(u.getCompletedDailyQuests() + 1);
            } else {
                u.setCompletedNonDailyQuests(u.getCompletedNonDailyQuests() + 1);
            }
        });
    }

    private static void withCurrentUser(String username, java.util.function.Consumer<User> action) {
        User current = App.getInstance().getCurrentUser();
        if (current == null || username == null || !username.equals(current.getUsername())) {
            return;
        }
        action.accept(current);
    }

    @Override
    public void purchaseDailyDeal(String username, String dealId) {}

    @Override
    public boolean hasPurchasedDailyDeal(String username, String dealId) {
        return false;
    }

    @Override
    public List<User> findAllOrderByMyopointDesc() {
        return Collections.emptyList();
    }

    @Override
    public List<User> findAllOrderByChapterProgressDesc() {
        return Collections.emptyList();
    }

    @Override
    public List<User> findAllOrderByMiniGamesDesc() {
        return Collections.emptyList();
    }

    @Override
    public List<User> findAllOrderByCompletedQuestsDesc() {
        return Collections.emptyList();
    }

    @Override
    public void loadAll() {}

    @Override
    public void flush() {}
}
