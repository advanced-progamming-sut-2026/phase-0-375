package model.plant.definition;

public class PlantLevels {
    private LevelUpgrade level2;
    private LevelUpgrade level3;
    private LevelUpgrade level4;

    public PlantLevels(LevelUpgrade level2, LevelUpgrade level3, LevelUpgrade level4) {
        this.level2 = level2;
        this.level3 = level3;
        this.level4 = level4;
    }

    /**
     * Returns the upgrade for the given level
     */
    public LevelUpgrade getUpgrade(int level) {
        return null;
    }

    // --- Getters ---

    public LevelUpgrade getLevel2() { return level2; }

    public LevelUpgrade getLevel3() { return level3; }

    public LevelUpgrade getLevel4() { return level4; }

    // --- Setters ---

    public void setLevel2(LevelUpgrade level2) { this.level2 = level2; }

    public void setLevel3(LevelUpgrade level3) { this.level3 = level3; }

    public void setLevel4(LevelUpgrade level4) { this.level4 = level4; }
}
