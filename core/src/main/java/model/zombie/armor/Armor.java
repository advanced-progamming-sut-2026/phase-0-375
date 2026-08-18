package model.zombie.armor;

import model.enums.ArmorType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a piece of armor worn by a zombie.
 */
public class Armor {
    private ArmorType type;
    private final int baseHealth;
    private int currentHealth;
    private boolean isMetallic;         // true = can be attracted by Magnet-shroom
    private boolean isDroppable;        // true = can be knocked off by specific attacks
    private boolean isHelm;             // true = occupies the head slot
    private boolean passesDamageThrough;// true = damage also hits zombie HP (shoulder pads)
    private List<String> damageLayers;  // visual layer names for damage states
    private List<Float> layerThresholds;// health % thresholds for each damage layer transition

    public Armor(ArmorType type, int baseHealth, boolean isMetallic, boolean isDroppable,
                 boolean isHelm, boolean passesDamageThrough) {
        this.type = type;
        this.baseHealth = baseHealth;
        this.currentHealth = baseHealth;
        this.isMetallic = isMetallic;
        this.isDroppable = isDroppable;
        this.isHelm = isHelm;
        this.passesDamageThrough = passesDamageThrough;
        this.damageLayers = new ArrayList<>();
        this.layerThresholds = new ArrayList<>();
    }

    /**
     * Applies damage to this armor piece. Returns overflow damage
     * that should be applied to the zombie's HP.
     */
    public int takeDamage(int damage) {
        if (damage <= 0) return 0;
        currentHealth -= damage;
        if (passesDamageThrough) {
            if (currentHealth < 0) {
                currentHealth = 0;
            }
            return damage;
        }
        if (currentHealth <= 0) {
            int overflow = -currentHealth;
            currentHealth = 0;
            return overflow;
        }
        return 0;
    }

    /**
     * @return true if the armor is fully destroyed
     */
    public boolean isDestroyed() { return currentHealth <= 0; }

    /**
     * {@code ArmorLayerHealth} values are remaining-HP fractions, high to low
     * (e.g. 0.666, 0.333). Layer 0 above the first, last layer at or below the last.
     */
    public int getCurrentDamageLayer() {
        int n = damageLayers == null ? 0 : damageLayers.size();
        if (n <= 1 || layerThresholds == null || layerThresholds.isEmpty() || baseHealth <= 0) {
            return 0;
        }
        float frac = (float) currentHealth / (float) baseHealth;
        for (int i = 0; i < layerThresholds.size(); i++) {
            if (frac > layerThresholds.get(i)) {
                return i;
            }
        }
        return n - 1;
    }

    /** Last damage sprite — the piece that pops off when this armor breaks. */
    public String popLayer() {
        if (damageLayers == null || damageLayers.isEmpty()) {
            return null;
        }
        return damageLayers.get(damageLayers.size() - 1);
    }

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

    public void setDamageLayers(List<String> damageLayers) {
        this.damageLayers = damageLayers;
    }

    public void setLayerThresholds(List<Float> layerThresholds) {
        this.layerThresholds = layerThresholds;
    }

    @Override
    public String toString() {
        return type.getPrimaryAlias();
    }
}
