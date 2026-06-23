package controller;

import controller.result.CommandResult;

public class GameMenuController extends AppMenuController {
    private static GameMenuController instance = null;

    private GameMenuController() {}

    public static GameMenuController getInstance() {
        if (instance == null) instance = new GameMenuController();
        return instance;
    }

    public CommandResult<Void> enterChapter(String chapterName) { return null; }
    public CommandResult<Void> greenhouse() { return null; }
    public CommandResult<Void> travelLog() { return null; }
    public CommandResult<Void> leaderboard() { return null; }
    public CommandResult<Void> coinWallet() { return null; }
    public CommandResult<Void> gemWallet() { return null; }
    public CommandResult<Void> cheatAdd(int n, String type) { return null; }
}
