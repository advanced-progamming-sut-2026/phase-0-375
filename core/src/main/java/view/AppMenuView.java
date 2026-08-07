package view;

import controller.*;
import controller.result.CommandResult;
import model.app.App;
import model.command.CommonCommand;
import model.enums.MenuType;

import view.tui.TuiShell;

import java.util.Scanner;

public class AppMenuView {
    private static AppMenuView instance;

    AppMenuView() {}

    public static AppMenuView getInstance() {
        if (instance == null) instance = new AppMenuView();
        return instance;
    }

    // Menu view singletons are fetched lazily on demand.
    private RegisterMenuView registerMenuView;
    private LoginMenuView loginMenuView;
    private MainMenuView mainMenuView;
    private GameMenuView gameMenuView;
    private GameplayMenuView gameplayMenuView;
    private CollectionMenuView collectionMenuView;
    private NewsMenuView newsMenuView;
    private PlantSelectionMenuView plantSelectionMenuView;
    private ProfileMenuView profileMenuView;
    private SettingsMenuView settingsMenuView;
    private ShopMenuView shopMenuView;
    private GreenhouseMenuView greenhouseMenuView;
    private TravelLogMenuView travelLogMenuVIew;

    private MenuType previousMenu = null;

    private final Scanner scanner = new Scanner(System.in);
    private boolean running = true;

    public void run() {
        while (running) {
            // Show the unread-news badge once when the user enters Main.
            MenuType current = App.getInstance().getCurrentMenu();
            if (current == MenuType.MAIN && previousMenu != MenuType.MAIN) {
                mainMenuView().showNewsBadgeIfAny();
            }
            previousMenu = current;

            String rawCommand = readCommandLine();
            if (rawCommand == null) break; // EOF or Ctrl-C
            String command = rawCommand.trim();
            // In TUI mode an empty submit just repaints; plain mode keeps
            // the original "Empty command." behavior of each menu view.
            if (TuiShell.getActive() != null && command.isEmpty()) continue;

            // Phase 1: universal commands (work in any menu)
            if (CommonCommand.MENU_ENTER.matches(command)) {
                String menuName = CommonCommand.MENU_ENTER.getParameter("menuName");
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
            current = App.getInstance().getCurrentMenu();
            switch (current) {
                case MAIN -> mainMenuView().processInput(command);
                case REGISTER -> registerMenuView().processInput(command);
                case LOGIN -> loginMenuView().processInput(command);
                case GAME -> gameMenuView().processInput(command);
                case IN_GAME -> gameplayMenuView().processInput(command);
                case NEWS -> newsMenuView().processInput(command);
                case SHOP -> shopMenuView().processInput(command);
                case PROFILE -> profileMenuView().processInput(command);
                case SETTINGS -> settingsMenuView().processInput(command);
                case COLLECTION -> collectionMenuView().processInput(command);
                case PLANT_SELECTION -> plantSelectionMenuView().processInput(command);
                case GREENHOUSE -> greenhouseMenuView().processInput(command);
                case TRAVEL_LOG -> travelLogMenuVIew().processInput(command);
            }
        }
        scanner.close();
    }

    /**
     * Reads one command line: from the TUI input box when the shell is
     * active, otherwise from stdin like the original CLI.
     */
    private String readCommandLine() {
        TuiShell shell = TuiShell.getActive();
        if (shell != null) {
            return shell.readCommand();
        }
        if (!scanner.hasNextLine()) return null;
        return scanner.nextLine();
    }

    // Lazy accessors for menu views

    private RegisterMenuView registerMenuView() {
        if (registerMenuView == null) registerMenuView = RegisterMenuView.getInstance();
        return registerMenuView;
    }
    private LoginMenuView loginMenuView() {
        if (loginMenuView == null) loginMenuView = LoginMenuView.getInstance();
        return loginMenuView;
    }
    private MainMenuView mainMenuView() {
        if (mainMenuView == null) mainMenuView = MainMenuView.getInstance();
        return mainMenuView;
    }
    private GameMenuView gameMenuView() {
        if (gameMenuView == null) gameMenuView = GameMenuView.getInstance();
        return gameMenuView;
    }
    private GameplayMenuView gameplayMenuView() {
        if (gameplayMenuView == null) gameplayMenuView = GameplayMenuView.getInstance();
        return gameplayMenuView;
    }
    private CollectionMenuView collectionMenuView() {
        if (collectionMenuView == null) collectionMenuView = CollectionMenuView.getInstance();
        return collectionMenuView;
    }
    private NewsMenuView newsMenuView() {
        if (newsMenuView == null) newsMenuView = NewsMenuView.getInstance();
        return newsMenuView;
    }
    private PlantSelectionMenuView plantSelectionMenuView() {
        if (plantSelectionMenuView == null) plantSelectionMenuView = PlantSelectionMenuView.getInstance();
        return plantSelectionMenuView;
    }
    private ProfileMenuView profileMenuView() {
        if (profileMenuView == null) profileMenuView = ProfileMenuView.getInstance();
        return profileMenuView;
    }
    private SettingsMenuView settingsMenuView() {
        if (settingsMenuView == null) settingsMenuView = SettingsMenuView.getInstance();
        return settingsMenuView;
    }
    private ShopMenuView shopMenuView() {
        if (shopMenuView == null) shopMenuView = ShopMenuView.getInstance();
        return shopMenuView;
    }
    private GreenhouseMenuView greenhouseMenuView() {
        if (greenhouseMenuView == null) greenhouseMenuView = GreenhouseMenuView.getInstance();
        return greenhouseMenuView;
    }
    private TravelLogMenuView travelLogMenuVIew() {
        if (travelLogMenuVIew == null) travelLogMenuVIew = TravelLogMenuView.getInstance();
        return travelLogMenuVIew;
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
        TuiShell shell = TuiShell.getActive();
        if (shell != null) {
            shell.log(message);
        } else {
            System.out.println(message);
        }
    }

    public void displayError(String error) {
        TuiShell shell = TuiShell.getActive();
        if (shell != null) {
            shell.logError(error);
        } else {
            System.err.println(error);
        }
    }

    public void displayCommandResult(CommandResult<?> result) {
        if (result.isSuccess()) {
            displayMessage(result.getMessage());
        } else {
            displayError(result.getMessage());
        }
    }
}
