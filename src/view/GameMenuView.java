package view;

import controller.GameMenuController;

public class GameMenuView extends AppMenuView {
    private static GameMenuView instance = null;

    public static GameMenuView getInstance() {
        if (instance == null) instance = new GameMenuView();
        return instance;
    }

    private GameMenuController controller;

    @Override
    public void processInput(String input) { }

    public void enterChapter(String chapterName){}
    public void greenhouse(){}
    public void travelLog(){}
    public void leaderboard(){}
    public void coinWallet(){}
    public void gemWallet(){}
    public void cheatAdd(int n, String type){}
}
