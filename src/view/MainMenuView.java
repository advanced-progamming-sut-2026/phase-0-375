package view;

import controller.MainMenuController;
import controller.NewsMenuController;
import controller.result.CommandResult;
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
        } else {
            displayError(
                "Unknown command. Try: menu logout, menu leaderboard [-s <column>] [-o asc|desc], " +
                        "menu enter score-game, or menu enter <menu_name>"
            );
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
}
