package view;

import controller.PlantSelectionMenuController;
import controller.result.CommandResult;
import model.command.PlantSelectionMenuCommand;

import java.util.List;

public class PlantSelectionMenuView extends AppMenuView {
    private static PlantSelectionMenuView instance;

    public static PlantSelectionMenuView getInstance() {
        if (instance == null) instance = new PlantSelectionMenuView();
        return instance;
    }

    private final PlantSelectionMenuController controller = PlantSelectionMenuController.getInstance();

    @Override
    public void processInput(String input) {
        if (PlantSelectionMenuCommand.SHOW_ALL_PLANTS.matches(input)) {
            showAllPlants();
        } else if (PlantSelectionMenuCommand.SHOW_AVAILABLE_PLANTS.matches(input)) {
            showAvailablePlants();
        } else if (PlantSelectionMenuCommand.ADD_PLANT.matches(input)) {
            String type = PlantSelectionMenuCommand.ADD_PLANT.getParameter("type");
            addPlant(type);
        } else if (PlantSelectionMenuCommand.REMOVE_PLANT.matches(input)) {
            String type = PlantSelectionMenuCommand.REMOVE_PLANT.getParameter("type");
            removePlant(type);
        } else if (PlantSelectionMenuCommand.BOOST_PLANT.matches(input)) {
            String type = PlantSelectionMenuCommand.BOOST_PLANT.getParameter("type");
            boostPlant(type);
        } else if (PlantSelectionMenuCommand.START_GAME.matches(input)) {
            startGame();
        } else {
            displayError("Usage:");
            displayError("  show all plants");
            displayError("  show available plants");
            displayError("  add plant -t <type>");
            displayError("  remove plant -t <type>");
            displayError("  boost plant -t <type>");
            displayError("  start game");
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

    public void showAvailablePlants() {
        CommandResult<List<String>> result = controller.showAvailablePlants();
        if (result.isSuccess()) {
            List<String> plants = result.getData();
            if (plants.isEmpty()) {
                displayMessage("No plants available yet. Purchase some from the collection!");
                return;
            }
            displayMessage("── Available Plants ──");
            for (String p : plants) {
                displayMessage("  " + p);
            }
        } else {
            displayError(result.getMessage());
        }
    }

    public void addPlant(String type) {
        CommandResult<Void> result = controller.addPlant(type);
        displayCommandResult(result);
    }

    public void removePlant(String type) {
        CommandResult<Void> result = controller.removePlant(type);
        displayCommandResult(result);
    }

    public void boostPlant(String type) {
        CommandResult<Void> result = controller.boostPlant(type);
        displayCommandResult(result);
    }

    public void startGame() {
        CommandResult<Void> result = controller.startGame();
        displayCommandResult(result);
    }
}
