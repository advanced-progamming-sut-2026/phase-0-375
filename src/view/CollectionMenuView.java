package view;

import controller.CollectionMenuController;

public class CollectionMenuView extends BaseMenuView {
    private CollectionMenuController controller;

    @Override
    public void processInput(String input) {}

    public void showPlants() {}
    public void showAllPlants() {}
    public void showZombies() {}
    public void showAllZombies() {}
    public void showPlant(String plantName) {}
    public void showZombie(String zombieName) {}
    public void upgradePlant(String plantName) {}
    public void purchasePlant(String plantName) {}
}
