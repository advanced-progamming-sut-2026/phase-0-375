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
            displayError("Usage: menu news show-unread | menu news show-all");
        }
    }

    public void showUnread() {
        CommandResult<List<NewsItem>> result = controller.showUnread();
        if (result.isSuccess()) {
            List<NewsItem> items = result.getData();
            if (items.isEmpty()) {
                displayMessage("No unread news.");
                return;
            }
            displayMessage("── Unread News ──");
            for (NewsItem item : items) {
                displayMessage("  [" + item.getPublishDate() + "] " + item.getTitle());
                displayMessage("    " + item.getBody());
                controller.markAsRead(item.getId());
            }
        } else {
            displayError(result.getMessage());
        }
    }

    public void showAll() {
        CommandResult<List<NewsItem>> result = controller.showAll();
        if (result.isSuccess()) {
            List<NewsItem> items = result.getData();
            if (items.isEmpty()) {
                displayMessage("No news.");
                return;
            }
            displayMessage("── All News ──");
            for (NewsItem item : items) {
                String tag = controller.isRead(item.getId()) ? "" : " [NEW]";
                displayMessage("  [" + item.getPublishDate() + "]" + tag + " " + item.getTitle());
                displayMessage("    " + item.getBody());
            }
        } else {
            displayError(result.getMessage());
        }
    }
}
