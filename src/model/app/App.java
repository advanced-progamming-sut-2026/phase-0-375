package model.app;

import model.enums.MenuType;
import model.user.User;

public class App {
    private static App instance;

    private User currentUser;
    private MenuType currentMenu;

    private App() {
        this.currentUser = null;
        this.currentMenu = MenuType.REGISTER;
    }

    public static App getInstance() {
        if (instance == null) instance = new App();
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public MenuType getCurrentMenu() {
        return currentMenu;
    }

    public void setCurrentMenu(MenuType currentMenu) {
        this.currentMenu = currentMenu;
    }
}
