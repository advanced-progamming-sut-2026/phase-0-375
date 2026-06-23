package view;

import controller.AppMenuController;
import controller.result.CommandResult;

public abstract class BaseMenuView {
    private AppMenuController appController = AppMenuController.getInstance();

    public abstract void processInput(String input);

    public void displayMessage(String message) {
        System.out.println(message);
    }
    public void displayError(String error) {
        System.err.println(error);
    }

    public void menuEnter(String menuName) {
        CommandResult<Void> result = appController.menuEnter(menuName);
        if (!result.isSuccess()) {
            System.out.println(result.getMessage());
            return;
        }
        System.out.println(result.getMessage());
    }
    public void menuExit() {
        CommandResult<Void> result = AppMenuController.getInstance().menuExit();
        if (!result.isSuccess()) {
            displayError(result.getMessage());
            return;
        }
        System.out.println(result.getMessage());
    }
    public void menuShowCurrent() {
        CommandResult<String> result = AppMenuController.getInstance().menuShowCurrent();
        if (!result.isSuccess()) {
            System.err.println(result.getMessage());
            return;
        }
        System.out.println("Current menu: " + result.getMessage());
    }
}
