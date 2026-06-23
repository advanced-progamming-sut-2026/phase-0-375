package controller;

import controller.result.CommandResult;
import model.enums.MenuType;

public class AppMenuController {
    private static AppMenuController instance = null;

    protected AppMenuController() {
    }

    public static AppMenuController getInstance() {
        if (instance == null) instance = new AppMenuController();
        return instance;
    }

    public CommandResult<Void> menuEnter(String menuName) { return null; }
    public CommandResult<Void> menuExit() { return null; }
    public CommandResult<MenuType> menuShowCurrent() { return null; }
}