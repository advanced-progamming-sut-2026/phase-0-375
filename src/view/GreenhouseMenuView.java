package view;

import controller.GreenhouseMenuController;
import controller.result.CommandResult;
import model.command.GreenhouseMenuCommand;

public class GreenhouseMenuView extends AppMenuView {
    private static GreenhouseMenuView instance = null;

    public static GreenhouseMenuView getInstance() {
        if (instance == null) instance = new GreenhouseMenuView();
        return instance;
    }

    private final GreenhouseMenuController controller = GreenhouseMenuController.getInstance();

    @Override
    public void processInput(String input) {
        if (GreenhouseMenuCommand.SHOW_GREENHOUSE.matches(input)) {
            showGreenhouse();
        } else if (GreenhouseMenuCommand.PLANT_POT.matches(input)) {
            int x = Integer.parseInt(GreenhouseMenuCommand.PLANT_POT.getParameter("x"));
            int y = Integer.parseInt(GreenhouseMenuCommand.PLANT_POT.getParameter("y"));
            plantPot(x, y);
        } else if (GreenhouseMenuCommand.COLLECT.matches(input)) {
            int x = Integer.parseInt(GreenhouseMenuCommand.COLLECT.getParameter("x"));
            int y = Integer.parseInt(GreenhouseMenuCommand.COLLECT.getParameter("y"));
            collect(x, y);
        } else if (GreenhouseMenuCommand.GROW.matches(input)) {
            int x = Integer.parseInt(GreenhouseMenuCommand.GROW.getParameter("x"));
            int y = Integer.parseInt(GreenhouseMenuCommand.GROW.getParameter("y"));
            grow(x, y);
        } else if (GreenhouseMenuCommand.ENTER_SHOP.matches(input)) {
            enterShop();
        } else {
            displayError("Usage:");
            displayError("  show greenhouse");
            displayError("  plant pot at (<x>, <y>)");
            displayError("  collect (<x>, <y>)");
            displayError("  grow (<x>, <y>)");
            displayError("  enter shop");
        }
    }

    public void showGreenhouse() {
        CommandResult<String> result = controller.showGreenhouse();
        if (result.isSuccess()) {
            displayMessage(result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void plantPot(int x, int y) {
        CommandResult<Void> result = controller.plantPot(x, y);
        displayCommandResult(result);
    }

    public void collect(int x, int y) {
        CommandResult<Void> result = controller.collect(x, y);
        displayCommandResult(result);
    }

    public void grow(int x, int y) {
        CommandResult<Void> result = controller.grow(x, y);
        displayCommandResult(result);
    }

    public void enterShop() {
        CommandResult<Void> result = controller.menuEnter("shop");
        displayCommandResult(result);
    }
}
