package model.app;

import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.user.User;
import model.user.persistance.JsonUserRepository;
import model.user.persistance.UserRepository;
import view.tui.TuiShell;

public class App {
    private static App instance;

    private User currentUser;
    private MenuType currentMenu;
    private UserRepository userRepository;

    // Game session state — set when starting a level, cleared on exit
    private GameModel currentGameModel;
    private PvZGameLoop currentGameLoop;

    private App() {
        this.currentUser = null;
        this.currentMenu = MenuType.REGISTER;
        this.userRepository = new JsonUserRepository();
        this.userRepository.loadAll();

        // Check for stay-logged-in user
        var stayLoggedIn = this.userRepository.findStayLoggedInUser();
        if (stayLoggedIn.isPresent()) {
            this.currentUser = stayLoggedIn.get();
            this.currentMenu = MenuType.MAIN;
        }
    }

    public static App getInstance() {
        if (instance == null) instance = new App();
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public MenuType getCurrentMenu() {
        return currentMenu;
    }

    public void setCurrentMenu(MenuType currentMenu) {
        this.currentMenu = currentMenu;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public GameModel getCurrentGameModel() {
        return currentGameModel;
    }

    public void setCurrentGameModel(GameModel currentGameModel) {
        this.currentGameModel = currentGameModel;
    }

    public PvZGameLoop getCurrentGameLoop() {
        return currentGameLoop;
    }

    public void setCurrentGameLoop(PvZGameLoop currentGameLoop) {
        this.currentGameLoop = currentGameLoop;
    }

    public static void logToShell(String message) {
        TuiShell shell = TuiShell.getActive();
        if (shell != null) shell.log(message);
        else System.out.println(message);
    }
}
