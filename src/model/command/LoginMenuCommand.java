package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum LoginMenuCommand implements CLICommand {
    LOGIN("login\\s+-u\\s+(?<username>\\S+)\\s+-p\\s+(?<password>\\S+)(\\s+-stay-logged-in)?"),
    FORGET_PASSWORD("forget password -u (?<username>\\S+) -e (?<email>\\S+)"),
    ANSWER("answer -a (?<answer>\\S+)");

    private final Pattern pattern;
    private Matcher matcher;

    LoginMenuCommand(String regex) { this.pattern = Pattern.compile(regex); }

    @Override public Pattern getPattern() { return pattern; }
    @Override public Matcher getMatcher() { return matcher; }
    @Override public boolean matches(String query) { matcher = pattern.matcher(query); return matcher.matches(); }
    @Override public String getParameter(String param) { return matcher != null ? matcher.group(param) : null; }
}
