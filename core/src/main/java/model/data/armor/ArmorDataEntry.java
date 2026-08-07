package model.data.armor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw DTO that mirrors one entry in {@code ArmorTypeData.json}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArmorDataEntry {

    @JsonProperty("aliases")
    private List<String> aliases;

    @JsonProperty("objdata")
    private ArmorObjData objdata;

    public String getPrimaryAlias() {
        return (aliases != null && !aliases.isEmpty()) ? aliases.get(0) : null;
    }

    public List<String> getAliases() { return aliases; }
    public ArmorObjData getObjdata()  { return objdata; }

    // ---------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArmorObjData {

        @JsonProperty("ArmorType")
        private String armorType;

        @JsonProperty("BaseHealth")
        private int baseHealth;

        /** e.g. ["metallic", "damageable", "droppable", "helm", "passdamage"] */
        @JsonProperty("ArmorFlags")
        private List<String> armorFlags;

        @JsonProperty("ArmorLayers")
        private List<String> armorLayers;

        /** Health-percentage thresholds (0..1) for visual damage transitions */
        @JsonProperty("ArmorLayerHealth")
        private List<Float> armorLayerHealth;

        // --- convenience flag helpers ---

        public boolean isMetallic()          { return has("metallic"); }
        public boolean isDroppable()         { return has("droppable"); }
        public boolean isHelm()              { return has("helm"); }
        public boolean passesDamageThrough(){ return has("passdamage"); }

        private boolean has(String flag) {
            return armorFlags != null && armorFlags.contains(flag);
        }

        // --- getters ---

        public String getArmorType() {
            return armorType;
        }

        public int getBaseHealth() {
            return baseHealth;
        }

        public List<String> getArmorFlags() {
            return armorFlags;
        }

        public List<String> getArmorLayers() {
            return armorLayers;
        }

        public List<Float> getArmorLayerHealth() {
            return armorLayerHealth;
        }
    }
}