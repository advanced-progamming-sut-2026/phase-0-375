package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.Chapter;
import model.enums.LevelType;
import model.enums.MenuType;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.data.level.LevelRegistry;
import model.game.level.DebugSandboxLevel;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.user.User;
import model.user.persistance.UserRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
            if (levelId > nextLevelId && !debugUnlocksAll(user)) {
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

    /**
     * Boots the FrontLawn debug sandbox: no plant selection, no win/lose, empty waves.
     */
    public CommandResult<Void> enterDebugLevel() {
        DebugSandboxLevel level = DebugSandboxLevel.create();
        if (!level.canStart()) {
            return CommandResult.error("Debug sandbox level cannot be started.");
        }

        GameModel model = new GameModel(level);
        PvZGameLoop loop = new PvZGameLoop(model);

        App.getInstance().setCurrentGameModel(model);
        App.getInstance().setCurrentGameLoop(loop);

        level.onStart();
        App.getInstance().setCurrentMenu(MenuType.IN_GAME);

        return CommandResult.success("Entering debug playground.");
    }

    private record LevelResolution(int levelId, Level level) {}

    /** Read-only chapter row for adventure UI. */
    public record ChapterSummary(
            Chapter chapter,
            String displayName,
            boolean unlocked,
            int completedLevels,
            int totalLevels) {}

    /** Read-only level row for chapter level list UI. */
    public record LevelSummary(
            int levelId,
            LevelType levelType,
            boolean unlocked,
            boolean completed) {}

    /**
     * Chapters with lock/progress for the adventure screen.
     * Does not mutate game state.
     */
    public CommandResult<List<ChapterSummary>> listChapters() {
        LevelRegistry registry = ensureRegistry();
        if (registry == null) {
            return errorTyped("Could not load level definitions.");
        }
        User user = App.getInstance().getCurrentUser();
        List<ChapterSummary> summaries = new ArrayList<>();
        for (Chapter chapter : Chapter.values()) {
            List<LevelConfig> configs = registry.getConfigsForChapter(chapter);
            int completed = completedLevels(user, chapter);
            summaries.add(new ChapterSummary(
                    chapter,
                    displayName(chapter),
                    isChapterUnlocked(user, chapter),
                    completed,
                    configs.size()));
        }
        return CommandResult.successWithData("Chapters loaded.", summaries);
    }

    /**
     * Levels in a chapter with lock/completion flags.
     * Does not mutate game state.
     */
    public CommandResult<List<LevelSummary>> listLevels(Chapter chapter) {
        if (chapter == null) {
            return errorTyped("Chapter is required.");
        }
        LevelRegistry registry = ensureRegistry();
        if (registry == null) {
            return errorTyped("Could not load level definitions.");
        }
        User user = App.getInstance().getCurrentUser();
        if (!isChapterUnlocked(user, chapter)) {
            return errorTyped("Chapter '" + displayName(chapter) + "' is not unlocked yet.");
        }
        int nextLevelId = getNextLevelId(user, chapter);
        int completed = completedLevels(user, chapter);
        List<LevelSummary> summaries = new ArrayList<>();
        for (LevelConfig config : registry.getConfigsForChapter(chapter)) {
            int id = config.getLevelId();
            summaries.add(new LevelSummary(
                    id,
                    config.getLevelType(),
                    id <= nextLevelId || debugUnlocksAll(user),
                    id <= completed));
        }
        return CommandResult.successWithData("Levels loaded.", summaries);
    }

    private LevelRegistry ensureRegistry() {
        try {
            return LevelRegistry.getInstance();
        } catch (IllegalStateException e) {
            try {
                LevelRegistry.init("/assets/data/levels/levels.json");
                return LevelRegistry.getInstance();
            } catch (IOException | RuntimeException loadError) {
                return null;
            }
        }
    }

    private static int completedLevels(User user, Chapter chapter) {
        if (user == null || user.getChapterProgress() == null) {
            return 0;
        }
        return user.getChapterProgress().getOrDefault(chapter, 0);
    }

    private static String displayName(Chapter chapter) {
        String raw = chapter.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(raw.length());
        boolean cap = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == ' ') {
                out.append(c);
                cap = true;
            } else if (cap) {
                out.append(Character.toUpperCase(c));
                cap = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
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
        return leaderboard(null, null);
    }

    /**
     * Leaderboard sorted by any column. Sort keys: "username", "score"/"myopoint"
     * (default), "progress", "minigames", "daily-quests" and "quests"
     * (non-daily). Order: "desc" (default) or "asc".
     * Data is loaded from the server when connected.
     */
    public CommandResult<List<User>> leaderboard(String sortKey, String order) {
        String key = sortKey == null ? "score" : sortKey.toLowerCase();
        Comparator<User> comparator = switch (key) {
            case "username", "name", "user" -> Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER);
            case "progress" -> Comparator.comparingInt(GameMenuController::totalProgress);
            case "minigames" -> Comparator.comparingInt(User::getCompletedMiniGames);
            case "daily-quests", "dailyquests", "daily" -> Comparator.comparingInt(User::getCompletedDailyQuests);
            case "quests", "other-quests" -> Comparator.comparingInt(User::getCompletedNonDailyQuests);
            case "score", "myopoint" -> Comparator.comparingInt(User::getHighestMyopoint);
            default -> null;
        };
        if (comparator == null) {
            return errorTyped("Unknown sort column '" + sortKey
                    + "'. Use: username, score, progress, minigames, daily-quests or quests.");
        }
        if (order != null && !order.equalsIgnoreCase("asc") && !order.equalsIgnoreCase("desc")) {
            return errorTyped("Unknown sort order '" + order + "'. Use: asc or desc.");
        }
        if (order == null || order.equalsIgnoreCase("desc")) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(User::getUsername, String.CASE_INSENSITIVE_ORDER);

        List<User> players = fetchLeaderboardUsers();
        if (players == null) {
            return errorTyped("Cannot load leaderboard: server is unreachable.");
        }
        List<User> sorted = players.stream().sorted(comparator).collect(Collectors.toList());
        return CommandResult.successWithData("Leaderboard (" + sorted.size() + " players).", sorted);
    }

    /**
     * Loads public leaderboard rows from the server. Falls back to local
     * {@code findAll()} only when not connected (offline / tests with JsonUserRepository).
     */
    private List<User> fetchLeaderboardUsers() {
        model.network.client.NetworkClient client = App.getInstance().getNetworkClient();
        if (client == null || !client.isConnected()) {
            UserRepository repo = App.getInstance().getUserRepository();
            if (repo == null) return List.of();
            return new ArrayList<>(repo.findAll());
        }

        model.network.enums.LeaderboardCategory category = model.network.enums.LeaderboardCategory.MYOPOINT;
        model.network.packet.user.LeaderboardRequestPacket req =
                new model.network.packet.user.LeaderboardRequestPacket(category);
        java.util.concurrent.atomic.AtomicReference<model.network.packet.user.LeaderboardResponsePacket> ref =
                new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.function.Consumer<model.network.packet.user.LeaderboardResponsePacket> handler = ref::set;

        boolean prev = client.isAutoPostToGdx();
        client.setAutoPostToGdx(false);
        client.registerHandler(model.network.packet.user.LeaderboardResponsePacket.class, handler);
        try {
            if (!client.sendPacket(req)) {
                return null;
            }
            long deadline = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < deadline && ref.get() == null && client.isConnected()) {
                client.pollEvents();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            client.unregisterHandler(model.network.packet.user.LeaderboardResponsePacket.class, handler);
            client.setAutoPostToGdx(prev);
        }

        model.network.packet.user.LeaderboardResponsePacket resp = ref.get();
        if (resp == null || !resp.isSuccess() || resp.getEntries() == null) {
            return null;
        }
        List<User> users = new ArrayList<>();
        for (var entry : resp.getEntries()) {
            users.add(toLeaderboardUser(entry));
        }
        return users;
    }

    private static User toLeaderboardUser(model.network.packet.user.LeaderboardResponsePacket.LeaderboardEntryDto entry) {
        User u = new User();
        u.setUsername(entry.getUsername());
        u.setNickname(entry.getNickname());
        u.setHighestMyopoint(entry.getHighestMyopoint());
        u.setCompletedMiniGames(entry.getCompletedMiniGames());
        u.setCompletedDailyQuests(entry.getCompletedDailyQuests());
        u.setCompletedNonDailyQuests(entry.getCompletedNonDailyQuests());
        if (entry.getChapterProgress() != null && !entry.getChapterProgress().isEmpty()) {
            java.util.Map<Chapter, Integer> progress = new java.util.HashMap<>();
            for (var e : entry.getChapterProgress().entrySet()) {
                try {
                    progress.put(Chapter.valueOf(e.getKey()), e.getValue() == null ? 0 : e.getValue());
                } catch (IllegalArgumentException ignored) {
                }
            }
            u.setChapterProgress(progress);
        }
        return u;
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
            model.user.persistance.UserSync.addCoins(n);
        } else if (type.equalsIgnoreCase("diamond")) {
            model.user.persistance.UserSync.addGems(n);
        } else {
            return CommandResult.error("Type must be 'coin' or 'diamond'.");
        }
        return CommandResult.success("Added " + n + " " + type + "s.");
    }

    private boolean isChapterUnlocked(User user, Chapter chapter) {
        if (debugUnlocksAll(user) || chapter == Chapter.ANCIENT_EGYPT) {
            return true;
        }
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

    private static boolean debugUnlocksAll(User user) {
        return user != null && user.isDebugMode();
    }
}