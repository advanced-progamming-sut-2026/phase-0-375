package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.news.NewsItem;
import model.news.NewsRepository;
import model.user.User;

import java.util.ArrayList;
import java.util.List;

public class NewsMenuController extends AppMenuController {
    private static NewsMenuController instance = null;

    private NewsMenuController() {}

    public static NewsMenuController getInstance() {
        if (instance == null) instance = new NewsMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return CommandResult.error("No menus reachable from news.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.MAIN);
        return CommandResult.success("Returned to main menu.");
    }

    public CommandResult<List<NewsItem>> showUnread() {
        NewsRepository repo = buildRepo();
        List<NewsItem> unread = repo.getUnread();
        return CommandResult.successWithData("Unread news (" + unread.size() + ").", unread);
    }

    public CommandResult<List<NewsItem>> showAll() {
        NewsRepository repo = buildRepo();
        List<NewsItem> all = repo.getAll();
        return CommandResult.successWithData("All news (" + all.size() + ").", all);
    }

    public void markAsRead(String newsId) {
        User user = App.getInstance().getCurrentUser();
        if (user == null || newsId == null) {
            return;
        }
        if (user.getReadNews() == null) {
            user.setReadNews(new ArrayList<>());
        }
        if (!user.getReadNews().contains(newsId)) {
            App.getInstance().getUserRepository().markNewsAsRead(user.getUsername(), newsId);
        }
    }

    public boolean isRead(String newsId) {
        User user = App.getInstance().getCurrentUser();
        return user != null
                && user.getReadNews() != null
                && user.getReadNews().contains(newsId);
    }

    public int countUnread() {
        return buildRepo().countUnread();
    }

    private NewsRepository buildRepo() {
        User user = App.getInstance().getCurrentUser();
        int before = user != null && user.getNewsPublishDates() != null
                ? user.getNewsPublishDates().size() : 0;
        NewsRepository repo = NewsRepository.fromUser(user);
        int after = user != null && user.getNewsPublishDates() != null
                ? user.getNewsPublishDates().size() : 0;
        if (after > before) {
            model.user.persistance.UserSync.flushIfLocal();
        }
        return repo;
    }
}
