package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum NewsMenuCommand implements CLICommand {
    SHOW_UNREAD("menu news show-unread"),
    SHOW_ALL("menu news show-all");

    private final Pattern pattern;
    private Matcher matcher;

    NewsMenuCommand(String regex) { this.pattern = Pattern.compile(regex); }

    @Override public Pattern getPattern() { return pattern; }
    @Override public Matcher getMatcher() { return matcher; }
    @Override public boolean matches(String query) { matcher = pattern.matcher(query); return matcher.matches(); }
    @Override public String getParameter(String param) { return matcher != null ? matcher.group(param) : null; }
}