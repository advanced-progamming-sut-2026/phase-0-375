package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.data.level.LevelRegistry;
import model.game.level.Level;
import model.user.User;
import model.user.persistance.UserRepository;

import java.io.IOException;
import java.util.List;

public class GameMenuController extends AppMenuController {
    private static GameMenuController instance = null;

    private GameMenuController() {}

    public static GameMenuController getInstance() {
        if (instance == null) instance = new GameMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        if (menuName.equalsIgnoreCase("collection")) {
            App.getInstance().setCurrentMenu(MenuType.COLLECTION);
            return CommandResult.success("Entered collection menu.");
        }
        return CommandResult.error("Cannot go to '" + menuName + "' from game menu.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.MAIN);
        return CommandResult.success("Returned to main menu.");
    }

    public CommandResult<Void> enterChapter(String chapterName) {
        Chapter chapter;
        try {
            chapter = Chapter.valueOf(chapterName.toUpperCase().replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return CommandResult.error("Unknown chapter: '" + chapterName + "'.");
        }

        User user = App.getInstance().getCurrentUser();
        if (!isChapterUnlocked(user, chapter)) {
            return CommandResult.error("Chapter '" + chapterName + "' is not unlocked yet.");
        }

        LevelRegistry registry;
        try {
            registry = LevelRegistry.getInstance();
        } catch (IllegalStateException e) {
            try {
                LevelRegistry.init("/assets/data/levels/levels.json");
                registry = LevelRegistry.getInstance();
            } catch (IOException | RuntimeException loadError) {
                return CommandResult.error("Could not load level definitions: " + loadError.getMessage());
            }
        }

        int levelId = getNextLevelId(user, chapter);
        Level level = registry.createLevel(chapter, levelId);
        if (level == null) {
            return CommandResult.error("No level definition found for " + chapter + " level " + levelId + ".");
        }

        if (!level.canStart()) {
            return CommandResult.error("Level " + levelId + " of " + chapter + " cannot be started.");
        }

        GameModel model = new GameModel(level);
        PvZGameLoop loop = new PvZGameLoop(model);

        App.getInstance().setCurrentGameModel(model);
        App.getInstance().setCurrentGameLoop(loop);

        level.onStart();
        App.getInstance().setCurrentMenu(MenuType.PLANT_SELECTION);

        return CommandResult.success("Entering " + chapterName + " level " + levelId + ".");
    }

    private int getNextLevelId(User user, Chapter chapter) {
        if (user == null || user.getChapterProgress() == null) return 1;
        return user.getChapterProgress().getOrDefault(chapter, 0) + 1;
    }

    public CommandResult<Void> greenhouse() {
        App.getInstance().setCurrentMenu(MenuType.GREENHOUSE);
        return CommandResult.success("Entered greenhouse.");
    }

    public CommandResult<Void> travelLog() {
        App.getInstance().setCurrentMenu(MenuType.TRAVEL_LOG);
        return CommandResult.success("Entered travel log.");
    }

    public CommandResult<List<User>> leaderboard() {
        UserRepository repo = App.getInstance().getUserRepository();
        List<User> sorted = repo.findAllOrderByMyopointDesc();
        return CommandResult.successWithData("Leaderboard (" + sorted.size() + " players).", sorted);
    }

    public CommandResult<Void> coinWallet() {
        int coins = App.getInstance().getCurrentUser().getCoins();
        return CommandResult.success("You have " + coins + " coins.");
    }

    public CommandResult<Void> gemWallet() {
        int gems = App.getInstance().getCurrentUser().getGems();
        return CommandResult.success("You have " + gems + " gems.");
    }

    public CommandResult<Void> cheatAdd(int n, String type) {
        User user = App.getInstance().getCurrentUser();
        if (type.equalsIgnoreCase("coin")) {
            user.setCoins(user.getCoins() + n);
        } else if (type.equalsIgnoreCase("diamond")) {
            user.setGems(user.getGems() + n);
        } else {
            return CommandResult.error("Type must be 'coin' or 'diamond'.");
        }
        App.getInstance().getUserRepository().flush();
        return CommandResult.success("Added " + n + " " + type + "s.");
    }

    private boolean isChapterUnlocked(User user, Chapter chapter) {
        if (chapter == Chapter.ANCIENT_EGYPT) return true;
        Chapter[] chapters = Chapter.values();
        for (int i = 1; i < chapters.length; i++) {
            if (chapters[i] == chapter) {
                Chapter prev = chapters[i - 1];
                return user.getChapterProgress() != null
                        && user.getChapterProgress().getOrDefault(prev, 0) >= 1;
            }
        }
        return false;
    }
}