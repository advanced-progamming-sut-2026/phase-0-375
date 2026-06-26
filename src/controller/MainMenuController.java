package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;

public class MainMenuController extends AppMenuController {
    private static MainMenuController instance = null;

    private MainMenuController() {}

    public static MainMenuController getInstance() {
        if (instance == null) instance = new MainMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return switch (menuName.toLowerCase()) {
            case "game" -> {
                App.getInstance().setCurrentMenu(MenuType.GAME);
                yield CommandResult.success("Entered game menu.");
            }
            case "settings" -> {
                App.getInstance().setCurrentMenu(MenuType.SETTINGS);
                yield CommandResult.success("Entered settings menu.");
            }
            case "news" -> {
                App.getInstance().setCurrentMenu(MenuType.NEWS);
                yield CommandResult.success("Entered news menu.");
            }
            case "profile" -> {
                App.getInstance().setCurrentMenu(MenuType.PROFILE);
                yield CommandResult.success("Entered profile menu.");
            }
            default -> CommandResult.error("Cannot go to '" + menuName + "' from main menu.");
        };
    }

    @Override
    public CommandResult<Void> menuExit() {
        return CommandResult.error("Use 'menu logout' to log out from the main menu.");
    }

    public CommandResult<Void> logout() {
        App app = App.getInstance();
        app.getCurrentUser().setStayLoggedIn(false);
        app.getUserRepository().flush();
        app.setCurrentUser(null);
        app.setCurrentMenu(MenuType.REGISTER);
        return CommandResult.success("Logged out successfully.");
    }
}
