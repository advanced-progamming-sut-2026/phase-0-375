package model.plant.definition;

import model.enums.LevelUpgradeType;
import model.enums.PlantSpecialTag;

/**
 * One permanent stat buff applied when a plant levels up.
 */
public class LevelUpgrade {
    private final int level;            // 2, 3 or 4
    private LevelUpgradeType type;
    private float value;
    private PlantSpecialTag specialTag; // non-null only when type == SPECIAL_MECHANIC

    public LevelUpgrade(int level, LevelUpgradeType type, float value, PlantSpecialTag specialTag) {
        this.level = level;
        this.type = type;
        this.value = value;
        this.specialTag = specialTag != null ? specialTag : PlantSpecialTag.NONE;
    }

    public int getLevel() { return level; }

    public LevelUpgradeType getType() { return type; }

    public float getValue() { return value; }

    public PlantSpecialTag getSpecialTag() { return specialTag; }

    public boolean isSpecialMechanic() {
        return type == LevelUpgradeType.SPECIAL_MECHANIC;
    }

    public void setType(LevelUpgradeType type) { this.type = type; }

    public void setValue(float value) { this.value = value; }

    public void setSpecialTag(PlantSpecialTag specialTag) {
        this.specialTag = specialTag != null ? specialTag : PlantSpecialTag.NONE;
    }
}