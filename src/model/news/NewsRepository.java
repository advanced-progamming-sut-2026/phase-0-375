package model.news;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Holds all game news items and tracks which ones the user has read.
 */
public class NewsRepository {
    private final List<NewsItem> allNews;
    private final Set<String> readIds;

    public NewsRepository(List<NewsItem> allNews, Set<String> readIds) {
        this.allNews = new ArrayList<>(allNews);
        this.readIds = readIds;
    }

    public List<NewsItem> getAll() {
        return Collections.unmodifiableList(allNews);
    }

    public List<NewsItem> getUnread() {
        List<NewsItem> unread = new ArrayList<>();
        for (NewsItem item : allNews) {
            if (!readIds.contains(item.getId())) {
                unread.add(item);
            }
        }
        return unread;
    }

    public boolean isRead(String id) {
        return readIds.contains(id);
    }

    public void markRead(String id) {
        readIds.add(id);
    }
}
