package view;

import controller.GameplayMenuController;

public class GameplayMenuView extends AppMenuView {
    private static GameplayMenuView instance = null;

    public static GameplayMenuView getInstance() {
        if (instance == null) instance = new GameplayMenuView();
        return instance;
    }

    private GameplayMenuController controller;

    @Override
    public void processInput(String input){}

    public void advanceTime(int count){}
    public void collectSun(int x, int y){}
    public void showSunAmount(){}
    public void cheatAddSuns(int count){}
    public void plant(String type, int x, int y){}
    public void cheatRemoveCooldown(){}
    public void pluck(int x, int y){}
    public void feed(int x, int y){}
    public void cheatAddPlantFood(){}
    public void showMap(){}
    public void showPlantsStatus(){}
    public void showTileStatus(int x, int y){}
    public void releaseNuke(){}
    public void zombiesInfo(){}
    public void cheatSpawnZombie(String zombieType, int x, int y){}
}
