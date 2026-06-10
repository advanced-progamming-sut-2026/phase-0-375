package view;

import controller.ProfileMenuController;

public class ProfileMenuView extends BaseMenuView {
    private ProfileMenuController controller;

    @Override
    public void processInput(String input) {}

    public void changeUsername(String username) {}
    public void changeNickname(String nickname) {}
    public void changeEmail(String email) {}
    public void changePassword(String newPassword, String oldPassword) {}
    public void showInfo() {}
}
