package model.plant.definition;

import model.enums.PlantFoodType;

/**
 * Data-driven description of what happens when a plant is fed plant food.
 */
public class PlantFoodEffect {
    private PlantFoodType type;
    private float value;

    public PlantFoodEffect(PlantFoodType type, float value) {
        this.type = type != null ? type : PlantFoodType.NONE;
        this.value = value;
    }

    public PlantFoodType getType() { return type; }

    public float getValue() { return value; }

    public void setType(PlantFoodType type) {
        this.type = type != null ? type : PlantFoodType.NONE;
    }

    public void setValue(float value) { this.value = value; }

    public boolean isNone() { return type == null || type == PlantFoodType.NONE; }
}
