package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum PlantSelectionMenuCommand implements CLICommand {
    SHOW_ALL_PLANTS("show all plants"),
    SHOW_AVAILABLE_PLANTS("show available plants"),
    ADD_PLANT("add plant -t (?<type>.+?)"),
    REMOVE_PLANT("remove plant -t (?<type>.+?)"),
    BOOST_PLANT("boost plant -t (?<type>.+?)"),
    START_GAME("start game");

    private final Pattern pattern;
    private Matcher matcher;

    PlantSelectionMenuCommand(String regex) { this.pattern = Pattern.compile(regex); }

    @Override public Pattern getPattern() { return pattern; }
    @Override public Matcher getMatcher() { return matcher; }
    @Override public boolean matches(String query) { matcher = pattern.matcher(query); return matcher.matches(); }
    @Override public String getParameter(String param) { return matcher != null ? matcher.group(param) : null; }
}