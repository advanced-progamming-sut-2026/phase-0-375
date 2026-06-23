package controller;

import controller.result.CommandResult;

public class PlantSelectionMenuController extends AppMenuController {
    private static PlantSelectionMenuController instance = null;

    private PlantSelectionMenuController() {}

    public static PlantSelectionMenuController getInstance() {
        if (instance == null) instance = new PlantSelectionMenuController();
        return instance;
    }


    public CommandResult<Object> showAllPlants() { return null; }
    public CommandResult<Object> showAvailablePlants() { return null; }
    public CommandResult<Void> addPlant(String type) { return null; }
    public CommandResult<Void> removePlant(String type) { return null; }
    public CommandResult<Void> boostPlant(String type) { return null; }
    public CommandResult<Void> startGame() { return null; }
}
