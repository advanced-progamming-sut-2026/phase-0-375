package view;

import controller.PlantSelectionMenuController;

public class PlantSelectionMenuView extends AppMenuView {
    private static PlantSelectionMenuView instance;

    public static PlantSelectionMenuView getInstance() {
        if (instance == null) instance = new PlantSelectionMenuView();
        return instance;
    }


    private PlantSelectionMenuController controller;

    @Override
    public void processInput(String input) {}

    public void showAllPlants() {}
    public void showAvailablePlants() {}
    public void addPlant(String type) {}
    public void removePlant(String type) {}
    public void boostPlant(String type) {}
    public void startGame() {}
}
