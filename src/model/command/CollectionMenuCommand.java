package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum CollectionMenuCommand implements CLICommand {
    SHOW_PLANTS("menu collection show-plants"),
    SHOW_ALL_PLANTS("menu collection show-all-plants"),
    SHOW_ZOMBIES("menu collection show-zombies"),
    SHOW_ALL_ZOMBIES("menu collection show-all-zombies"),
    SHOW_PLANT("menu collection show-plant -p (?<plant_name>\\S+)"),
    SHOW_ZOMBIE("menu collection show-zombie -z (?<zombie_name>\\S+)"),
    UPGRADE_PLANT("menu collection upgrade-plant -p (?<plant_name>\\S+)"),
    PURCHASE_PLANT("menu collection purchase-plant -p (?<plant_name>\\S+)");

    private final Pattern pattern;
    private Matcher matcher;

    CollectionMenuCommand(String regex) { this.pattern = Pattern.compile(regex); }

    @Override public Pattern getPattern() { return pattern; }
    @Override public Matcher getMatcher() { return matcher; }
    @Override public boolean matches(String query) { matcher = pattern.matcher(query); return matcher.matches(); }
    @Override public String getParameter(String param) { return matcher != null ? matcher.group(param) : null; }
}