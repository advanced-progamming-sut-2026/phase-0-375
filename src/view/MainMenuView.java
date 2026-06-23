package view;

import controller.MainMenuController;

public class MainMenuView extends AppMenuView {
    private static MainMenuView instance = null;

    MainMenuView() {}

    public static MainMenuView getInstance() {
        if (instance == null) instance = new MainMenuView();
        return instance;
    }

    private MainMenuController controller =  MainMenuController.getInstance();

    @Override
    public void processInput(String input) { }

    public void run() { }

    public void logout() {

    }
}
