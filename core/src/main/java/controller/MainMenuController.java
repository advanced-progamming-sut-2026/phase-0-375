package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.level.Level;
import model.game.score.ScoreLevelGenerator;

import java.io.IOException;

public class MainMenuController extends AppMenuController {
    private static MainMenuController instance = null;

    private MainMenuController() {}

    public static MainMenuController getInstance() {
        if (instance == null) instance = new MainMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return switch (menuName.toLowerCase()) {
            case "game" -> {
                App.getInstance().setCurrentMenu(MenuType.GAME);
                yield CommandResult.success("Entered game menu.");
            }
            case "settings" -> {
                App.getInstance().setCurrentMenu(MenuType.SETTINGS);
                yield CommandResult.success("Entered settings menu.");
            }
            case "news" -> {
                App.getInstance().setCurrentMenu(MenuType.NEWS);
                yield CommandResult.success("Entered news menu.");
            }
            case "profile" -> {
                App.getInstance().setCurrentMenu(MenuType.PROFILE);
                yield CommandResult.success("Entered profile menu.");
            }
            case "score-game", "score_game", "scoregame" -> enterScoreGame();
            default -> CommandResult.error("Cannot go to '" + menuName + "' from main menu.");
        };
    }

    @Override
    public CommandResult<Void> menuExit() {
        System.exit(0);
        return CommandResult.error("Use 'menu logout' to log out from the main menu.");
    }

    /**
     * Starts today's Myopoint score game: a deterministic daily level where
     * stylish kills earn points (see MyopointTracker). The best score is kept
     * on the profile and shown on the leaderboard.
     */
    public CommandResult<Void> enterScoreGame() {
        Level level;
        try {
            level = ScoreLevelGenerator.createDailyLevel();
        } catch (IOException | RuntimeException buildError) {
            return CommandResult.error("Could not build today's score level: " + buildError.getMessage());
        }
        if (!level.canStart()) {
            return CommandResult.error("Today's score level cannot be started.");
        }

        GameModel model = new GameModel(level);
        PvZGameLoop loop = new PvZGameLoop(model);

        App.getInstance().setCurrentGameModel(model);
        App.getInstance().setCurrentGameLoop(loop);

        level.onStart();
        App.getInstance().setCurrentMenu(MenuType.PLANT_SELECTION);

        return CommandResult.success("Entering today's Myopoint score game. Good luck!");
    }

    public CommandResult<Void> logout() {
        App app = App.getInstance();
        app.getCurrentUser().setStayLoggedIn(false);
        app.getUserRepository().flush();
        app.setCurrentUser(null);
        app.setCurrentMenu(MenuType.REGISTER);
        return CommandResult.success("Logged out successfully.");
    }
}
