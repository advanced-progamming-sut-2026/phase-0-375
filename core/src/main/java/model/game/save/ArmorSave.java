package model.game.save;

/** Snapshot of one zombie armor piece. */
public class ArmorSave {
    private String armorType;
    private int currentHealth;

    public String getArmorType() { return armorType; }
    public void setArmorType(String armorType) { this.armorType = armorType; }
    public int getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(int currentHealth) { this.currentHealth = currentHealth; }
}
