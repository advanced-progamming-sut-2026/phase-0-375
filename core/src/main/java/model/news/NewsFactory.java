package model.news;

import java.time.LocalDate;

public final class NewsFactory {

    private NewsFactory() {}

    public static final String PLANT_PREFIX = "plant:";
    public static final String ZOMBIE_PREFIX = "zombie:";
    public static final String MINIGAME_PREFIX = "minigame:";
    public static final String LEVEL_PREFIX = "level:";

    public static String plantNewsId(String plantName) {
        return PLANT_PREFIX + normalize(nonNull(plantName));
    }

    public static String zombieNewsId(String zombieName) {
        return ZOMBIE_PREFIX + normalize(nonNull(zombieName));
    }

    public static String miniGameNewsId(String miniGameId) {
        return MINIGAME_PREFIX + normalize(nonNull(miniGameId));
    }

    public static String levelNewsId(String level) {
        return LEVEL_PREFIX + normalize(nonNull(level));
    }

    /** News for "user unlocked a new plant". */
    public static NewsItem forPlantUnlock(String plantName, LocalDate unlockDate) {
        String safe = nonNull(plantName);
        String id = plantNewsId(safe);
        String title = "New Plant Unlocked: " + safe;
        String body = "You've added '" + safe + "' to your collection. "
                + "Open the collection menu to see its stats, or drop it into your "
                + "next level and give those zombies something to chew on.";
        return new NewsItem(id, NewsCategory.PLANT_UNLOCKED, title, body, unlockDate);
    }

    /** News for "user encountered a new zombie in a level". */
    public static NewsItem forZombieUnlock(String zombieName, LocalDate unlockDate) {
        String safe = nonNull(zombieName);
        String id = zombieNewsId(safe);
        String title = "New Zombie Discovered: " + safe;
        String body = "A '" + safe + "' has shown up in a level. "
                + "Visit the collection menu to learn its strengths, weaknesses, "
                + "and the best plants to use against it.";
        return new NewsItem(id, NewsCategory.ZOMBIE_UNLOCKED, title, body, unlockDate);
    }

    /** News for "user unlocked a new mini-game". */
    public static NewsItem forMiniGameUnlock(String miniGameId, LocalDate unlockDate) {
        String safe = nonNull(miniGameId);
        String id = miniGameNewsId(safe);
        String pretty = prettify(safe);
        String title = "New Mini-Game Unlocked: " + pretty;
        String body = "Mini-Game '" + pretty + "' is now available. "
                + "Head to the game menu whenever you're ready to give it a try.";
        return new NewsItem(id, NewsCategory.MINIGAME_UNLOCKED, title, body, unlockDate);
    }

    /** News for "user unlocked a new level" */
    public static NewsItem forLevelUnlock(String level, LocalDate unlockDate) {
        String safe = nonNull(level);
        String id = levelNewsId(safe);
        String pretty = prettify(safe);
        String title = "New Level Unlocked: " + pretty;
        String body = "Level '" + pretty + "' has been unlocked. "
                + "Go to the game menu and face a new challenge.";
        return new NewsItem(id, NewsCategory.LEVEL_UNLOCKED, title, body, unlockDate);
    }

    // --- Helpers ---

    private static String nonNull(String s) {
        return s == null ? "" : s.trim();
    }

    /** Lower-cases and replaces spaces with underscores. */
    private static String normalize(String key) {
        return key.trim().toLowerCase().replace(' ', '_');
    }

    /** Turns {@code "VASE_BREAKER"} into {@code "Vase Breaker"} for display. */
    private static String prettify(String key) {
        if (key == null || key.isEmpty()) return key;
        String[] parts = key.trim().toLowerCase().split("_");
        StringBuilder pretty = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            pretty.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) pretty.append(part.substring(1));
            pretty.append(' ');
        }
        return pretty.toString().trim();
    }
}