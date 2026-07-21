package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GameplayMenuCommand implements CLICommand {
    ADVANCE_TIME("advance time -t (?<count>\\d+) ticks"),
    COLLECT_SUN("collect sun -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)"),
    SHOW_SUN_AMOUNT("show sun amount"),
    CHEAT_ADD_SUNS("cheat add -n (?<count>\\d+) suns"),
    PLANT("plant plant -t (?<type>\\S+) -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)"),
    BREAK_VASE("break vase -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)"),
    CHEAT_REMOVE_COOLDOWN("cheat remove-cooldown"),
    PLUCK("pluck plant -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)"),
    FEED("feed plant -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)"),
    CHEAT_ADD_PLANT_FOOD("cheat add-plant-food"),
    SHOW_MAP("show map"),
    SHOW_PLANTS_STATUS("show plants status"),
    SHOW_TILE_STATUS("show tile status -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)"),
    RELEASE_NUKE("release the nuke"),
    ZOMBIES_INFO("zombies info"),
    START_ZOMBIE_WAVES("start zombie waves"),
    CHEAT_SPAWN_ZOMBIE("cheat spawn-zombie -t (?<zombieType>\\S+) -l (?<x>\\d+),\\s*(?<y>\\d+)"),
    PLACE_ZOMBIE("place zombie -t (?<type>\\S+) -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\)"),
    SWAP_PLANT("swap plant -l \\((?<x>\\d+),\\s*(?<y>\\d+)\\) -d (?<dir>up|down|left|right)"),
    UPGRADE_PLANT("upgrade plant -t (?<type>.+)"),
    SHOW_BEGHOULED_STATUS("show beghouled status");

    private final Pattern pattern;
    private Matcher matcher;

    GameplayMenuCommand(String regex) { this.pattern = Pattern.compile(regex); }

    @Override public Pattern getPattern() { return pattern; }
    @Override public Matcher getMatcher() { return matcher; }
    @Override public boolean matches(String query) { matcher = pattern.matcher(query); return matcher.matches(); }
    @Override public String getParameter(String param) { return matcher != null ? matcher.group(param) : null; }
}