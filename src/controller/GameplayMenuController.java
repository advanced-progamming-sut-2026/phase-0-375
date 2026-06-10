package controller;

import controller.result.CommandResult;

public class GameplayMenuController extends BaseMenuController {
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
