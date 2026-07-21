package view;

import controller.NewsMenuController;
import controller.result.CommandResult;
import model.command.NewsMenuCommand;
import model.news.NewsItem;

import java.util.List;

public class NewsMenuView extends AppMenuView {
    private static NewsMenuView instance = null;

    public static NewsMenuView getInstance() {
        if (instance == null) instance = new NewsMenuView();
        return instance;
    }

    private final NewsMenuController controller = NewsMenuController.getInstance();

    @Override
    public void processInput(String input) {
        if (NewsMenuCommand.SHOW_UNREAD.matches(input)) {
            showUnread();
        } else if (NewsMenuCommand.SHOW_ALL.matches(input)) {
            showAll();
        } else {
            displayError("Usage:");
            displayError("  menu news show-unread");
            displayError("  menu news show-all");
        }
    }

    public void showUnread() {
        CommandResult<List<NewsItem>> result = controller.showUnread();
        if (!result.isSuccess()) {
            displayError(result.getMessage());
            return;
        }
        List<NewsItem> items = result.getData();
        if (items == null || items.isEmpty()) {
            displayMessage("No unread news.");
            return;
        }
        displayMessage("── Unread News (" + items.size() + ") ──");
        int marked = 0;
        for (NewsItem item : items) {
            displayMessage(formatItem(item, ""));
            controller.markAsRead(item.getId());
            marked++;
        }
        displayMessage("(" + marked + " news item" + (marked == 1 ? "" : "s") + " marked as read.)");
    }

    public void showAll() {
        CommandResult<List<NewsItem>> result = controller.showAll();
        if (!result.isSuccess()) {
            displayError(result.getMessage());
            return;
        }
        List<NewsItem> items = result.getData();
        if (items == null || items.isEmpty()) {
            displayMessage("No news yet. Unlock plants, encounter zombies, or unlock "
                    + "new levels or mini-games to start generating news.");
            return;
        }
        displayMessage("── All News (" + items.size() + ") ──");
        for (NewsItem item : items) {
            String tag = controller.isRead(item.getId()) ? "" : " [NEW]";
            displayMessage(formatItem(item, tag));
        }
    }

    private String formatItem(NewsItem item, String tag) {
        return "[" + item.getPublishDate() + "] [" + item.getCategory().getLabel() + "]" + tag + " "
                + item.getTitle()
                + System.lineSeparator()
                + "    " + item.getBody();
    }
}
