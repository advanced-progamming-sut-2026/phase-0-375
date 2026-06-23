package view;

import controller.ProfileMenuController;

public class ProfileMenuView extends AppMenuView {
    private static ProfileMenuView instance;

    public static ProfileMenuView getInstance() {
        if (instance == null) instance = new ProfileMenuView();
        return instance;
    }

    private ProfileMenuController controller;

    @Override
    public void processInput(String input) {}

    public void changeUsername(String username) {}
    public void changeNickname(String nickname) {}
    public void changeEmail(String email) {}
    public void changePassword(String newPassword, String oldPassword) {}
    public void showInfo() {}
}
