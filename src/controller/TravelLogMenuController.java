package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.enums.MiniGameType;
import model.enums.QuestCategory;
import model.enums.QuestPriority;
import model.quest.Quest;
import model.quest.QuestProgress;
import model.quest.QuestReward;
import model.quest.TravelLog;
import model.user.User;
import model.data.quest.QuestLoader;
import model.data.minigame.MiniGameDataEntry;
import model.data.minigame.MiniGameRegistry;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.level.minigame.MiniGameLevel;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TravelLogMenuController extends AppMenuController {
    private static final String QUESTS_JSON = "/assets/data/quests/quests.json";

    private static TravelLogMenuController instance = null;

    private final TravelLog travelLog;
    private String ownerUsername; // whose quests are currently loaded

    private TravelLogMenuController() {
        this.travelLog = new TravelLog();
        travelLog.setQuests(loadQuests());
    }

    private List<Quest> loadQuests() {
        try {
            return new ArrayList<>(new QuestLoader().load(QUESTS_JSON));
        } catch (IOException e) {
            System.err.println("[TravelLogMenuController] Failed to load quests.json: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static TravelLogMenuController getInstance() {
        if (instance == null) instance = new TravelLogMenuController();
        return instance;
    }

    /**
     * Reloads quests when the logged-in user changes and refreshes
     * daily quests once per day. Called before handling any command.
     */
    public void syncForCurrentUser() {
        User user = App.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        boolean userChanged = !Objects.equals(user.getUsername(), ownerUsername);
        if (userChanged) {
            ownerUsername = user.getUsername();
        }

        String today = LocalDate.now().toString();
        boolean newDay = !today.equals(user.getDailyQuestRefreshDate());
        if (newDay) {
            // completed dailies become available again on a new day;
            // also drop stale daily variants (any day's value) from status/progress
            Set<String> dailyBases = new HashSet<>();
            for (Quest q : loadQuests()) {
                if (q.getCategory() == QuestCategory.DAILY) {
                    dailyBases.add(baseName(q.getName()));
                }
            }
            removeDailyEntries(user.getQuestStatus(), dailyBases);
            removeDailyEntries(user.getQuestProgress(), dailyBases);
            user.setDailyQuestRefreshDate(today);
            App.getInstance().getUserRepository().flush();
        }

        // always rebuild so progress saved at level end shows up immediately
        reloadForUser(user);
    }

    /** Rebuilds active/completed lists from quests.json and the user's saved status. */
    private void reloadForUser(User user) {
        Map<String, Boolean> status = user.getQuestStatus();
        List<Quest> active = new ArrayList<>();
        List<Quest> completed = new ArrayList<>();
        Map<String, Integer> savedProgress = user.getQuestProgress();
        for (Quest q : loadQuests()) {
            boolean done = status != null && Boolean.TRUE.equals(status.get(q.getName()));
            if (done) {
                completed.add(q);
            } else {
                applySavedProgress(q, savedProgress);
                active.add(q);
            }
        }
        travelLog.setQuests(active);
        travelLog.setCompletedQuests(completed);
    }

    /** Applies progress persisted by QuestTracker to a freshly loaded quest. */
    private static void applySavedProgress(Quest q, Map<String, Integer> saved) {
        if (saved == null || q.getProgress() == null) return;
        Integer value = saved.get(q.getName());
        if (value != null && value > 0) {
            q.getProgress().setCurrentValue(Math.min(value, q.getProgress().getTargetValue()));
        }
    }

    /** "One Column Less (4)" -> "One Column Less". */
    private static String baseName(String questName) {
        int idx = questName.indexOf(" (");
        return idx > 0 ? questName.substring(0, idx) : questName;
    }

    private static void removeDailyEntries(Map<String, ?> map, Set<String> dailyBases) {
        if (map == null) return;
        map.keySet().removeIf(key -> dailyBases.contains(baseName(key)));
    }

    // Menu navigation

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return CommandResult.error("No menus reachable from travel log. Use 'menu exit' to return to the game menu.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.GAME);
        return CommandResult.success("Returned to game menu.");
    }

    // Travel log commands

    public CommandResult<Void> changePage(String pageName) {
        if (pageName == null || pageName.isBlank()) {
            return CommandResult.error("Page name cannot be empty. Use: daily | main | epic | minigame");
        }
        String trimmed = pageName.trim().toLowerCase(Locale.ROOT);
        if (trimmed.equals("minigame") || trimmed.equals("mini-game") || trimmed.equals("mini_game")) {
            travelLog.setViewingMiniGamePage(true);
            return CommandResult.success("Switched to 'minigame' page.");
        }
        // Leaving the mini-game page: re-affirm a quest category.
        travelLog.setViewingMiniGamePage(false);
        QuestCategory target;
        try {
            target = QuestCategory.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return CommandResult.error("Unknown page '" + pageName
                    + "'. Available pages: daily, main, epic, minigame.");
        }
        travelLog.setCurrentPage(target);
        return CommandResult.success("Switched to '" + target.name().toLowerCase(Locale.ROOT) + "' page.");
    }

    public CommandResult<List<Quest>> showCurrentPage() {
        List<Quest> quests = travelLog.showCurrentPage();
        if (quests.isEmpty()) {
            // Fall back to filtering the master list ourselves
            quests = filterByCategory(travelLog.getQuests(), travelLog.getCurrentPage());
        }
        quests = sortByPriority(quests);
        return CommandResult.successWithData(
                "Page '" + travelLog.getCurrentPage().name().toLowerCase()
                        + "' — " + quests.size() + " quest(s).",
                quests);
    }

    public CommandResult<List<Quest>> showDailyQuests() {
        List<Quest> quests = sortByPriority(filterByCategory(
                travelLog.getQuests(), QuestCategory.DAILY));
        return CommandResult.successWithData(
                "Daily quests (" + quests.size() + "):", quests);
    }

    public CommandResult<List<Quest>> showMainQuests() {
        List<Quest> quests = sortByPriority(filterByCategory(
                travelLog.getQuests(), QuestCategory.MAIN));
        return CommandResult.successWithData(
                "Main quests (" + quests.size() + "):", quests);
    }

    public CommandResult<List<Quest>> showEpicQuests() {
        List<Quest> quests = sortByPriority(filterByCategory(
                travelLog.getQuests(), QuestCategory.EPIC));
        return CommandResult.successWithData(
                "Epic quests (" + quests.size() + "):", quests);
    }

    public CommandResult<List<Quest>> showAllQuests() {
        List<Quest> quests = sortByPriority(new ArrayList<>(travelLog.getQuests()));
        return CommandResult.successWithData(
                "All quests (" + quests.size() + "):", quests);
    }

    public CommandResult<List<Quest>> showCompletedQuests() {
        List<Quest> quests = sortByPriority(new ArrayList<>(travelLog.getCompletedQuests()));
        return CommandResult.successWithData(
                "Completed quests (" + quests.size() + "):", quests);
    }

    public CommandResult<Void> showQuestProgress(String questName) {
        boolean completed = false;
        Quest q = findQuest(questName);
        if (q == null) {
            q = findIn(travelLog.getCompletedQuests(), questName);
            completed = q != null;
        }
        if (q == null) {
            return CommandResult.error("No quest named '" + questName + "'.");
        }
        QuestProgress p = q.getProgress();
        String info = "Quest: " + q.getName() + "\n"
                + "  Category: " + q.getCategory().name().toLowerCase() + "\n"
                + "  Priority: " + q.getPriority().name().toLowerCase() + "\n"
                + "  Condition: " + q.getConditionDescription() + "\n"
                + "  Progress: "
                + (p == null ? "(no progress tracked)"
                : p.getCurrentValue() + " / " + p.getTargetValue())
                + "\n  Reward: " + describeReward(q.getReward())
                + (completed ? "\n  Status: completed" : "");
        return CommandResult.success(info, null);
    }

    public CommandResult<Void> completeQuest(String questName) {
        Quest done = findIn(travelLog.getCompletedQuests(), questName);
        if (done != null) {
            return CommandResult.error("Quest '" + done.getName() + "' is already completed.");
        }
        Quest q = findQuest(questName);
        if (q == null) {
            return CommandResult.error("No quest named '" + questName + "'.");
        }
        if (!q.checkCompletion()) {
            QuestProgress p = q.getProgress();
            String remaining = (p == null)
                    ? "no progress tracked"
                    : (p.getCurrentValue() + " / " + p.getTargetValue());
            return CommandResult.error("Quest '" + questName
                    + "' is not complete yet. Progress: " + remaining);
        }

        travelLog.completeQuest(q);
        // Bookkeeping on the user
        User user = App.getInstance().getCurrentUser();
        if (user != null) {
            if (q.getCategory() == QuestCategory.DAILY) {
                user.setCompletedDailyQuests(user.getCompletedDailyQuests() + 1);
            } else {
                user.setCompletedNonDailyQuests(user.getCompletedNonDailyQuests() + 1);
            }
            if (user.getQuestStatus() == null) {
                user.setQuestStatus(new HashMap<>());
            }
            user.getQuestStatus().put(q.getName(), true);
            if (user.getQuestProgress() != null) {
                user.getQuestProgress().remove(q.getName());
            }
            App.getInstance().getUserRepository().flush();
        }
        String note = "";
        QuestReward reward = q.getReward();
        if (reward != null && reward.getLastSeedPacketPlant() != null) {
            note += " Seed packets granted for '" + reward.getLastSeedPacketPlant() + "'.";
        }
        if (reward != null && reward.getLastUnlockedPlant() != null) {
            note += " New plant unlocked: '" + reward.getLastUnlockedPlant() + "'!";
        }
        return CommandResult.success("Quest '" + questName + "' completed! Reward granted." + note);
    }

    public boolean isViewingMiniGamePage() {
        return travelLog.isViewingMiniGamePage();
    }

    public CommandResult<List<MiniGameDataEntry>> showMiniGames() {
        travelLog.setViewingMiniGamePage(true);
        MiniGameRegistry registry;
        try {
            registry = MiniGameRegistry.getInstance();
        } catch (IllegalStateException e) {
            try {
                MiniGameRegistry.init("/assets/data/minigames/minigames.json");
                registry = MiniGameRegistry.getInstance();
            } catch (IOException | RuntimeException loadError) {
                return CommandResult.errorTyped("Could not load mini-game definitions: " + loadError.getMessage());
            }
        }
        List<MiniGameDataEntry> entries = registry.getAllEntries();
        return CommandResult.successWithData(
                "Mini-games (" + entries.size() + "):", entries);
    }

    public CommandResult<Void> enterMiniGame(String typeName, int stage) {
        MiniGameType type;
        try {
            type = MiniGameType.valueOf(typeName.toUpperCase(Locale.ROOT)
                    .replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return CommandResult.error("Unknown mini-game: '" + typeName + "'.");
        }

        MiniGameRegistry registry;
        try {
            registry = MiniGameRegistry.getInstance();
        } catch (IllegalStateException e) {
            try {
                MiniGameRegistry.init("/assets/data/minigames/minigames.json");
                registry = MiniGameRegistry.getInstance();
            } catch (IOException | RuntimeException loadError) {
                return CommandResult.error("Could not load mini-game definitions: " + loadError.getMessage());
            }
        }

        MiniGameLevel level;
        try {
            level = registry.createMiniGame(type, stage);
        } catch (IOException | RuntimeException buildError) {
            return CommandResult.error("Could not build mini-game " + type + ": " + buildError.getMessage());
        }
        if (level == null) {
            return CommandResult.error("No definition found for " + type + " stage " + stage + ".");
        }

        if (!level.canStart()) {
            return CommandResult.error(type + " stage " + stage + " cannot be started yet.");
        }

        GameModel model = new GameModel(level);
        PvZGameLoop loop = new PvZGameLoop(model);

        App.getInstance().setCurrentGameModel(model);
        App.getInstance().setCurrentGameLoop(loop);

        level.onStart();
        App.getInstance().setCurrentMenu(MenuType.PLANT_SELECTION);

        return CommandResult.success("Entering " + type + " stage " + stage + ".");
    }

    // Helpers

    private Quest findQuest(String name) {
        return findIn(travelLog.getQuests(), name);
    }

    private Quest findIn(List<Quest> quests, String name) {
        if (name == null) return null;
        for (Quest q : quests) {
            if (q.getName().equalsIgnoreCase(name.trim())) {
                return q;
            }
        }
        return null;
    }

    private List<Quest> filterByCategory(List<Quest> source, QuestCategory cat) {
        List<Quest> out = new ArrayList<>();
        for (Quest q : source) {
            if (q.getCategory() == cat) out.add(q);
        }
        return out;
    }

    private List<Quest> sortByPriority(List<Quest> source) {
        List<Quest> out = new ArrayList<>(source);
        out.sort(Comparator.comparingInt(q -> priorityRank(q.getPriority())));
        return out;
    }

    private int priorityRank(QuestPriority p) {
        if (p == null) return 99;
        return switch (p) {
            case CRITICAL -> 0;
            case HIGH     -> 1;
            case MEDIUM   -> 2;
            case LOW      -> 3;
        };
    }

    private String describeReward(QuestReward r) {
        if (r == null) return "(none)";
        StringBuilder sb = new StringBuilder();
        if (r.getCoinAmount() > 0)  sb.append(r.getCoinAmount()).append(" coins ");
        if (r.getGemAmount() > 0)   sb.append(r.getGemAmount()).append(" gems ");
        if (r.getInventoryItem() != null && !r.getInventoryItem().isBlank()) {
            sb.append(r.getInventoryItemAmount()).append("x ").append(r.getInventoryItem());
        }
        if (r.getUnlockableName() != null && !r.getUnlockableName().isBlank()) {
            String unlockable = r.getUnlockableName();
            if (unlockable.toLowerCase().startsWith("random")) {
                // Placeholder resolves at claim time to a locked, kill-capable plant.
                sb.append("unlock: a random plant you haven't unlocked yet");
            } else {
                sb.append("unlock: ").append(unlockable);
            }
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? "(none)" : s;
    }

}