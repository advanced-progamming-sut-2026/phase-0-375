package view;

import controller.TravelLogMenuController;
import controller.result.CommandResult;
import model.command.TravelLogMenuCommand;
import model.data.minigame.MiniGameDataEntry;
import model.enums.MiniGameType;
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

        if (dispatchQuestCommand(input)) return;
        if (dispatchMiniGameCommand(input)) return;

        printHelp();
    }

    /** Handles the quest-related travel log commands; returns true if a command matched. */
    private boolean dispatchQuestCommand(String input) {
        if (TravelLogMenuCommand.CHANGE_PAGE.matches(input)) {
            String page = TravelLogMenuCommand.CHANGE_PAGE.getParameter("pageName");
            changePage(page);
            return true;
        }
        if (TravelLogMenuCommand.SHOW_CURRENT_PAGE.matches(input)) {
            showCurrentPage();
            return true;
        }
        if (TravelLogMenuCommand.SHOW_DAILY_QUESTS.matches(input)) {
            showDailyQuests();
            return true;
        }
        if (TravelLogMenuCommand.SHOW_MAIN_QUESTS.matches(input)) {
            showMainQuests();
            return true;
        }
        if (TravelLogMenuCommand.SHOW_EPIC_QUESTS.matches(input)) {
            showEpicQuests();
            return true;
        }
        if (TravelLogMenuCommand.SHOW_ALL_QUESTS.matches(input)) {
            showAllQuests();
            return true;
        }
        if (TravelLogMenuCommand.SHOW_COMPLETED_QUESTS.matches(input)) {
            showCompletedQuests();
            return true;
        }
        if (TravelLogMenuCommand.COMPLETE_QUEST.matches(input)) {
            String name = TravelLogMenuCommand.COMPLETE_QUEST.getParameter("questName");
            completeQuest(name);
            return true;
        }
        if (TravelLogMenuCommand.SHOW_QUEST_PROGRESS.matches(input)) {
            String name = TravelLogMenuCommand.SHOW_QUEST_PROGRESS.getParameter("questName");
            showQuestProgress(name);
            return true;
        }
        return false;
    }

    /** Handles the mini-game travel log commands; returns true if a command matched. */
    private boolean dispatchMiniGameCommand(String input) {
        if (TravelLogMenuCommand.SHOW_MINIGAMES.matches(input)) {
            showMiniGames();
            return true;
        }
        if (TravelLogMenuCommand.ENTER_MINIGAME.matches(input)) {
            String type = TravelLogMenuCommand.ENTER_MINIGAME.getParameter("type");
            int stage = Integer.parseInt(TravelLogMenuCommand.ENTER_MINIGAME.getParameter("stage"));
            enterMiniGame(type, stage);
            return true;
        }
        return false;
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
        // When on the mini-game page, delegate to the mini-game listing
        // so "show current page" reflects the page the player is viewing.
        if (controller.isViewingMiniGamePage()) {
            showMiniGames();
            return;
        }
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

    // --- Mini-games (travel log's mini-game page) ---

    public void showMiniGames() {
        CommandResult<List<MiniGameDataEntry>> result = controller.showMiniGames();
        if (result.isSuccess()) {
            renderMiniGameList(result.getMessage(), result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void enterMiniGame(String type, int stage) {
        CommandResult<Void> result = controller.enterMiniGame(type, stage);
        displayCommandResult(result);
    }

    private void renderMiniGameList(String header, List<MiniGameDataEntry> entries) {
        displayMessage("── " + header + " ──");
        if (entries == null || entries.isEmpty()) {
            displayMessage("  (no mini-games available)");
            return;
        }
        for (MiniGameDataEntry e : entries) {
            displayMessage(formatMiniGameLine(e));
        }
        displayMessage("Use 'enter minigame -t <type> -s <stage>' to play one.");
    }

    private String formatMiniGameLine(MiniGameDataEntry e) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        String typeLabel = prettyType(e.getMiniGameType());
        sb.append(typeLabel);
        sb.append(" — stage ").append(e.getStage());
        sb.append(" (difficulty ").append(e.getDifficultyTier()).append(")");
        if (e.getCoinReward() > 0) {
            sb.append("  reward: ").append(e.getCoinReward()).append("c");
        }
        return sb.toString();
    }

    /** Pretty-prints a mini-game type for the listing (e.g. VASE_BREAKER → Vase Breaker). */
    private String prettyType(String raw) {
        if (raw == null) return "Unknown";
        String friendly = raw.replace('_', ' ').toLowerCase();
        if (friendly.isEmpty()) return raw;
        return Character.toUpperCase(friendly.charAt(0)) + friendly.substring(1);
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

    private void printHelp() {
        displayError("Unknown travel log command. Available commands:");
        displayError("  travel log page <daily|main|epic|minigame>");
        displayError("  show current page");
        displayError("  show daily quests");
        displayError("  show main quests");
        displayError("  show epic quests");
        displayError("  show all quests");
        displayError("  show completed quests");
        displayError("  show quest progress -n <quest name>");
        displayError("  complete quest -n <quest name>");
        displayError("  show minigames");
        displayError("  enter minigame -t <type> -s <stage>");
        displayError("  menu exit");
    }
}
