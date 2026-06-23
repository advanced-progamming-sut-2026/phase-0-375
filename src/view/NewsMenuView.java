package view;

import controller.NewsMenuController;

public class NewsMenuView extends AppMenuView {
    private static NewsMenuView instance = null;

    public static NewsMenuView getInstance() {
        if (instance == null) instance = new NewsMenuView();
        return instance;
    }

    private NewsMenuController controller;

    @Override
    public void processInput(String input) {}

    public void showUnread() {}
    public void showAll() {}
}
