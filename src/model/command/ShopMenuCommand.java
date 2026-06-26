package model.command;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ShopMenuCommand implements CLICommand {
    SHOP_LIST("shop list"),
    SHOP_DAILY("shop daily"),
    SHOP_BUY("shop buy -i (?<item_id>\\d+) -n (?<count>\\d+)( -t (?<plant_type>\\S+))?");

    private final Pattern pattern;
    private Matcher matcher;

    ShopMenuCommand(String regex) { this.pattern = Pattern.compile(regex); }

    @Override public Pattern getPattern() { return pattern; }
    @Override public Matcher getMatcher() { return matcher; }
    @Override public boolean matches(String query) { matcher = pattern.matcher(query); return matcher.matches(); }
    @Override public String getParameter(String param) { return matcher != null ? matcher.group(param) : null; }
}
