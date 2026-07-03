package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ProfileMenuCommand implements CLICommand {
    CHANGE_USERNAME("menu profile change-username -u (?<username>\\S+)"),
    CHANGE_NICKNAME("menu profile change-nickname -u (?<nickname>\\S+)"),
    CHANGE_EMAIL("menu profile change-email -e (?<email>\\S+)"),
    CHANGE_PASSWORD("menu profile change-password -p (?<newPassword>\\S+) -o (?<oldPassword>\\S+)"),
    SHOW_INFO("menu profile show-info");

    private final Pattern pattern;
    private Matcher matcher;

    ProfileMenuCommand(String regex) { this.pattern = Pattern.compile(regex); }

    public Pattern getPattern() { return pattern; }
    public Matcher getMatcher() { return matcher; }
    public boolean matches(String query) { matcher = pattern.matcher(query); return matcher.matches(); }
    public String getParameter(String param) { return matcher != null ? matcher.group(param) : null; }
}