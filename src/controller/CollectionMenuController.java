package controller;

import controller.result.CommandResult;

public class CollectionMenuController extends AppMenuController {
    private static CollectionMenuController instance = null;

    private CollectionMenuController() {
    }

    public static CollectionMenuController getInstance() {
        if  (instance == null) instance = new CollectionMenuController();
        return instance;
    }


    public CommandResult<Object> showPlants() { return null; }
    public CommandResult<Object> showAllPlants() { return null; }
    public CommandResult<Object> showZombies() { return null; }
    public CommandResult<Object> showAllZombies() { return null; }
    public CommandResult<Object> showPlant(String plantName) { return null; }
    public CommandResult<Object> showZombie(String zombieName) { return null; }
    public CommandResult<Void> upgradePlant(String plantName) { return null; }
    public CommandResult<Void> purchasePlant(String plantName) { return null; }
}
