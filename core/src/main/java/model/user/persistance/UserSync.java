package model.user.persistance;

import model.app.App;
import model.user.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Helpers that prefer {@link RemoteUserRepository} commands when online.
 */
public final class UserSync {
    private UserSync() {}

    public static UserRepository repo() {
        return App.getInstance().getUserRepository();
    }

    public static String username() {
        User u = App.getInstance().getCurrentUser();
        return u != null ? u.getUsername() : null;
    }

    public static void addCoins(int amount) {
        String u = username();
        if (u != null && repo() != null) repo().addCoins(u, amount);
    }

    public static void addGems(int amount) {
        String u = username();
        if (u != null && repo() != null) repo().addGems(u, amount);
    }

    public static boolean spendCoins(int amount) {
        String u = username();
        return u != null && repo() != null && repo().spendCoins(u, amount);
    }

    public static boolean spendGems(int amount) {
        String u = username();
        return u != null && repo() != null && repo().spendGems(u, amount);
    }

    public static boolean purchaseShopItem(int itemId, int count, String plantType) {
        UserRepository r = repo();
        if (r instanceof RemoteUserRepository remote) {
            return remote.purchaseShopItem(itemId, count, plantType);
        }
        return false;
    }

    public static void persistSettingsFromCurrentUser() {
        User user = App.getInstance().getCurrentUser();
        UserRepository r = repo();
        if (user == null || r == null) return;
        if (r instanceof RemoteUserRepository remote) {
            Map<String, String> m = new HashMap<>();
            m.put("difficultyLevel", String.valueOf(user.getDifficultyLevel()));
            m.put("gameSpeed", String.valueOf(user.getGameSpeed()));
            m.put("showLawnGrid", String.valueOf(user.isShowLawnGrid()));
            m.put("debugMode", String.valueOf(user.isDebugMode()));
            m.put("musicVolume", String.valueOf(user.getMusicVolume()));
            m.put("sfxVolume", String.valueOf(user.getSfxVolume()));
            remote.setSettings(m);
        } else {
            r.flush();
        }
    }

    public static void syncDailyOfferFromServer() {
        UserRepository r = repo();
        if (r instanceof RemoteUserRepository remote) {
            remote.fetchDailyOffer();
        }
    }

    public static void persistQuestProgressFromCurrentUser() {
        User user = App.getInstance().getCurrentUser();
        UserRepository r = repo();
        if (user == null || r == null) return;
        if (r instanceof RemoteUserRepository remote) {
            Map<String, Integer> progress = user.getQuestProgress();
            if (progress != null) {
                for (Map.Entry<String, Integer> e : progress.entrySet()) {
                    remote.setQuestProgress(e.getKey(), e.getValue(), user.getDailyQuestRefreshDate());
                }
            } else {
                remote.setQuestProgress("_noop_", 0, user.getDailyQuestRefreshDate());
            }
        } else {
            r.flush();
        }
    }

    /** No-op when remote (commands already saved); flush when local test repo. */
    public static void flushIfLocal() {
        UserRepository r = repo();
        if (r != null && !(r instanceof RemoteUserRepository)) {
            r.flush();
        }
    }
}
