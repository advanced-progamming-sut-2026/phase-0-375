package controller;

import controller.result.CommandResult;

public class AppMenuController {
    private static AppMenuController instance;

    private AppMenuController() {
    }

    public static AppMenuController getInstance() {
        if (instance == null) instance = new AppMenuController();
        return instance;
    }

    public CommandResult<Void> menuEnter(String menuName) { return null; }
    public CommandResult<Void> menuExit() { return null; }
    public CommandResult<String> menuShowCurrent() { return null; }
}