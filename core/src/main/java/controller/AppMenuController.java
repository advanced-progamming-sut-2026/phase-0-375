package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;

public abstract class AppMenuController {

    /**
     * Each concrete controller decides which menu can be entered
     * from its own menu via "menu enter".
     */
    public abstract CommandResult<Void> menuEnter(String menuName);

    /**
     * Each concrete controller decides where "menu exit"
     * takes the user.
     */
    public abstract CommandResult<Void> menuExit();

    /**
     * Common: reads current menu from App singleton.
     */
    public CommandResult<MenuType> menuShowCurrent() {
        MenuType current = App.getInstance().getCurrentMenu();
        return CommandResult.successWithData("Current menu: " + current.name().toLowerCase(), current);
    }
}
