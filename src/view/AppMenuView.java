package view;

import controller.AppMenuController;
import controller.result.CommandResult;
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

    private final MainMenuView mainMenuView = MainMenuView.getInstance();
    private final RegisterMenuView registerMenuView = RegisterMenuView.getInstance();
    private final LoginMenuView loginMenuView = LoginMenuView.getInstance();
    private final CollectionMenuView collectionMenuView = CollectionMenuView.getInstance();
    private final GameMenuView gameMenuView = GameMenuView.getInstance();
    private final GameplayMenuView gameplayMenuView = GameplayMenuView.getInstance();
    private final NewsMenuView newsMenuView = NewsMenuView.getInstance();
    private final PlantSelectionMenuView plantSelectionMenuView = PlantSelectionMenuView.getInstance();
    private final ProfileMenuView profileMenuView = ProfileMenuView.getInstance();
    private final SettingsMenuView settingsMenuView = SettingsMenuView.getInstance();
    private final ShopMenuView shopMenuView = ShopMenuView.getInstance();
    private final GreenhouseMenuView greenhouseMenuView = GreenhouseMenuView.getInstance();
    private final TravelLogMenuVIew travelLogMenuVIew = TravelLogMenuVIew.getInstance();

    private final AppMenuController appController = AppMenuController.getInstance();
    private final Scanner scanner = new Scanner(System.in);

    private boolean running = true;

    protected void pause() {
        running = false;
    }

    public void run() {
        while (running && scanner.hasNextLine()) {
            String command = scanner.nextLine();

            if (CommonCommand.MENU_ENTER.matches(command)) {
                String menu = CommonCommand.MENU_ENTER.getParameter("menu_name");
                menuEnter(menu);
                continue;
            } else if (CommonCommand.MENU_EXIT.matches(command)) {
                menuExit();
                continue;
            } else if (CommonCommand.MENU_SHOW_CURRENT.matches(command)) {
                menuShowCurrent();
                continue;
            }

            MenuType menuType = appController.menuShowCurrent().getData();

            switch (menuType) {
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

            scanner.close();
        }
    }

    public void processInput(String input) {}

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
        CommandResult<Void> result = appController.menuExit();
        if (!result.isSuccess()) {
            displayError(result.getMessage());
            return;
        }
        System.out.println(result.getMessage());
    }
    public void menuShowCurrent() {
        CommandResult<MenuType> result = appController.menuShowCurrent();
        if (!result.isSuccess()) {
            System.err.println(result.getMessage());
            return;
        }
        System.out.println("Current menu: " + result.getMessage());
    }
}
