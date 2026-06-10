package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum CommonCommand implements CLICommand {
    MENU_ENTER("menu enter (?<menu_name>\\S+)"),
    MENU_SHOW_CURRENT("menu show current"),
    MENU_EXIT("menu exit");

    private final Pattern pattern;
    private Matcher matcher;

    CommonCommand(String regex) { this.pattern = Pattern.compile(regex); }

    @Override public Pattern getPattern() { return pattern; }
    @Override public Matcher getMatcher() { return matcher; }
    @Override public boolean matches(String query) { matcher = pattern.matcher(query); return matcher.matches(); }
    @Override public String getParameter(String param) { return matcher != null ? matcher.group(param) : null; }
}
