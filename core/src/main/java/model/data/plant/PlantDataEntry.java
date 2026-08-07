package model.data.plant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Raw DTO mirroring one entry of {@code plants.json}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlantDataEntry {

    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("category")
    private String category;

    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    @JsonProperty("cost")
    private int cost;

    @JsonProperty("baseHp")
    private int baseHp;

    @JsonProperty("damage")
    private int damage;

    @JsonProperty("actionInterval")
    private float actionInterval;

    @JsonProperty("recharge")
    private float recharge;

    @JsonProperty("abilityType")
    private String abilityType;

    @JsonProperty("abilityValue")
    private float abilityValue;

    @JsonProperty("plantFoodType")
    private String plantFoodType;

    @JsonProperty("plantFoodValue")
    private float plantFoodValue;

    @JsonProperty("upgrades")
    private List<UpgradeEntry> upgrades = new ArrayList<>();

    // --- Getters ---

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public List<String> getTags() { return tags; }
    public int getCost() { return cost; }
    public int getBaseHp() { return baseHp; }
    public int getDamage() { return damage; }
    public float getActionInterval() { return actionInterval; }
    public float getRecharge() { return recharge; }
    public String getAbilityType() { return abilityType; }
    public float getAbilityValue() { return abilityValue; }
    public String getPlantFoodType() { return plantFoodType; }
    public float getPlantFoodValue() { return plantFoodValue; }
    public List<UpgradeEntry> getUpgrades() { return upgrades; }

    // --- Setters ---

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setCost(int cost) { this.cost = cost; }
    public void setBaseHp(int baseHp) { this.baseHp = baseHp; }
    public void setDamage(int damage) { this.damage = damage; }
    public void setActionInterval(float actionInterval) { this.actionInterval = actionInterval; }
    public void setRecharge(float recharge) { this.recharge = recharge; }
    public void setAbilityType(String abilityType) { this.abilityType = abilityType; }
    public void setAbilityValue(float abilityValue) { this.abilityValue = abilityValue; }
    public void setPlantFoodType(String plantFoodType) { this.plantFoodType = plantFoodType; }
    public void setPlantFoodValue(float plantFoodValue) { this.plantFoodValue = plantFoodValue; }
    public void setUpgrades(List<UpgradeEntry> upgrades) { this.upgrades = upgrades; }

    /** Mirrors one element of the {@code upgrades} array. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpgradeEntry {
        @JsonProperty("level")
        private int level;

        @JsonProperty("type")
        private String type;

        @JsonProperty("value")
        private float value;

        @JsonProperty("specialTag")
        private String specialTag = "";

        public int getLevel() { return level; }
        public String getType() { return type; }
        public float getValue() { return value; }
        public String getSpecialTag() { return specialTag; }

        public void setLevel(int level) { this.level = level; }
        public void setType(String type) { this.type = type; }
        public void setValue(float value) { this.value = value; }
        public void setSpecialTag(String specialTag) { this.specialTag = specialTag; }
    }
}
