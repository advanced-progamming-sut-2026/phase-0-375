package controller;

import controller.result.CommandResult;

public class LoginMenuController extends AppMenuController {
    private static LoginMenuController instance = null;

    private LoginMenuController() {}

    public static LoginMenuController getInstance() {
        if (instance == null) instance = new LoginMenuController();
        return instance;
    }

    public CommandResult<Void> login(String username, String password, boolean stayLoggedIn) { return null; }
    public CommandResult<Void> forgetPassword(String username, String email) { return null; }
    public CommandResult<Void> answer(String answer) { return null; }
}
