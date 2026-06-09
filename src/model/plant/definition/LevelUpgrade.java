package model.plant.definition;

import model.enums.LevelUpgradeType;

public class LevelUpgrade {
    private LevelUpgradeType type;
    private float value;        // the numeric delta applied to the stat
    private String description; // human-readable description (e.g. "Dmg +10")

    public LevelUpgrade(LevelUpgradeType type, float value, String description) {
        this.type = type;
        this.value = value;
        this.description = description;
    }

    // --- Getters ---

    public LevelUpgradeType getType() { return type; }

    public float getValue() { return value; }

    public String getDescription() { return description; }

    // --- Setters ---

    public void setType(LevelUpgradeType type) { this.type = type; }

    public void setValue(float value) { this.value = value; }

    public void setDescription(String description) { this.description = description; }

}
