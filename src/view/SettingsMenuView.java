package view;

import controller.SettingsMenuController;
import controller.result.CommandResult;
import model.command.SettingsMenuCommand;

public class SettingsMenuView extends AppMenuView {
    private static SettingsMenuView instance;

    public static SettingsMenuView getInstance() {
        if (instance == null) instance = new SettingsMenuView();
        return instance;
    }

    private final SettingsMenuController controller = SettingsMenuController.getInstance();

    @Override
    public void processInput(String input) {
        if (SettingsMenuCommand.CHANGE_DIFFICULTY.matches(input)) {
            int level = Integer.parseInt(
                SettingsMenuCommand.CHANGE_DIFFICULTY.getParameter("difficultyLevel"));
            changeDifficulty(level);
        } else {
            displayError("Usage: menu settings change-difficulty -l <1-5>");
        }
    }

    public void changeDifficulty(int difficultyLevel) {
        CommandResult<Void> result = controller.changeDifficulty(difficultyLevel);
        displayCommandResult(result);
    }
}
