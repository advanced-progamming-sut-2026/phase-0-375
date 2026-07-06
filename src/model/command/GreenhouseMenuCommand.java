package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GreenhouseMenuCommand implements CLICommand {
    SHOW_GREENHOUSE("show greenhouse"),
    PLANT_POT("plant pot at \\((?<x>\\d+),\\s*(?<y>\\d+)\\)"),
    COLLECT("collect \\((?<x>\\d+),\\s*(?<y>\\d+)\\)"),
    GROW("grow \\((?<x>\\d+),\\s*(?<y>\\d+)\\)"),
    ENTER_SHOP("enter shop");

    private final Pattern pattern;
    private Matcher matcher;

    GreenhouseMenuCommand(String regex) { this.pattern = Pattern.compile(regex); }

    @Override public Pattern getPattern() { return pattern; }
    @Override public Matcher getMatcher() { return matcher; }
    @Override public boolean matches(String query) { matcher = pattern.matcher(query); return matcher.matches(); }
    @Override public String getParameter(String param) { return matcher != null ? matcher.group(param) : null; }
}
