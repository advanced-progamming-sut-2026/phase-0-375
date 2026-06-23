package controller;

import controller.result.CommandResult;

public class ProfileMenuController extends AppMenuController {
    private static ProfileMenuController instance = null;

    private ProfileMenuController() {}

    public static ProfileMenuController getInstance() {
        if (instance == null) instance = new ProfileMenuController();
        return instance;
    }


    public CommandResult<Void> changeUsername(String username) { return null; }
    public CommandResult<Void> changeNickname(String nickname) { return null; }
    public CommandResult<Void> changeEmail(String email) { return null; }
    public CommandResult<Void> changePassword(String newPassword, String oldPassword) { return null; }
    public CommandResult<Object> showInfo() { return null; }
}
