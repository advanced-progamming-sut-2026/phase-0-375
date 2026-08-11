package view;

import controller.SettingsMenuController;
import controller.result.CommandResult;
import model.command.SettingsMenuCommand;
import model.user.User;

public class SettingsMenuView extends AppMenuView {
    private static SettingsMenuView instance;

    public static SettingsMenuView getInstance() {
        if (instance == null) instance = new SettingsMenuView();
        return instance;
    }

    private final SettingsMenuController controller = SettingsMenuController.getInstance();

    @Override
    public void processInput(String input) {
        if (SettingsMenuCommand.CHANGE_DIFFICULTY.matches(input)) {
            int level = Integer.parseInt(
                    SettingsMenuCommand.CHANGE_DIFFICULTY.getParameter("difficultyLevel"));
            changeDifficulty(level);
        } else if (SettingsMenuCommand.CHANGE_GAME_SPEED.matches(input)) {
            int speed = Integer.parseInt(
                    SettingsMenuCommand.CHANGE_GAME_SPEED.getParameter("gameSpeed"));
            changeGameSpeed(speed);
        } else if (SettingsMenuCommand.SET_LAWN_GRID.matches(input)) {
            boolean enabled = Boolean.parseBoolean(
                    SettingsMenuCommand.SET_LAWN_GRID.getParameter("enabled"));
            setShowLawnGrid(enabled);
        } else if (SettingsMenuCommand.SET_DEBUG_MODE.matches(input)) {
            boolean enabled = Boolean.parseBoolean(
                    SettingsMenuCommand.SET_DEBUG_MODE.getParameter("enabled"));
            setDebugMode(enabled);
        } else if (SettingsMenuCommand.SET_MUSIC_VOLUME.matches(input)) {
            float volume = parseVolume(
                    SettingsMenuCommand.SET_MUSIC_VOLUME.getParameter("volume"));
            setMusicVolume(volume);
        } else if (SettingsMenuCommand.SET_SFX_VOLUME.matches(input)) {
            float volume = parseVolume(
                    SettingsMenuCommand.SET_SFX_VOLUME.getParameter("volume"));
            setSfxVolume(volume);
        } else if (SettingsMenuCommand.SHOW.matches(input)) {
            showSettings();
        } else {
            displayHelp();
        }
    }

    public void changeDifficulty(int difficultyLevel) {
        CommandResult<Void> result = controller.changeDifficulty(difficultyLevel);
        displayCommandResult(result);
    }

    public void changeGameSpeed(int gameSpeed) {
        CommandResult<Void> result = controller.changeGameSpeed(gameSpeed);
        displayCommandResult(result);
    }

    public void setShowLawnGrid(boolean enabled) {
        CommandResult<Void> result = controller.setShowLawnGrid(enabled);
        displayCommandResult(result);
    }

    public void setDebugMode(boolean enabled) {
        CommandResult<Void> result = controller.setDebugMode(enabled);
        displayCommandResult(result);
    }

    public void setMusicVolume(float volume) {
        CommandResult<Void> result = controller.setMusicVolume(volume);
        displayCommandResult(result);
    }

    public void setSfxVolume(float volume) {
        CommandResult<Void> result = controller.setSfxVolume(volume);
        displayCommandResult(result);
    }

    public void showSettings() {
        CommandResult<User> result = controller.showSettings();
        displayCommandResult(result);
    }

    /** Accepts either a 0–1 fraction or a 0–100 percentage. */
    private static float parseVolume(String raw) {
        float value = Float.parseFloat(raw);
        if (value > 1f) {
            return value / 100f;
        }
        return value;
    }

    private void displayHelp() {
        displayError("Usage:");
        displayError("  menu settings show");
        displayError("  menu settings change-difficulty -l <1-5>");
        displayError("  menu settings change-game-speed -s <1-3>");
        displayError("  menu settings set-lawn-grid -v <true|false>");
        displayError("  menu settings set-debug-mode -v <true|false>");
        displayError("  menu settings set-music-volume -v <0-1|0-100>");
        displayError("  menu settings set-sfx-volume -v <0-1|0-100>");
    }
}
