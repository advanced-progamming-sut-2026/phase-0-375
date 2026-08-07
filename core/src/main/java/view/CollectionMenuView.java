package view;

import controller.CollectionMenuController;
import controller.result.CommandResult;
import model.command.CollectionMenuCommand;

import java.util.List;

public class CollectionMenuView extends AppMenuView {
    private static CollectionMenuView instance = null;

    public static CollectionMenuView getInstance() {
        if (instance == null) instance = new CollectionMenuView();
        return instance;
    }

    private final CollectionMenuController controller = CollectionMenuController.getInstance();

    @Override
    public void processInput(String input) {
        if (CollectionMenuCommand.SHOW_PLANTS.matches(input)) {
            showPlants();
        } else if (CollectionMenuCommand.SHOW_ALL_PLANTS.matches(input)) {
            showAllPlants();
        } else if (CollectionMenuCommand.SHOW_ZOMBIES.matches(input)) {
            showZombies();
        } else if (CollectionMenuCommand.SHOW_ALL_ZOMBIES.matches(input)) {
            showAllZombies();
        } else if (CollectionMenuCommand.SHOW_PLANT.matches(input)) {
            String name = CollectionMenuCommand.SHOW_PLANT.getParameter("plantName");
            showPlant(name);
        } else if (CollectionMenuCommand.SHOW_ZOMBIE.matches(input)) {
            String name = CollectionMenuCommand.SHOW_ZOMBIE.getParameter("zombieName");
            showZombie(name);
        } else if (CollectionMenuCommand.UPGRADE_PLANT.matches(input)) {
            String name = CollectionMenuCommand.UPGRADE_PLANT.getParameter("plantName");
            upgradePlant(name);
        } else if (CollectionMenuCommand.PURCHASE_PLANT.matches(input)) {
            String name = CollectionMenuCommand.PURCHASE_PLANT.getParameter("plantName");
            purchasePlant(name);
        } else {
            displayError("Usage:");
            displayError("  menu collection show-plants");
            displayError("  menu collection show-all-plants");
            displayError("  menu collection show-zombies");
            displayError("  menu collection show-all-zombies");
            displayError("  menu collection show-plant -p <name>");
            displayError("  menu collection show-zombie -z <name>");
            displayError("  menu collection upgrade-plant -p <name>");
            displayError("  menu collection purchase-plant -p <name>");
        }
    }

    public void showPlants() {
        CommandResult<List<String>> result = controller.showPlants();
        if (result.isSuccess()) {
            List<String> plants = result.getData();
            if (plants.isEmpty()) {
                displayMessage("No plants unlocked yet.");
                return;
            }
            displayMessage("── Unlocked Plants ──");
            for (String p : plants) {
                displayMessage("  " + p);
            }
        } else {
            displayError(result.getMessage());
        }
    }

    public void showAllPlants() {
        CommandResult<List<String>> result = controller.showAllPlants();
        if (result.isSuccess()) {
            List<String> plants = result.getData();
            displayMessage("── All Plants ──");
            for (String p : plants) {
                displayMessage("  " + p);
            }
        } else {
            displayError(result.getMessage());
        }
    }

    public void showZombies() {
        CommandResult<List<String>> result = controller.showZombies();
        if (result.isSuccess()) {
            List<String> zombies = result.getData();
            if (zombies.isEmpty()) {
                displayMessage("No zombies discovered yet.");
                return;
            }
            displayMessage("── Discovered Zombies ──");
            for (String z : zombies) {
                displayMessage("  " + z);
            }
        } else {
            displayError(result.getMessage());
        }
    }

    public void showAllZombies() {
        CommandResult<List<String>> result = controller.showAllZombies();
        if (result.isSuccess()) {
            List<String> zombies = result.getData();
            displayMessage("── All Zombies ──");
            for (String z : zombies) {
                displayMessage("  " + z);
            }
        } else {
            displayError(result.getMessage());
        }
    }

    public void showPlant(String plantName) {
        CommandResult<String> result = controller.showPlant(plantName);
        if (result.isSuccess()) {
            displayMessage(result.getData());
            displayMessage("Use 'menu collection upgrade-plant -p " + plantName
                    + "' to upgrade (if unlocked).");
        } else {
            displayError(result.getMessage());
        }
    }

    public void showZombie(String zombieName) {
        CommandResult<String> result = controller.showZombie(zombieName);
        if (result.isSuccess()) {
            displayMessage(result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void upgradePlant(String plantName) {
        CommandResult<Void> result = controller.upgradePlant(plantName);
        displayCommandResult(result);
    }

    public void purchasePlant(String plantName) {
        CommandResult<Void> result = controller.purchasePlant(plantName);
        displayCommandResult(result);
    }
}
