package model.news;

import model.user.User;

import java.time.LocalDate;
import java.util.*;

/**
 * Holds all game news items and tracks which ones the user has read.
 */
public class NewsRepository {
    private final List<NewsItem> allNews;
    private final Set<String> readIds;

    public NewsRepository(List<NewsItem> allNews, Set<String> readIds) {
        this.allNews = new ArrayList<>(allNews);
        this.readIds = readIds != null ? readIds : new HashSet<>();
    }

    /** Builds the news repository for a user. */
    public static NewsRepository fromUser(User user) {
        if (user == null) {
            return new NewsRepository(Collections.emptyList(), Collections.emptySet());
        }

        Set<String> readIds = new HashSet<>();
        if (user.getReadNews() != null) {
            readIds.addAll(user.getReadNews());
        }

        List<NewsItem> items = new ArrayList<>();

        if (user.getUnlockedPlants() != null) {
            for (String plantName : user.getUnlockedPlants()) {
                if (plantName != null && !plantName.trim().isEmpty()) {
                    LocalDate date = user.rememberNewsPublishDate(NewsFactory.plantNewsId(plantName));
                    items.add(NewsFactory.forPlantUnlock(plantName, date));
                }
            }
        }
        if (user.getUnlockedZombies() != null) {
            for (String zombieName : user.getUnlockedZombies()) {
                if (zombieName != null && !zombieName.trim().isEmpty()) {
                    LocalDate date = user.rememberNewsPublishDate(NewsFactory.zombieNewsId(zombieName));
                    items.add(NewsFactory.forZombieUnlock(zombieName, date));
                }
            }
        }
        if (user.getUnlockedMiniGames() != null) {
            for (String miniGame : user.getUnlockedMiniGames()) {
                if (miniGame != null && !miniGame.trim().isEmpty()) {
                    LocalDate date = user.rememberNewsPublishDate(NewsFactory.miniGameNewsId(miniGame));
                    items.add(NewsFactory.forMiniGameUnlock(miniGame, date));
                }
            }
        }

        if (user.getUnlockedLevels() != null) {
            for (String level : user.getUnlockedLevels()) {
                if (level != null && !level.trim().isEmpty()) {
                    LocalDate date = user.rememberNewsPublishDate(NewsFactory.levelNewsId(level));
                    items.add(NewsFactory.forLevelUnlock(level, date));
                }
            }
        }

        items.sort(Comparator
                .comparing(NewsItem::getPublishDate).reversed()
                .thenComparingInt((NewsItem a) -> a.getCategory().ordinal())
                .thenComparing(NewsItem::getId));

        return new NewsRepository(items, readIds);
    }

    /** All news items visible to this user, read or unread. */
    public List<NewsItem> getAll() {
        return Collections.unmodifiableList(allNews);
    }

    /** Subset of news items the user has not read yet. */
    public List<NewsItem> getUnread() {
        List<NewsItem> unread = new ArrayList<>();
        for (NewsItem item : allNews) {
            if (!readIds.contains(item.getId())) {
                unread.add(item);
            }
        }
        return unread;
    }

    /** @return number of unread news items. */
    public int countUnread() {
        int n = 0;
        for (NewsItem item : allNews) {
            if (!readIds.contains(item.getId())) {
                n++;
            }
        }
        return n;
    }

    public boolean isRead(String id) {
        return id != null && readIds.contains(id);
    }

    public void markRead(String id) {
        if (id != null) {
            readIds.add(id);
        }
    }
}
