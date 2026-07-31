package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum TravelLogMenuCommand implements CLICommand {
    CHANGE_PAGE("travel log page (?<pageName>\\S+)"),
    SHOW_CURRENT_PAGE("show current page"),
    SHOW_DAILY_QUESTS("show daily quests"),
    SHOW_MAIN_QUESTS("show main quests"),
    SHOW_EPIC_QUESTS("show epic quests"),
    SHOW_ALL_QUESTS("show all quests"),
    SHOW_COMPLETED_QUESTS("show completed quests"),
    COMPLETE_QUEST("complete quest -n (?<questName>.+)"),
    SHOW_QUEST_PROGRESS("show quest progress -n (?<questName>.+)"),
    SHOW_MINIGAMES("show minigames"),
    ENTER_MINIGAME("enter minigame -t (?<type>\\S+) -s (?<stage>\\d+)");

    private final Pattern pattern;
    private Matcher matcher;

    TravelLogMenuCommand(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }

    @Override
    public Matcher getMatcher() {
        return matcher;
    }

    @Override
    public boolean matches(String query) {
        matcher = pattern.matcher(query);
        return matcher.matches();
    }

    @Override
    public String getParameter(String param) {
        return matcher != null ? matcher.group(param) : null;
    }
}