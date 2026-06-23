package controller;

import controller.result.CommandResult;

public class NewsMenuController extends AppMenuController {
    private static NewsMenuController instance = null;

    private  NewsMenuController() {}

    public static NewsMenuController getInstance() {
        if (instance == null) instance = new NewsMenuController();
        return instance;
    }

    public CommandResult<Object> showUnread() { return null; }
    public CommandResult<Object> showAll() { return null; }
}
