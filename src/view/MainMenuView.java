package view;

import controller.MainMenuController;
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
        } else {
            displayError("Unknown command. Try: menu logout, or menu enter <menu_name>");
        }
    }

    public void logout() {
        CommandResult<Void> result = controller.logout();
        displayCommandResult(result);
    }
}
