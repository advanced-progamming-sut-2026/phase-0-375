package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;

public class TravelLogMenuController extends AppMenuController {
    private static TravelLogMenuController instance = null;

    private TravelLogMenuController() {}

    public static TravelLogMenuController getInstance() {
        if (instance == null) instance = new TravelLogMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return CommandResult.error("No menus reachable from travel log.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.GAME);
        return CommandResult.success("Returned to game menu.");
    }
}
