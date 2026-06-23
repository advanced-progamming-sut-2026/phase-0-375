package view;

import controller.LoginMenuController;

public class LoginMenuView extends AppMenuView {
    private static LoginMenuView  instance;

    public static LoginMenuView getInstance() {
        if (instance == null) instance = new LoginMenuView();
        return instance;
    }

    private LoginMenuController controller;

    @Override
    public void processInput(String input) { }

    public void login(String username, String password, boolean stayLoggedIn) { }
    public void forgetPassword(String username, String email) { }
    public void answer(String answer) { }
}
