package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;

public class GameplayMenuController extends AppMenuController {
    private static GameplayMenuController instance = null;

    private GameplayMenuController() {}

    public static GameplayMenuController getInstance() {
        if (instance == null) instance = new GameplayMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return CommandResult.error("Cannot enter other menus during gameplay.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.GAME);
        App.getInstance().setCurrentGameModel(null);
        App.getInstance().setCurrentGameLoop(null);
        return CommandResult.success("Returned to game menu.");
    }

    // ── In-game commands (stubs) ──

    public CommandResult<Void> advanceTime(int count) { return null; }
    public CommandResult<Void> collectSun(int x, int y) { return null; }
    public CommandResult<Integer> showSunAmount() { return null; }
    public CommandResult<Void> cheatAddSuns(int count) { return null; }
    public CommandResult<Void> plant(String type, int x, int y) { return null; }
    public CommandResult<Void> cheatRemoveCooldown() { return null; }
    public CommandResult<Void> pluck(int x, int y) { return null; }
    public CommandResult<Void> feed(int x, int y) { return null; }
    public CommandResult<Void> cheatAddPlantFood() { return null; }
    public CommandResult<Object> showMap() { return null; }
    public CommandResult<Object> showPlantsStatus() { return null; }
    public CommandResult<Object> showTileStatus(int x, int y) { return null; }
    public CommandResult<Void> releaseNuke() { return null; }
    public CommandResult<Object> zombiesInfo() { return null; }
    public CommandResult<Void> cheatSpawnZombie(String zombieType, int x, int y) { return null; }
}
