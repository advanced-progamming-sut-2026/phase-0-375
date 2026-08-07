package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public interface CLICommand {
    Pattern getPattern();
    Matcher getMatcher();
    boolean matches(String query);
    String getParameter(String param);
}
