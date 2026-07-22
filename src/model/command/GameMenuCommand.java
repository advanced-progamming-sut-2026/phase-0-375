package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GameMenuCommand implements CLICommand {
    ENTER_CHAPTER("menu enter chapter -c (?<chaptername>\\S+)"),
    ENTER_MINIGAME("menu enter minigame -t (?<type>\\S+) -s (?<stage>\\d+)"),
    GREENHOUSE("menu greenhouse"),
    TRAVEL_LOG("menu travel-log"),
    LEADERBOARD("menu leaderboard(?: -s (?<sort>\\S+))?(?: -o (?<order>\\S+))?"),
    COIN_WALLET("menu coin-wallet"),
    GEM_WALLET("menu gem-wallet"),
    CHEAT_ADD("menu cheat add (?<n>\\d+) (?<type>coin|diamond)");

    private final Pattern pattern;
    private Matcher matcher;

    GameMenuCommand(String regex) { this.pattern = Pattern.compile(regex); }

    @Override public Pattern getPattern() { return pattern; }
    @Override public Matcher getMatcher() { return matcher; }
    @Override public boolean matches(String query) { matcher = pattern.matcher(query); return matcher.matches(); }
    @Override public String getParameter(String param) { return matcher != null ? matcher.group(param) : null; }
}
