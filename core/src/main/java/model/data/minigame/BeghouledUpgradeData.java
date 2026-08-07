package model.data.minigame;

/**
 * JSON DTO for one Beghouled plant upgrade
 * (see the "upgrades" key in minigames.json).
 */
public class BeghouledUpgradeData {

    /** Plant definition name to upgrade from. */
    private String from;
    /** Plant definition name to upgrade to. */
    private String to;
    /** Sun cost for upgrading every plant of the type at once. */
    private int cost;

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }
}
