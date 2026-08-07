package model.game.rule;

import model.enums.PlantCategory;

import java.util.Set;

/* Game rules for each game mode */
public class GameRules {
    private boolean skyDropEnabled;
    private boolean zombiesFreezable;
    private int initialSun;
    private double sunDropRateModifier;
    private int minPlantsRequired;
    private int maxPlantsAllowed;
    private Set<PlantCategory> allowedPlantCategories;
    private Set<Integer> forbiddenColumns;
    private Set<Integer> forbiddenRows;

    // Rules relevant to special levels
    private boolean sunFallsFromSky;             // false for Night Ops and Plant What You Get
    private boolean sunProducingPlantsAllowed;   // false for Plant What You Get
    private boolean plantFoodDrops;              // whether plant food drops during the level
    private boolean lawnMowersEnabled;           // whether lawn mowers are active
    private boolean shovelEnabled;               // whether the shovel tool is available
    private boolean allowsChoosingPlants;        // false for Conveyor Belt / Locked Plants
    private boolean plantRechargeInSetup;        // false for Plant What You Get setup phase
    private int deadLineColumn;                  // for Dead Line levels: column of the red line (-1 = none)
    private int maxPlantDeaths;                  // for Love Your Plants: max plant deaths allowed (-1 = unlimited)
    private float timedWarLimit;                 // for Timed War: time limit in seconds (-1 = no limit)
    private int timedWarTargetKills;             // for Timed War: target zombie kills (-1 = not timed war)

    public GameRules(boolean skyDropEnabled, boolean zombiesFreezable, int initialSun,
                     double sunDropRateModifier, int minPlantsRequired, int maxPlantsAllowed,
                     Set<PlantCategory> allowedPlantCategories, Set<Integer> forbiddenColumns,
                     Set<Integer> forbiddenRows) {
        this.skyDropEnabled = skyDropEnabled;
        this.zombiesFreezable = zombiesFreezable;
        this.initialSun = initialSun;
        this.sunDropRateModifier = sunDropRateModifier;
        this.minPlantsRequired = minPlantsRequired;
        this.maxPlantsAllowed = maxPlantsAllowed;
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

    public int getMinPlantsRequired() {
        return minPlantsRequired;
    }

    public int getMaxPlantsAllowed() {
        return maxPlantsAllowed;
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

    public int getTimedWarTargetKills() {
        return timedWarTargetKills;
    }

    public float getTimedWarLimit() {
        return timedWarLimit;
    }

    public int getMaxPlantDeaths() {
        return maxPlantDeaths;
    }

    public int getDeadLineColumn() {
        return deadLineColumn;
    }

    public boolean isPlantRechargeInSetup() {
        return plantRechargeInSetup;
    }

    public boolean isAllowsChoosingPlants() {
        return allowsChoosingPlants;
    }

    public boolean isShovelEnabled() {
        return shovelEnabled;
    }

    public boolean isLawnMowersEnabled() {
        return lawnMowersEnabled;
    }

    public boolean isPlantFoodDrops() {
        return plantFoodDrops;
    }

    public boolean isSunProducingPlantsAllowed() {
        return sunProducingPlantsAllowed;
    }

    public boolean isSunFallsFromSky() {
        return sunFallsFromSky;
    }

    // --- Special Level fields Setters ---

    public void setSunFallsFromSky(boolean sunFallsFromSky) {
        this.sunFallsFromSky = sunFallsFromSky;
    }

    public void setSunProducingPlantsAllowed(boolean sunProducingPlantsAllowed) {
        this.sunProducingPlantsAllowed = sunProducingPlantsAllowed;
    }

    public void setPlantFoodDrops(boolean plantFoodDrops) {
        this.plantFoodDrops = plantFoodDrops;
    }

    public void setLawnMowersEnabled(boolean lawnMowersEnabled) {
        this.lawnMowersEnabled = lawnMowersEnabled;
    }

    public void setShovelEnabled(boolean shovelEnabled) {
        this.shovelEnabled = shovelEnabled;
    }

    public void setAllowsChoosingPlants(boolean allowsChoosingPlants) {
        this.allowsChoosingPlants = allowsChoosingPlants;
    }

    public void setPlantRechargeInSetup(boolean plantRechargeInSetup) {
        this.plantRechargeInSetup = plantRechargeInSetup;
    }

    public void setDeadLineColumn(int deadLineColumn) {
        this.deadLineColumn = deadLineColumn;
    }

    public void setMaxPlantDeaths(int maxPlantDeaths) {
        this.maxPlantDeaths = maxPlantDeaths;
    }

    public void setTimedWarLimit(float timedWarLimit) {
        this.timedWarLimit = timedWarLimit;
    }

    public void setTimedWarTargetKills(int timedWarTargetKills) {
        this.timedWarTargetKills = timedWarTargetKills;
    }
}
