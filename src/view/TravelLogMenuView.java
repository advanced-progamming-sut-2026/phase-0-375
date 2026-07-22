package view;

import controller.TravelLogMenuController;
import controller.result.CommandResult;
import model.command.TravelLogMenuCommand;
import model.enums.QuestCategory;
import model.quest.Quest;
import model.quest.QuestProgress;
import model.quest.QuestReward;

import java.util.List;

public class TravelLogMenuView extends AppMenuView {
    private static TravelLogMenuView instance = null;

    public static TravelLogMenuView getInstance() {
        if (instance == null) instance = new TravelLogMenuView();
        return instance;
    }

    private final TravelLogMenuController controller = TravelLogMenuController.getInstance();

    @Override
    public void processInput(String input) {
        if (input == null || input.isBlank()) {
            displayError("Empty command.");
            return;
        }

        // per-user reload + once-a-day daily quest refresh
        controller.syncForCurrentUser();

        if (TravelLogMenuCommand.CHANGE_PAGE.matches(input)) {
            String page = TravelLogMenuCommand.CHANGE_PAGE.getParameter("pageName");
            changePage(page);
            return;
        }
        if (TravelLogMenuCommand.SHOW_CURRENT_PAGE.matches(input)) {
            showCurrentPage();
            return;
        }
        if (TravelLogMenuCommand.SHOW_DAILY_QUESTS.matches(input)) {
            showDailyQuests();
            return;
        }
        if (TravelLogMenuCommand.SHOW_MAIN_QUESTS.matches(input)) {
            showMainQuests();
            return;
        }
        if (TravelLogMenuCommand.SHOW_EPIC_QUESTS.matches(input)) {
            showEpicQuests();
            return;
        }
        if (TravelLogMenuCommand.SHOW_ALL_QUESTS.matches(input)) {
            showAllQuests();
            return;
        }
        if (TravelLogMenuCommand.SHOW_COMPLETED_QUESTS.matches(input)) {
            showCompletedQuests();
            return;
        }
        if (TravelLogMenuCommand.COMPLETE_QUEST.matches(input)) {
            String name = TravelLogMenuCommand.COMPLETE_QUEST.getParameter("questName");
            completeQuest(name);
            return;
        }
        if (TravelLogMenuCommand.SHOW_QUEST_PROGRESS.matches(input)) {
            String name = TravelLogMenuCommand.SHOW_QUEST_PROGRESS.getParameter("questName");
            showQuestProgress(name);
            return;
        }

        displayError("Unknown travel log command. Available commands:");
        displayError("  travel log page <daily|main|epic>");
        displayError("  show current page");
        displayError("  show daily quests");
        displayError("  show main quests");
        displayError("  show epic quests");
        displayError("  show all quests");
        displayError("  show completed quests");
        displayError("  show quest progress -n <quest name>");
        displayError("  complete quest -n <quest name>");
        displayError("  menu exit");
    }

    // Per-command view methods

    public void changePage(String pageName) {
        CommandResult<Void> result = controller.changePage(pageName);
        displayCommandResult(result);
        if (result.isSuccess()) {
            // Eagerly show the new page so the player sees what they switched to.
            showCurrentPage();
        }
    }

    public void showCurrentPage() {
        CommandResult<List<Quest>> result = controller.showCurrentPage();
        if (result.isSuccess()) {
            renderQuestList(result.getMessage(), result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void showDailyQuests() {
        CommandResult<List<Quest>> result = controller.showDailyQuests();
        if (result.isSuccess()) {
            renderQuestList(result.getMessage(), result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void showMainQuests() {
        CommandResult<List<Quest>> result = controller.showMainQuests();
        if (result.isSuccess()) {
            renderQuestList(result.getMessage(), result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void showEpicQuests() {
        CommandResult<List<Quest>> result = controller.showEpicQuests();
        if (result.isSuccess()) {
            renderQuestList(result.getMessage(), result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void showAllQuests() {
        CommandResult<List<Quest>> result = controller.showAllQuests();
        if (result.isSuccess()) {
            renderQuestList(result.getMessage(), result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void showCompletedQuests() {
        CommandResult<List<Quest>> result = controller.showCompletedQuests();
        if (result.isSuccess()) {
            renderQuestList(result.getMessage(), result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void showQuestProgress(String questName) {
        CommandResult<Void> result = controller.showQuestProgress(questName);
        if (result.isSuccess()) {
            displayMessage(result.getMessage());
        } else {
            displayError(result.getMessage());
        }
    }

    public void completeQuest(String questName) {
        CommandResult<Void> result = controller.completeQuest(questName);
        displayCommandResult(result);
    }

    // Rendering helpers

    private void renderQuestList(String header, List<Quest> quests) {
        displayMessage("── " + header + " ──");
        if (quests == null || quests.isEmpty()) {
            displayMessage("  (no quests)");
            return;
        }
        for (Quest q : quests) {
            displayMessage(formatQuestLine(q));
        }
    }

    private String formatQuestLine(Quest q) {
        StringBuilder sb = new StringBuilder();
        sb.append("  [").append(priorityLabel(q.getPriority())).append("] ");
        sb.append(q.getName());
        sb.append(" (").append(categoryLabel(q.getCategory())).append(")");
        sb.append(" — ").append(q.getConditionDescription());

        QuestProgress p = q.getProgress();
        if (p != null) {
            sb.append("  [").append(p.getCurrentValue())
                    .append("/").append(p.getTargetValue()).append("]");
        }

        QuestReward r = q.getReward();
        if (r != null) {
            StringBuilder rb = new StringBuilder();
            if (r.getCoinAmount() > 0) rb.append(r.getCoinAmount()).append("c ");
            if (r.getGemAmount() > 0)  rb.append(r.getGemAmount()).append("g ");
            if (r.getInventoryItem() != null && !r.getInventoryItem().isBlank()) {
                rb.append(r.getInventoryItemAmount()).append("x ")
                        .append(r.getInventoryItem()).append(' ');
            }
            if (r.getUnlockableName() != null && !r.getUnlockableName().isBlank()) {
                String unlockable = r.getUnlockableName();
                if (unlockable.toLowerCase().startsWith("random")) {
                    // Placeholder resolves at claim time to a locked plant.
                    rb.append("unlock: a random new plant");
                } else {
                    rb.append("unlock:").append(unlockable);
                }
            }
            String rewardStr = rb.toString().trim();
            if (!rewardStr.isEmpty()) {
                sb.append("  reward: ").append(rewardStr);
            }
        }
        return sb.toString();
    }

    private String priorityLabel(model.enums.QuestPriority p) {
        if (p == null) return "?";
        return switch (p) {
            case CRITICAL -> "!!!";
            case HIGH     -> "!!";
            case MEDIUM   -> "!";
            case LOW      -> " ";
        };
    }

    private String categoryLabel(QuestCategory c) {
        if (c == null) return "?";
        return c.name().toLowerCase();
    }
}
