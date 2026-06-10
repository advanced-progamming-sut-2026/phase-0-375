package model.news;

import java.time.LocalDate;

public class NewsItem {
    private final String id;
    private final String title;
    private final String body;
    private final LocalDate publishDate;

    public NewsItem(String id, String title, String body, LocalDate publishDate) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.publishDate = publishDate;
    }

    public String getId() {
        return id;
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
}
