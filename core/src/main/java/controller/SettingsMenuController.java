package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.user.User;

/**
 * Settings menu: difficulty plus game speed, lawn grid, debug mode,
 * and audio volumes.
 */
public class SettingsMenuController extends AppMenuController {
    private static SettingsMenuController instance = null;

    private SettingsMenuController() {}

    public static SettingsMenuController getInstance() {
        if (instance == null) instance = new SettingsMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return CommandResult.error("No menus reachable from settings.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.MAIN);
        return CommandResult.success("Returned to main menu.");
    }

    public CommandResult<User> showSettings() {
        User user = requireUser();
        if (user == null) {
            return CommandResult.errorTyped("No user logged in.");
        }
        return CommandResult.successWithData(formatSummary(user), user);
    }

    public CommandResult<Void> changeDifficulty(int level) {
        if (level < 1 || level > 5) {
            return CommandResult.error("Difficulty must be between 1 and 5.");
        }
        User user = requireUser();
        if (user == null) {
            return CommandResult.error("No user logged in.");
        }
        user.setDifficultyLevel(level);
        persist();
        return CommandResult.success("Difficulty set to " + level + ".");
    }

    public CommandResult<Void> changeGameSpeed(int speed) {
        if (speed < 1 || speed > 3) {
            return CommandResult.error("Game speed must be between 1 and 3.");
        }
        User user = requireUser();
        if (user == null) {
            return CommandResult.error("No user logged in.");
        }
        user.setGameSpeed(speed);
        persist();
        return CommandResult.success("Game speed set to " + speed + "x.");
    }

    public CommandResult<Void> setShowLawnGrid(boolean enabled) {
        User user = requireUser();
        if (user == null) {
            return CommandResult.error("No user logged in.");
        }
        user.setShowLawnGrid(enabled);
        persist();
        return CommandResult.success(enabled
                ? "Lawn grid enabled (shown in red during gameplay)."
                : "Lawn grid disabled.");
    }

    public CommandResult<Void> setDebugMode(boolean enabled) {
        User user = requireUser();
        if (user == null) {
            return CommandResult.error("No user logged in.");
        }
        user.setDebugMode(enabled);
        persist();
        return CommandResult.success(enabled
                ? "Debug mode enabled (cheat controls appear during gameplay)."
                : "Debug mode disabled.");
    }

    public CommandResult<Void> setMusicVolume(float volume) {
        if (volume < 0f || volume > 1f) {
            return CommandResult.error("Music volume must be between 0 and 1.");
        }
        User user = requireUser();
        if (user == null) {
            return CommandResult.error("No user logged in.");
        }
        user.setMusicVolume(volume);
        persist();
        return CommandResult.success("Music volume set to " + percent(volume) + "%.");
    }

    public CommandResult<Void> setSfxVolume(float volume) {
        if (volume < 0f || volume > 1f) {
            return CommandResult.error("SFX volume must be between 0 and 1.");
        }
        User user = requireUser();
        if (user == null) {
            return CommandResult.error("No user logged in.");
        }
        user.setSfxVolume(volume);
        persist();
        return CommandResult.success("SFX volume set to " + percent(volume) + "%.");
    }

    private static User requireUser() {
        return App.getInstance().getCurrentUser();
    }

    private static void persist() {
        App.getInstance().getUserRepository().flush();
    }

    private static String formatSummary(User user) {
        return "Difficulty " + user.getDifficultyLevel()
                + " | Speed " + user.getGameSpeed() + "x"
                + " | Grid " + onOff(user.isShowLawnGrid())
                + " | Debug " + onOff(user.isDebugMode())
                + " | Music " + percent(user.getMusicVolume()) + "%"
                + " | SFX " + percent(user.getSfxVolume()) + "%";
    }

    private static String onOff(boolean value) {
        return value ? "on" : "off";
    }

    private static int percent(float volume) {
        return Math.round(volume * 100f);
    }
}
