package model.zombie.armor;

import model.enums.ArmorType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a piece of armor worn by a zombie.
 */
public class Armor {
    private ArmorType type;
    private int baseHealth;
    private int currentHealth;
    private boolean isMetallic;         // true = can be attracted by Magnet-shroom
    private boolean isDroppable;        // true = can be knocked off by specific attacks
    private boolean isHelm;             // true = occupies the head slot
    private boolean passesDamageThrough;// true = damage also hits zombie HP (shoulder pads)
    private boolean chillsAttacker;     // true = chills plants that attack this armor (ice block)
    private List<String> damageLayers;  // visual layer names for damage states
    private List<Float> layerThresholds;// health % thresholds for each damage layer transition

    public Armor() {
        this.damageLayers = new ArrayList<>();
        this.layerThresholds = new ArrayList<>();
    }

    public Armor(ArmorType type, int baseHealth, boolean isMetallic, boolean isDroppable,
                 boolean isHelm, boolean passesDamageThrough, boolean chillsAttacker) {
        this.type = type;
        this.baseHealth = baseHealth;
        this.currentHealth = baseHealth;
        this.isMetallic = isMetallic;
        this.isDroppable = isDroppable;
        this.isHelm = isHelm;
        this.passesDamageThrough = passesDamageThrough;
        this.chillsAttacker = chillsAttacker;
        this.damageLayers = new ArrayList<>();
        this.layerThresholds = new ArrayList<>();
    }

    /**
     * Applies damage to this armor piece. Returns overflow damage
     * that should be applied to the zombie's HP.
     */
    public int takeDamage(int damage) { return 0; }

    /**
     * @return true if the armor is fully destroyed
     */
    public boolean isDestroyed() { return currentHealth <= 0; }

    /**
     * @return the current damage layer index
     */
    public int getCurrentDamageLayer() { return 0; }

    // --- Getters ---

    public ArmorType getType() {
        return type;
    }

    public int getBaseHealth() {
        return baseHealth;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public boolean isMetallic() {
        return isMetallic;
    }

    public boolean isDroppable() {
        return isDroppable;
    }

    public boolean isHelm() {
        return isHelm;
    }

    public boolean isPassesDamageThrough() {
        return passesDamageThrough;
    }

    public boolean isChillsAttacker() {
        return chillsAttacker;
    }

    public List<String> getDamageLayers() {
        return damageLayers;
    }

    public List<Float> getLayerThresholds() {
        return layerThresholds;
    }

    // --- Setters ---

    public void setType(ArmorType type) {
        this.type = type;
    }

    public void setBaseHealth(int baseHealth) {
        this.baseHealth = baseHealth;
    }

    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }

    public void setMetallic(boolean metallic) {
        isMetallic = metallic;
    }

    public void setDroppable(boolean droppable) {
        isDroppable = droppable;
    }

    public void setHelm(boolean helm) {
        isHelm = helm;
    }

    public void setPassesDamageThrough(boolean passesDamageThrough) {
        this.passesDamageThrough = passesDamageThrough;
    }

    public void setChillsAttacker(boolean chillsAttacker) {
        this.chillsAttacker = chillsAttacker;
    }

    public void setDamageLayers(List<String> damageLayers) {
        this.damageLayers = damageLayers;
    }

    public void setLayerThresholds(List<Float> layerThresholds) {
        this.layerThresholds = layerThresholds;
    }
}
