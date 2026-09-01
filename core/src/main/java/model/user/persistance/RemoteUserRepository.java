package model.user.persistance;

import model.app.App;
import model.enums.Chapter;
import model.network.client.NetworkClient;
import model.network.enums.UserCommand;
import model.network.packet.user.UserCommandRequestPacket;
import model.network.packet.user.UserCommandResponsePacket;
import model.user.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Server-backed {@link UserRepository}. Mutations are command packets; {@link #flush()} is a no-op.
 * Account CRUD/auth methods are unsupported (use Login/Register controllers).
 */
public class RemoteUserRepository implements UserRepository {

    private static final int TIMEOUT_MS = 3000;

    private final NetworkClient client;

    public RemoteUserRepository(NetworkClient client) {
        this.client = client;
    }

    private String currentUsername() {
        User u = App.getInstance().getCurrentUser();
        return u != null ? u.getUsername() : null;
    }

    private boolean execute(UserCommand command, Map<String, String> args) {
        if (client == null || !client.isConnected()) {
            return false;
        }
        UserCommandRequestPacket req = new UserCommandRequestPacket(command, args);
        AtomicReference<UserCommandResponsePacket> responseRef = new AtomicReference<>(null);
        Consumer<UserCommandResponsePacket> handler = resp -> {
            if (resp != null && req.getClientRequestId() != null
                    && req.getClientRequestId().equals(resp.getClientRequestId())) {
                responseRef.set(resp);
            }
        };

        boolean prevAutoPost = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(UserCommandResponsePacket.class, handler);
        try {
            if (!client.sendPacket(req)) {
                return false;
            }
            long deadline = System.currentTimeMillis() + TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline && responseRef.get() == null && client.isConnected()) {
                client.pollEvents();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            client.unregisterHandler(UserCommandResponsePacket.class, handler);
            client.setAutoPostToGdx(prevAutoPost);
        }

        UserCommandResponsePacket resp = responseRef.get();
        if (resp == null || !resp.isSuccess()) {
            return false;
        }
        applyProfile(resp.getUser());
        return true;
    }

    private void applyProfile(User serverUser) {
        if (serverUser == null) return;
        User local = App.getInstance().getCurrentUser();
        if (local != null && local.getPasswordHash() != null && serverUser.getPasswordHash() == null) {
            serverUser.setPasswordHash(local.getPasswordHash());
        }
        if (local != null) {
            serverUser.setStayLoggedIn(local.isStayLoggedIn());
        }
        App.getInstance().setCurrentUser(serverUser);
    }

    private static Map<String, String> args(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i] != null && kv[i + 1] != null) {
                m.put(kv[i], kv[i + 1]);
            }
        }
        return m;
    }

    @Override
    public void save(User user) {
        // Profile identity fields go through ProfileUpdate packets; ignore silent saves.
    }

    @Override
    public void delete(User user) {
        // Unsupported on client.
    }

    @Override
    public Optional<User> findByUsername(String username) {
        User current = App.getInstance().getCurrentUser();
        if (current != null && current.getUsername() != null && current.getUsername().equals(username)) {
            return Optional.of(current);
        }
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
    public void clearStayLoggedIn(String username) {
        User u = App.getInstance().getCurrentUser();
        if (u != null) u.setStayLoggedIn(false);
    }

    @Override
    public boolean verifySecurityAnswer(String username, String answer) {
        return false;
    }

    @Override
    public void updatePassword(String username, String newPasswordHash) {
        // Use PasswordChangeRequestPacket via ProfileMenuController.
    }

    @Override
    public void addCoins(String username, int amount) {
        execute(UserCommand.ADD_COINS, args("amount", String.valueOf(amount)));
    }

    @Override
    public boolean spendCoins(String username, int amount) {
        return execute(UserCommand.SPEND_COINS, args("amount", String.valueOf(amount)));
    }

    @Override
    public void addGems(String username, int amount) {
        execute(UserCommand.ADD_GEMS, args("amount", String.valueOf(amount)));
    }

    @Override
    public boolean spendGems(String username, int amount) {
        return execute(UserCommand.SPEND_GEMS, args("amount", String.valueOf(amount)));
    }

    @Override
    public void unlockPlant(String username, String plantName) {
        execute(UserCommand.UNLOCK_PLANT, args("plantName", plantName));
    }

    @Override
    public void unlockZombie(String username, String zombieName) {
        execute(UserCommand.UNLOCK_ZOMBIE, args("zombieName", zombieName));
    }

    @Override
    public void updateChapterProgress(String username, Chapter chapter, int level) {
        execute(UserCommand.UPDATE_CHAPTER_PROGRESS, args(
                "chapter", chapter != null ? chapter.name() : null,
                "level", String.valueOf(level)));
    }

    @Override
    public void updateHighestMyopoint(String username, int myopoint) {
        execute(UserCommand.UPDATE_HIGHEST_MYOPOINT, args("myopoint", String.valueOf(myopoint)));
    }

    @Override
    public void incrementGamesPlayed(String username) {
        execute(UserCommand.INCREMENT_GAMES_PLAYED, args());
    }

    @Override
    public void unlockMiniGame(String username, String miniGameId) {
        execute(UserCommand.UNLOCK_MINI_GAME, args("miniGameId", miniGameId));
    }

    @Override
    public void updateDifficulty(String username, int difficultyLevel) {
        execute(UserCommand.UPDATE_DIFFICULTY, args("difficultyLevel", String.valueOf(difficultyLevel)));
    }

    @Override
    public void addSeedPackets(String username, String plantName, int count) {
        execute(UserCommand.ADD_SEED_PACKETS, args("plantName", plantName, "count", String.valueOf(count)));
    }

    @Override
    public boolean spendSeedPackets(String username, String plantName, int count) {
        return execute(UserCommand.SPEND_SEED_PACKETS, args("plantName", plantName, "count", String.valueOf(count)));
    }

    @Override
    public boolean addPlantFood(String username) {
        return execute(UserCommand.ADD_PLANT_FOOD, args());
    }

    @Override
    public boolean usePlantFood(String username) {
        return execute(UserCommand.USE_PLANT_FOOD, args());
    }

    @Override
    public void storePlantBoost(String username, String plantName) {
        execute(UserCommand.STORE_PLANT_BOOST, args("plantName", plantName));
    }

    @Override
    public boolean consumePlantBoost(String username, String plantName) {
        return execute(UserCommand.CONSUME_PLANT_BOOST, args("plantName", plantName));
    }

    @Override
    public void unlockGreenhousePot(String username, int x, int y) {
        execute(UserCommand.UNLOCK_GREENHOUSE_POT, args("x", String.valueOf(x), "y", String.valueOf(y)));
    }

    @Override
    public void plantInGreenhouse(String username, int x, int y, String plantName, long timestamp) {
        execute(UserCommand.PLANT_IN_GREENHOUSE, args(
                "x", String.valueOf(x), "y", String.valueOf(y),
                "plantName", plantName, "timestamp", String.valueOf(timestamp)));
    }

    @Override
    public void harvestGreenhousePlant(String username, int x, int y) {
        execute(UserCommand.HARVEST_GREENHOUSE, args("x", String.valueOf(x), "y", String.valueOf(y)));
    }

    @Override
    public void markNewsAsRead(String username, String newsId) {
        execute(UserCommand.MARK_NEWS_READ, args("newsId", newsId));
    }

    @Override
    public void completeQuest(String username, String questId, boolean isDaily) {
        execute(UserCommand.COMPLETE_QUEST, args("questId", questId, "isDaily", String.valueOf(isDaily)));
    }

    @Override
    public void purchaseDailyDeal(String username, String dealId) {
        execute(UserCommand.PURCHASE_DAILY_DEAL, args("dealId", dealId));
    }

    @Override
    public boolean hasPurchasedDailyDeal(String username, String dealId) {
        User u = App.getInstance().getCurrentUser();
        return u != null && u.getPurchasedDailyDeals() != null
                && Boolean.TRUE.equals(u.getPurchasedDailyDeals().get(dealId));
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
    public void loadAll() {
        // no-op
    }

    @Override
    public void flush() {
        // Mutations already persisted per command.
    }

    /** High-level shop purchase (server runs Shop rules). */
    public boolean purchaseShopItem(int itemId, int count, String plantType) {
        return execute(UserCommand.PURCHASE_SHOP_ITEM, args(
                "itemId", String.valueOf(itemId),
                "count", String.valueOf(count),
                "plantType", plantType));
    }

    public boolean setSettings(Map<String, String> settings) {
        return execute(UserCommand.SET_SETTINGS, settings);
    }

    public boolean fetchDailyOffer() {
        return execute(UserCommand.GET_DAILY_OFFER, Map.of());
    }

    public boolean setQuestProgress(String questId, int value, String dailyRefreshDate) {
        Map<String, String> m = args("questId", questId, "value", String.valueOf(value));
        if (dailyRefreshDate != null) {
            m.put("dailyQuestRefreshDate", dailyRefreshDate);
        }
        return execute(UserCommand.SET_QUEST_PROGRESS, m);
    }

    public boolean setPlantLevel(String plantName, int level) {
        return execute(UserCommand.SET_PLANT_LEVEL, args("plantName", plantName, "level", String.valueOf(level)));
    }
}
