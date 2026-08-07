package model.news;

import java.time.LocalDate;
import java.util.Objects;

public class NewsItem {
    private final String id;
    private final NewsCategory category;
    private final String title;
    private final String body;
    private final LocalDate publishDate;

    public NewsItem(String id, NewsCategory category, String title, String body, LocalDate publishDate) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.body = body;
        this.publishDate = publishDate;
    }

    public String getId() {
        return id;
    }

    public NewsCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public LocalDate getPublishDate() {
        return publishDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NewsItem other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}