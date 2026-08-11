package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum SettingsMenuCommand implements CLICommand {
    CHANGE_DIFFICULTY("menu settings change-difficulty -l (?<difficultyLevel>\\d+)"),
    CHANGE_GAME_SPEED("menu settings change-game-speed -s (?<gameSpeed>\\d+)"),
    SET_LAWN_GRID("menu settings set-lawn-grid -v (?<enabled>true|false)"),
    SET_DEBUG_MODE("menu settings set-debug-mode -v (?<enabled>true|false)"),
    SET_MUSIC_VOLUME("menu settings set-music-volume -v (?<volume>\\d+(\\.\\d+)?)"),
    SET_SFX_VOLUME("menu settings set-sfx-volume -v (?<volume>\\d+(\\.\\d+)?)"),
    SHOW("menu settings show");

    private final Pattern pattern;
    private Matcher matcher;

    SettingsMenuCommand(String regex) { this.pattern = Pattern.compile(regex); }

    @Override public Pattern getPattern() { return pattern; }
    @Override public Matcher getMatcher() { return matcher; }
    @Override public boolean matches(String query) { matcher = pattern.matcher(query); return matcher.matches(); }
    @Override public String getParameter(String param) { return matcher != null ? matcher.group(param) : null; }
}
