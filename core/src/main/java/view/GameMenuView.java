package view;

import controller.GameMenuController;
import controller.result.CommandResult;
import model.command.GameMenuCommand;
import model.enums.Chapter;
import model.user.User;

import java.util.List;

public class GameMenuView extends AppMenuView {
    private static GameMenuView instance = null;

    public static GameMenuView getInstance() {
        if (instance == null) instance = new GameMenuView();
        return instance;
    }

    private final GameMenuController controller = GameMenuController.getInstance();

    @Override
    public void processInput(String input) {
        if (GameMenuCommand.ENTER_CHAPTER.matches(input)) {
            String chapter = GameMenuCommand.ENTER_CHAPTER.getParameter("chaptername");
            String levelIdStr = GameMenuCommand.ENTER_CHAPTER.getParameter("levelid");
            Integer levelId = levelIdStr == null ? null : Integer.valueOf(levelIdStr);
            enterChapter(chapter, levelId);
        } else if (GameMenuCommand.GREENHOUSE.matches(input)) {
            greenhouse();
        } else if (GameMenuCommand.TRAVEL_LOG.matches(input)) {
            travelLog();
        } else if (GameMenuCommand.LEADERBOARD.matches(input)) {
            leaderboard(GameMenuCommand.LEADERBOARD.getParameter("sort"),
                    GameMenuCommand.LEADERBOARD.getParameter("order"));
        } else if (GameMenuCommand.COIN_WALLET.matches(input)) {
            coinWallet();
        } else if (GameMenuCommand.GEM_WALLET.matches(input)) {
            gemWallet();
        } else if (GameMenuCommand.CHEAT_ADD.matches(input)) {
            int n = Integer.parseInt(GameMenuCommand.CHEAT_ADD.getParameter("n"));
            String type = GameMenuCommand.CHEAT_ADD.getParameter("type");
            cheatAdd(n, type);
        } else {
            displayError("Usage:");
            displayError("  menu enter chapter -c <name> [-l <level_id>]");
            displayError("  menu greenhouse");
            displayError("  menu travel-log  (mini-games are entered from here)");
            displayError("  menu leaderboard [-s score|progress|minigames|daily-quests|quests] [-o asc|desc]");
            displayError("  menu coin-wallet | menu gem-wallet");
            displayError("  menu cheat add <n> <coin|diamond>");
        }
    }

    public void enterChapter(String chapterName, Integer levelId) {
        CommandResult<Void> result = controller.enterChapter(chapterName, levelId);
        displayCommandResult(result);
    }

    public void greenhouse() {
        CommandResult<Void> result = controller.greenhouse();
        displayCommandResult(result);
    }

    public void travelLog() {
        CommandResult<Void> result = controller.travelLog();
        displayCommandResult(result);
    }

    public void leaderboard() {
        leaderboard(null, null);
    }

    /** Renders the leaderboard sorted by the given column and order. */
    public void leaderboard(String sortKey, String order) {
        CommandResult<List<User>> result = controller.leaderboard(sortKey, order);
        if (result.isSuccess()) {
            List<User> users = result.getData();
            if (users.isEmpty()) {
                displayMessage("No players yet.");
                return;
            }
            displayMessage("── Leaderboard ──");
            displayMessage(String.format("%-20s %-12s %-10s %-12s %-8s %s",
                    "Username", "Progress", "MiniGames", "DailyQuests", "Quests", "MyoPoint"));
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                String progress = formatProgress(u);
                displayMessage(String.format("%-20s %-12s %-10d %-12d %-8d %s",
                        u.getUsername(), progress,
                        u.getCompletedMiniGames(),
                        u.getCompletedDailyQuests(),
                        u.getCompletedNonDailyQuests(),
                        u.formattedHighestMyopoint()));
            }
        } else {
            displayError(result.getMessage());
        }
    }

    public void coinWallet() {
        CommandResult<Void> result = controller.coinWallet();
        displayCommandResult(result);
    }

    public void gemWallet() {
        CommandResult<Void> result = controller.gemWallet();
        displayCommandResult(result);
    }

    public void cheatAdd(int n, String type) {
        CommandResult<Void> result = controller.cheatAdd(n, type);
        displayCommandResult(result);
    }

    private String formatProgress(User user) {
        var progress = user.getChapterProgress();
        if (progress == null || progress.isEmpty()) return "1-0";
        Chapter[] chapters = Chapter.values();
        for (int i = chapters.length - 1; i >= 0; i--) {
            int level = progress.getOrDefault(chapters[i], 0);
            if (level > 0) return (i + 1) + "-" + level;
        }
        return "1-0";
    }
}
