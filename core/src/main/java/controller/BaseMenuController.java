package controller;

import controller.result.CommandResult;

public class BaseMenuController {
    public CommandResult<Void> menuEnter(String menuName) { return null; }
    public CommandResult<Void> menuExit() { return null; }
    public CommandResult<String> menuShowCurrent() { return null; }
}