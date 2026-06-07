package model.plant.instance;

import model.Placeable;
import model.enums.PlantState;
import model.plant.definition.Plant;

public class PlantInstance implements Placeable {
    private Plant plant;
    private PlantState state;
    private int currentDamage;

    public PlantInstance(Plant plant, PlantState state) {
        this.plant = plant;
        this.state = state;
        this.currentDamage = plant.getDamage();
    }

    public Plant getPlant() {
        return plant;
    }

    public PlantState getState() {
        return state;
    }

    public int getCurrentDamage() {
        return currentDamage;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public void setState(PlantState state) {
        this.state = state;
    }

    public void setCurrentDamage(int currentDamage) {
        this.currentDamage = currentDamage;
    }

    public void onPlantFood() {}
}
