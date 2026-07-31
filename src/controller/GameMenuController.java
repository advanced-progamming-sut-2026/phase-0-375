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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    public CommandResult<Void> enterChapter(String chapterName, Integer levelId) {
        CommandResult<LevelResolution> resolved = resolveLevel(chapterName, levelId);
        if (!resolved.isSuccess()) {
            return CommandResult.error(resolved.getMessage());
        }
        return startLevel(chapterName, resolved.getData());
    }

    private CommandResult<LevelResolution> resolveLevel(String chapterName, Integer levelId) {
        Chapter chapter;
        try {
            chapter = Chapter.valueOf(chapterName.toUpperCase().replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return CommandResult.errorTyped("Unknown chapter: '" + chapterName + "'.");
        }

        User user = App.getInstance().getCurrentUser();
        if (!isChapterUnlocked(user, chapter)) {
            return CommandResult.errorTyped("Chapter '" + chapterName + "' is not unlocked yet.");
        }

        LevelRegistry registry;
        try {
            registry = LevelRegistry.getInstance();
        } catch (IllegalStateException e) {
            try {
                LevelRegistry.init("/assets/data/levels/levels.json");
                registry = LevelRegistry.getInstance();
            } catch (IOException | RuntimeException loadError) {
                return CommandResult.errorTyped("Could not load level definitions: " + loadError.getMessage());
            }
        }

        int nextLevelId = getNextLevelId(user, chapter);
        int targetLevelId = levelId == null ? nextLevelId : levelId;

        if (levelId != null) {
            if (!registry.hasLevel(chapter, levelId)) {
                return CommandResult.errorTyped("Level " + levelId + " does not exist in " + chapter + ".");
            }
            if (levelId > nextLevelId) {
                return CommandResult.errorTyped("Level " + levelId + " is locked. Beat level "
                        + (nextLevelId - 1) + " first (next unlocked: " + nextLevelId + ").");
            }
        }

        Level level = registry.createLevel(chapter, targetLevelId);
        if (level == null) {
            return CommandResult.errorTyped(
                    "No level definition found for " + chapter + " level " + targetLevelId + ".");
        }
        if (!level.canStart()) {
            return CommandResult.errorTyped("Level " + targetLevelId + " of " + chapter + " cannot be started.");
        }
        return CommandResult.successWithData(
                "Resolved " + chapter + " level " + targetLevelId + ".",
                new LevelResolution(targetLevelId, level));
    }

    private CommandResult<Void> startLevel(String chapterName, LevelResolution resolution) {
        Level level = resolution.level();

        GameModel model = new GameModel(level);
        PvZGameLoop loop = new PvZGameLoop(model);

        App.getInstance().setCurrentGameModel(model);
        App.getInstance().setCurrentGameLoop(loop);

        level.onStart();
        App.getInstance().setCurrentMenu(MenuType.PLANT_SELECTION);

        return CommandResult.success("Entering " + chapterName + " level " + resolution.levelId() + ".");
    }

    private record LevelResolution(int levelId, Level level) {}

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
        return leaderboard(null, null);
    }

    /**
     * Leaderboard sorted by any column. Sort keys: "score"/"myopoint"
     * (default), "progress", "minigames", "daily-quests" and "quests"
     * (non-daily). Order: "desc" (default) or "asc".
     */
    public CommandResult<List<User>> leaderboard(String sortKey, String order) {
        String key = sortKey == null ? "score" : sortKey.toLowerCase();
        Comparator<User> comparator = switch (key) {
            case "progress" -> Comparator.comparingInt(GameMenuController::totalProgress);
            case "minigames" -> Comparator.comparingInt(User::getCompletedMiniGames);
            case "daily-quests", "dailyquests", "daily" -> Comparator.comparingInt(User::getCompletedDailyQuests);
            case "quests", "other-quests" -> Comparator.comparingInt(User::getCompletedNonDailyQuests);
            case "score", "myopoint" -> Comparator.comparingInt(User::getHighestMyopoint);
            default -> null;
        };
        if (comparator == null) {
            return errorTyped("Unknown sort column '" + sortKey
                    + "'. Use: score, progress, minigames, daily-quests or quests.");
        }
        if (order != null && !order.equalsIgnoreCase("asc") && !order.equalsIgnoreCase("desc")) {
            return errorTyped("Unknown sort order '" + order + "'. Use: asc or desc.");
        }
        if (order == null || order.equalsIgnoreCase("desc")) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(User::getUsername, String.CASE_INSENSITIVE_ORDER);

        UserRepository repo = App.getInstance().getUserRepository();
        List<User> sorted = repo.findAll().stream().sorted(comparator).collect(Collectors.toList());
        return CommandResult.successWithData("Leaderboard (" + sorted.size() + " players).", sorted);
    }

    /** Total chapter progress across all chapters (leaderboard sort helper). */
    private static int totalProgress(User user) {
        if (user.getChapterProgress() == null) return 0;
        int total = 0;
        for (Integer level : user.getChapterProgress().values()) {
            total += level == null ? 0 : level;
        }
        return total;
    }

    /** Adapts a Void error result to a typed one for data-carrying commands. */
    @SuppressWarnings("unchecked")
    private static <T> CommandResult<T> errorTyped(String message) {
        return (CommandResult<T>) CommandResult.error(message);
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