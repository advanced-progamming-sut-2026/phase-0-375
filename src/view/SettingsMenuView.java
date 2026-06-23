package view;

public class SettingsMenuView extends AppMenuView {
    private static SettingsMenuView instance;

    public static SettingsMenuView getInstance() {
        if (instance == null) instance = new SettingsMenuView();
        return instance;
    }

//    private SettingsMenuController controller;

    @Override
    public void processInput(String input) {}

    public void changeDifficulty(int difficultyLevel) { }
}
