package model.plant.instance;

/**
 * Represents a pre-placed plant that must be protected in Save Our Seeds levels.
 */
public class ProtectedPlant {
    private PlantInstance plantInstance;

    public ProtectedPlant(PlantInstance plantInstance) {
        this.plantInstance = plantInstance;
    }

    public PlantInstance getPlantInstance() {
        return plantInstance;
    }

    public void setPlantInstance(PlantInstance plantInstance) {
        this.plantInstance = plantInstance;
    }
}
