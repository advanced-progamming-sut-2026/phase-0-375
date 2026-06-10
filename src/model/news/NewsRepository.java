package model.news;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Holds all game news items and tracks which ones the user has read.
 */
public class NewsRepository {
    private final List<NewsItem> allNews = new ArrayList<>();
    private Set<String> readIds;

    public NewsRepository(Set<String> readIds) {
        this.readIds = readIds;
    }

    public List<NewsItem> getAll() {
        return null;
    }

    public List<NewsItem> getUnread() {
        return null;
    }

    public boolean isRead(String id) {
        return false;
    }

    public void markRead(String id) {

    }
}
