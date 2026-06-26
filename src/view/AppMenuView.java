package view;

import controller.*;
import controller.result.CommandResult;
import model.app.App;
import model.command.CommonCommand;
import model.enums.MenuType;

import java.util.Scanner;

public class AppMenuView {
    private static AppMenuView instance;

    AppMenuView() {}

    public static AppMenuView getInstance() {
        if (instance == null) instance = new AppMenuView();
        return instance;
    }

    private final RegisterMenuView registerMenuView = RegisterMenuView.getInstance();
    private final LoginMenuView loginMenuView = LoginMenuView.getInstance();
    private final MainMenuView mainMenuView = MainMenuView.getInstance();
    private final GameMenuView gameMenuView = GameMenuView.getInstance();
    private final GameplayMenuView gameplayMenuView = GameplayMenuView.getInstance();
    private final CollectionMenuView collectionMenuView = CollectionMenuView.getInstance();
    private final NewsMenuView newsMenuView = NewsMenuView.getInstance();
    private final PlantSelectionMenuView plantSelectionMenuView = PlantSelectionMenuView.getInstance();
    private final ProfileMenuView profileMenuView = ProfileMenuView.getInstance();
    private final SettingsMenuView settingsMenuView = SettingsMenuView.getInstance();
    private final ShopMenuView shopMenuView = ShopMenuView.getInstance();
    private final GreenhouseMenuView greenhouseMenuView = GreenhouseMenuView.getInstance();
    private final TravelLogMenuVIew travelLogMenuVIew = TravelLogMenuVIew.getInstance();

    private final Scanner scanner = new Scanner(System.in);
    private boolean running = true;

    public void run() {
        while (running && scanner.hasNextLine()) {
            String command = scanner.nextLine().trim();

            // Phase 1: universal commands (work in any menu)
            if (CommonCommand.MENU_ENTER.matches(command)) {
                String menuName = CommonCommand.MENU_ENTER.getParameter("menu_name");
                handleMenuEnter(menuName);
                continue;
            } else if (CommonCommand.MENU_EXIT.matches(command)) {
                handleMenuExit();
                continue;
            } else if (CommonCommand.MENU_SHOW_CURRENT.matches(command)) {
                handleMenuShowCurrent();
                continue;
            }

            // Phase 2: delegate to current menu's specific handler
            MenuType current = App.getInstance().getCurrentMenu();
            switch (current) {
                case MAIN -> mainMenuView.processInput(command);
                case REGISTER -> registerMenuView.processInput(command);
                case LOGIN -> loginMenuView.processInput(command);
                case GAME -> gameMenuView.processInput(command);
                case IN_GAME -> gameplayMenuView.processInput(command);
                case NEWS -> newsMenuView.processInput(command);
                case SHOP -> shopMenuView.processInput(command);
                case PROFILE -> profileMenuView.processInput(command);
                case SETTINGS -> settingsMenuView.processInput(command);
                case COLLECTION -> collectionMenuView.processInput(command);
                case PLANT_SELECTION -> plantSelectionMenuView.processInput(command);
                case GREENHOUSE -> greenhouseMenuView.processInput(command);
                case TRAVEL_LOG -> travelLogMenuVIew.processInput(command);
            }
        }
        scanner.close();
    }

    public void processInput(String input) {}

    // ── Common command handlers ──

    private void handleMenuEnter(String menuName) {
        AppMenuController controller = getControllerForCurrentMenu();
        CommandResult<Void> result = controller.menuEnter(menuName);
        displayCommandResult(result);
    }

    private void handleMenuExit() {
        AppMenuController controller = getControllerForCurrentMenu();
        CommandResult<Void> result = controller.menuExit();
        if (result.isSuccess() && "Exiting application.".equals(result.getMessage())) {
            System.out.println("Goodbye!");
            running = false;
            return;
        }
        displayCommandResult(result);
    }

    private void handleMenuShowCurrent() {
        MenuType current = App.getInstance().getCurrentMenu();
        displayMessage("You are in the " + current.name().toLowerCase() + " menu.");
    }

    /**
     * Routes "menu enter" and "menu exit" to the controller of the current menu.
     */
    private AppMenuController getControllerForCurrentMenu() {
        MenuType current = App.getInstance().getCurrentMenu();
        return switch (current) {
            case REGISTER -> RegisterMenuController.getInstance();
            case LOGIN -> LoginMenuController.getInstance();
            case MAIN -> MainMenuController.getInstance();
            case GAME -> GameMenuController.getInstance();
            case SETTINGS -> SettingsMenuController.getInstance();
            case NEWS -> NewsMenuController.getInstance();
            case PROFILE -> ProfileMenuController.getInstance();
            case COLLECTION -> CollectionMenuController.getInstance();
            case PLANT_SELECTION -> PlantSelectionMenuController.getInstance();
            case IN_GAME -> GameplayMenuController.getInstance();
            case GREENHOUSE -> GreenhouseMenuController.getInstance();
            case SHOP -> ShopMenuController.getInstance();
            case TRAVEL_LOG -> TravelLogMenuController.getInstance();
        };
    }

    // ── Display helpers ──

    public void displayMessage(String message) {
        System.out.println(message);
    }

    public void displayError(String error) {
        System.err.println(error);
    }

    public void displayCommandResult(CommandResult<?> result) {
        if (result.isSuccess()) {
            displayMessage(result.getMessage());
        } else {
            displayError(result.getMessage());
        }
    }
}
