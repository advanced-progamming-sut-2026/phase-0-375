package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.LevelType;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.game.level.LevelConfig;
import model.game.level.RegularLevel;
import model.game.rule.GameRules;
import model.game.wave.EntryRuntime;
import model.game.wave.Wave;
import model.game.wave.WaveZombieEntry;
import model.user.User;
import model.user.persistance.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * TODO: PlantRegistry integration.
     * Stub: creates a minimal RegularLevel with empty config.
     * Once LevelFactory and PlantRegistry are available, use them.
     */
    public CommandResult<Void> enterChapter(String chapterName) {
        Chapter chapter;
        try {
            chapter = Chapter.valueOf(chapterName.toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return CommandResult.error("Unknown chapter: '" + chapterName + "'.");
        }

        // Check if chapter is unlocked (first chapter always unlocked)
        User user = App.getInstance().getCurrentUser();
        if (!isChapterUnlocked(user, chapter)) {
            return CommandResult.error("Chapter '" + chapterName + "' is not unlocked yet.");
        }

        // Build a minimal LevelConfig stub
        GameRules rules = new GameRules(true, true, 150, 1.0, 1, 8,
                Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

        LevelConfig config = new LevelConfig();
        config.setChapter(chapter);
        config.setLevelId(1);
        config.setRows(5);
        config.setColumns(9);
        config.setLevelType(LevelType.NORMAL);
        config.setRules(rules);
        config.setWaves(buildStubWaves());

        RegularLevel level = new RegularLevel(config);
        GameModel model = new GameModel(level);
        PvZGameLoop loop = new PvZGameLoop(model);

        App.getInstance().setCurrentGameModel(model);
        App.getInstance().setCurrentGameLoop(loop);
        App.getInstance().setCurrentMenu(MenuType.PLANT_SELECTION);

        return CommandResult.success("Entering " + chapterName + ".");
    }

    private static List<Wave> buildStubWaves() {
        List<Wave> waves = new ArrayList<>();
        waves.add(new Wave(1, stubEntries(), 5.0f, false, false));
        waves.add(new Wave(2, stubEntries(), 10.0f, false, true));
        return waves;
    }

    private static List<EntryRuntime> stubEntries() {
        List<EntryRuntime> entries = new ArrayList<>();
        entries.add(new EntryRuntime(new WaveZombieEntry()));
        return entries;
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