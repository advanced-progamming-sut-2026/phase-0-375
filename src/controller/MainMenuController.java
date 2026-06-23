package controller;

import controller.result.CommandResult;

public class MainMenuController extends AppMenuController {
    private static  MainMenuController instance = null;

    private MainMenuController() {}

    public static MainMenuController getInstance() {
        if (instance == null) instance = new MainMenuController();
        return instance;
    }

    public CommandResult<Void> logout() { return null; }
}
