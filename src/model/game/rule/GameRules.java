package model.game.rule;

import model.enums.PlantCategory;

import java.util.Set;

/* Game rules for each game mode */
public class GameRules {
    private boolean skyDropEnabled;
    private boolean zombiesFreezable;
    private int initialSun;
    private double sunDropRateModifier;
    private Set<PlantCategory> allowedPlantCategories;
    private Set<Integer> forbiddenColumns;
    private Set<Integer> forbiddenRows;

    public GameRules(boolean skyDropEnabled, boolean zombiesFreezable,
                     int initialSun, double sunDropRateModifier,
                     Set<PlantCategory> allowedPlantCategories,
                     Set<Integer> forbiddenColumns, Set<Integer> forbiddenRows) {
        this.skyDropEnabled = skyDropEnabled;
        this.zombiesFreezable = zombiesFreezable;
        this.initialSun = initialSun;
        this.sunDropRateModifier = sunDropRateModifier;
        this.allowedPlantCategories = allowedPlantCategories;
        this.forbiddenColumns = forbiddenColumns;
        this.forbiddenRows = forbiddenRows;
    }


    public boolean isSkyDropEnabled() {
        return skyDropEnabled;
    }

    public boolean areZombiesFreezable() {
        return zombiesFreezable;
    }

    public int getInitialSun() {
        return initialSun;
    }

    public double getSunDropRateModifier() {
        return sunDropRateModifier;
    }

    public Set<PlantCategory> getAllowedPlantCategories() {
        return allowedPlantCategories;
    }

    public Set<Integer> getForbiddenColumns() {
        return forbiddenColumns;
    }

    public Set<Integer> getForbiddenRows() {
        return forbiddenRows;
    }
}
