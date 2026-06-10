package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum RegisterMenuCommand implements CLICommand {
    REGISTER("register\\s+" +
            "-u\\s+(?<username>\\S+)\\s+" +
            "-p\\s+(?<password>\\S+)\\s+(?<password_confirm>\\S+)\\s+" +
            "-n\\s+(?<nickname>\\S+)\\s+" +
            "-e\\s+(?<email>\\S+)\\s+" +
            "-g\\s+(?<gender>\\S+)"),
    PICK_QUESTION("pick question -q (?<question_number>\\d+) -a (?<answer>\\S+) -c (?<answer_confirm>\\S+)");

    private final Pattern pattern;
    private Matcher matcher;

    RegisterMenuCommand(String regex) { this.pattern = Pattern.compile(regex); }

    @Override public Pattern getPattern() { return pattern; }
    @Override public Matcher getMatcher() { return matcher; }
    @Override public boolean matches(String query) { matcher = pattern.matcher(query); return matcher.matches(); }
    @Override public String getParameter(String param) { return matcher != null ? matcher.group(param) : null; }
}