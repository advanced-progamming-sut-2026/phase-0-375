package view;

import controller.CollectionMenuController;

public class CollectionMenuView extends AppMenuView {
    private static CollectionMenuView instance = null;

    public static CollectionMenuView getInstance() {
        if (instance == null) instance = new CollectionMenuView();
        return instance;
    }

    private final CollectionMenuController controller = CollectionMenuController.getInstance();

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
