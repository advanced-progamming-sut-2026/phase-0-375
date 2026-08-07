package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.user.User;

public class SettingsMenuController extends AppMenuController {
    private static SettingsMenuController instance = null;

    private SettingsMenuController() {}

    public static SettingsMenuController getInstance() {
        if (instance == null) instance = new SettingsMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return CommandResult.error("No menus reachable from settings.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.MAIN);
        return CommandResult.success("Returned to main menu.");
    }

    public CommandResult<Void> changeDifficulty(int level) {
        if (level < 1 || level > 5) {
            return CommandResult.error("Difficulty must be between 1 and 5.");
        }
        User user = App.getInstance().getCurrentUser();
        user.setDifficultyLevel(level);
        App.getInstance().getUserRepository().flush();
        return CommandResult.success("Difficulty set to " + level + ".");
    }
}
