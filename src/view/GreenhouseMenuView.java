package view;

import controller.GreenhouseMenuController;
import controller.result.CommandResult;
import model.command.GreenhouseMenuCommand;

import java.util.function.BiConsumer;

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
            runWithCoords(GreenhouseMenuCommand.PLANT_POT, this::plantPot);
        } else if (GreenhouseMenuCommand.COLLECT.matches(input)) {
            runWithCoords(GreenhouseMenuCommand.COLLECT, this::collect);
        } else if (GreenhouseMenuCommand.GROW.matches(input)) {
            runWithCoords(GreenhouseMenuCommand.GROW, this::grow);
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

    /** Parses x/y safely so huge numbers can't crash the app. */
    private void runWithCoords(GreenhouseMenuCommand cmd, BiConsumer<Integer, Integer> action) {
        try {
            int x = Integer.parseInt(cmd.getParameter("x"));
            int y = Integer.parseInt(cmd.getParameter("y"));
            action.accept(x, y);
        } catch (NumberFormatException e) {
            displayError("Invalid number in command.");
        }
    }

    public void showGreenhouse() {
        CommandResult<String> result = controller.showGreenhouse();
        if (result.isSuccess()) {
            displayMessage(result.getMessage());
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
