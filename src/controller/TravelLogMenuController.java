package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.enums.QuestCategory;
import model.enums.QuestPriority;
import model.quest.Quest;
import model.quest.QuestProgress;
import model.quest.QuestReward;
import model.quest.TravelLog;
import model.user.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TravelLogMenuController extends AppMenuController {
    private static TravelLogMenuController instance = null;

    private final TravelLog travelLog;

    private TravelLogMenuController() {
        this.travelLog = new TravelLog();

        travelLog.setQuests(buildStarterQuests());
    }

    public static TravelLogMenuController getInstance() {
        if (instance == null) instance = new TravelLogMenuController();
        return instance;
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
            return CommandResult.error("Page name cannot be empty. Use: daily | main | epic");
        }
        QuestCategory target;
        try {
            target = QuestCategory.valueOf(pageName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CommandResult.error("Unknown page '" + pageName
                    + "'. Available pages: daily, main, epic.");
        }
        travelLog.setCurrentPage(target);
        return CommandResult.success("Switched to '" + target.name().toLowerCase() + "' page.");
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

    public CommandResult<Void> showQuestProgress(String questName) {
        Quest q = findQuest(questName);
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
                + "\n  Reward: " + describeReward(q.getReward());
        return CommandResult.success(info, null);
    }

    public CommandResult<Void> completeQuest(String questName) {
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

        grantReward(q.getReward());
        travelLog.completeQuest(q);
        // Bookkeeping on the user
        User user = App.getInstance().getCurrentUser();
        if (user != null) {
            if (q.getCategory() == QuestCategory.DAILY) {
                user.setCompletedDailyQuests(user.getCompletedDailyQuests() + 1);
            } else {
                user.setCompletedNonDailyQuests(user.getCompletedNonDailyQuests() + 1);
            }
            App.getInstance().getUserRepository().flush();
        }
        return CommandResult.success("Quest '" + questName + "' completed! Reward granted.");
    }

    // Helpers

    private Quest findQuest(String name) {
        if (name == null) return null;
        for (Quest q : travelLog.getQuests()) {
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
            sb.append("unlock: ").append(r.getUnlockableName());
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? "(none)" : s;
    }

    private void grantReward(QuestReward reward) {
        if (reward == null) return;
        User user = App.getInstance().getCurrentUser();
        if (user == null) return;
        if (reward.getCoinAmount() > 0) {
            user.setCoins(user.getCoins() + reward.getCoinAmount());
        }
        if (reward.getGemAmount() > 0) {
            user.setGems(user.getGems() + reward.getGemAmount());
        }
        if (reward.getInventoryItem() != null && !reward.getInventoryItem().isBlank()
                && reward.getInventoryItemAmount() > 0
                && user.getSeedPackets() != null) {
            int current = user.getSeedPackets().getOrDefault(reward.getInventoryItem(), 0);
            user.getSeedPackets().put(reward.getInventoryItem(),
                    current + reward.getInventoryItemAmount());
        }
        if (reward.getUnlockableName() != null && !reward.getUnlockableName().isBlank()
                && user.getUnlockedPlants() != null) {
            user.getUnlockedPlants().add(reward.getUnlockableName());
        }
    }

    /**
     * TODO: replace with QuestRegistry.getInstance().loadAll() once merged.
     */
    private List<Quest> buildStarterQuests() {
        List<Quest> quests = new ArrayList<>();
        // Daily quests
        quests.add(new Quest(
                "Sun Harvester",
                QuestCategory.DAILY,
                "Collect 500 sun in a single level",
                new QuestReward(model.enums.QuestRewardType.CURRENCY, 100, 0,
                        null, null, 0),
                QuestPriority.HIGH,
                null,
                new QuestProgress(0, 500)
        ));
        quests.add(new Quest(
                "Zombie Slayer",
                QuestCategory.DAILY,
                "Defeat 20 zombies",
                new QuestReward(model.enums.QuestRewardType.CURRENCY, 150, 0,
                        null, null, 0),
                QuestPriority.MEDIUM,
                null,
                new QuestProgress(0, 20)
        ));
        // Main quests
        quests.add(new Quest(
                "First Victory",
                QuestCategory.MAIN,
                "Complete your first level",
                new QuestReward(model.enums.QuestRewardType.UNLOCKABLE, 0, 0,
                        "Potato Mine", null, 0),
                QuestPriority.CRITICAL,
                null,
                new QuestProgress(0, 1)
        ));
        quests.add(new Quest(
                "Garden Keeper",
                QuestCategory.MAIN,
                "Harvest 5 plants from the greenhouse",
                new QuestReward(model.enums.QuestRewardType.CURRENCY, 250, 1,
                        null, null, 0),
                QuestPriority.HIGH,
                null,
                new QuestProgress(0, 5)
        ));
        // Epic quests
        quests.add(new Quest(
                "Egypt Explorer",
                QuestCategory.EPIC,
                "Complete all Ancient Egypt levels",
                new QuestReward(model.enums.QuestRewardType.UNLOCKABLE, 0, 5,
                        "Snow Pea", null, 0),
                QuestPriority.HIGH,
                null,
                new QuestProgress(0, 10)
        ));
        return quests;
    }
}