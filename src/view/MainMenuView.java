package view;

import controller.MainMenuController;
import controller.NewsMenuController;
import controller.result.CommandResult;
import model.command.CommonCommand;
import model.command.MainMenuCommand;

public class MainMenuView extends AppMenuView {
    private static MainMenuView instance = null;

    MainMenuView() {}

    public static MainMenuView getInstance() {
        if (instance == null) instance = new MainMenuView();
        return instance;
    }

    private final MainMenuController controller = MainMenuController.getInstance();

    @Override
    public void processInput(String input) {
        if (MainMenuCommand.LOGOUT.matches(input)) {
            logout();
        } else if (MainMenuCommand.LEADERBOARD.matches(input)) {
            GameMenuView.getInstance().leaderboard(
                    MainMenuCommand.LEADERBOARD.getParameter("sort"),
                    MainMenuCommand.LEADERBOARD.getParameter("order"));
        } else if (CommonCommand.HELP.matches(input)) {
            displayHelp();
        } else {
            displayError("Unknown command");
            displayHelp();
        }
    }

    public void logout() {
        CommandResult<Void> result = controller.logout();
        displayCommandResult(result);
    }

    public void showNewsBadgeIfAny() {
        int unread = NewsMenuController.getInstance().countUnread();
        if (unread <= 0) {
            return;
        }
        String redBold = "\u001B[31;1m";
        String reset = "\u001B[0m";
        String bullet = "\u25CF";
        System.out.println(redBold
                + bullet + " You have " + unread + " unread news! "
                + "Type 'menu enter news' to view."
                + reset);
    }

    private void displayHelp() {
        displayError("Usage:");
        displayError("  menu logout");
        displayError("  menu leaderboard [-s <column>] [-o asc|desc]");
        displayError("  menu enter game");
        displayError("  menu enter score-game");
        displayError("  menu enter settings");
        displayError("  menu enter news");
        displayError("  menu enter profile");
    }
}
