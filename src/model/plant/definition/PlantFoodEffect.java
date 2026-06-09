package model.plant.definition;

public class PlantFoodEffect {
    private String description;    // human-readable effect description; null if N/A
    private boolean isInstant;     // true = resolves immediately (burst sun, explosion)
    private float duration;        // seconds the effect lasts; 0 if instant

    public PlantFoodEffect(String description, boolean isInstant, float duration) {
        this.description = description;
        this.isInstant = isInstant;
        this.duration = duration;
    }

    // --- Getters ---

    public String getDescription() { return description; }

    public boolean isInstant() { return isInstant; }

    public float getDuration() { return duration; }

    // --- Setters ---

    public void setDescription(String description) { this.description = description; }

    public void setInstant(boolean instant) { isInstant = instant; }

    public void setDuration(float duration) { this.duration = duration; }
}
