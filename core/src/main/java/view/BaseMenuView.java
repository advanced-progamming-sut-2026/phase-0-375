package view;

import java.util.Scanner;

public abstract class BaseMenuView {
    protected Scanner scanner;

    public abstract void processInput(String input);

    public void displayMessage(String message) { }
    public void displayError(String error) { }

    public void menuEnter(String menuName) { }
    public void menuExit() { }
    public void menuShowCurrent() { }
}
