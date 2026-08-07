package model.plant.definition;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.enums.PlantTags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Concrete, data-driven definition of a plant type.
 */
public class Plant {
    private final int id;
    private String name;
    private PlantCategory category;
    private List<PlantTags> tags;
    private int cost;
    private int baseHP;
    private int damage;
    private float rechargeTime;         // seconds before the seed is available again
    private float actionInterval;       // seconds between ability actions; 0 = no recurring action

    /** What the plant does on each {@link #actionInterval} tick. */
    private PlantAbilityType abilityType;
    /** Magnitude parameter for {@link #abilityType}. */
    private float abilityValue;

    /** What the plant does when fed plant food. */
    private PlantFoodEffect plantFoodEffect;

    private PlantLevels levels;

    // --- Constructors ---

    public Plant(int id, String name, PlantCategory category, List<PlantTags> tags,
                 int cost, int baseHP, int damage,
                 float rechargeTime, float actionInterval,
                 PlantAbilityType abilityType, float abilityValue,
                 PlantFoodType plantFoodType, float plantFoodValue,
                 PlantLevels levels) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.cost = cost;
        this.baseHP = baseHP;
        this.damage = damage;
        this.rechargeTime = rechargeTime;
        this.actionInterval = actionInterval;
        this.abilityType = abilityType;
        this.abilityValue = abilityValue;
        this.plantFoodEffect = new PlantFoodEffect(plantFoodType, plantFoodValue);
        this.levels = levels;
    }

    /** Copy constructor: per-instance copy so level upgrades never touch the shared definition. */
    public Plant(Plant other) {
        this.id = other.id;
        this.name = other.name;
        this.category = other.category;
        this.tags = new ArrayList<>(other.tags);
        this.cost = other.cost;
        this.baseHP = other.baseHP;
        this.damage = other.damage;
        this.rechargeTime = other.rechargeTime;
        this.actionInterval = other.actionInterval;
        this.abilityType = other.abilityType;
        this.abilityValue = other.abilityValue;
        this.plantFoodEffect = new PlantFoodEffect(other.getPlantFoodType(), other.getPlantFoodValue());
        this.levels = other.levels;
    }

    // --- Tag helpers ---

    public boolean hasTag(PlantTags tag) {
        return tag != null && tags.contains(tag);
    }

    public boolean isShroom() { return hasTag(PlantTags.SHROOM); }
    public boolean isDayPlant() { return hasTag(PlantTags.DAY); }
    public boolean isNightPlant() { return hasTag(PlantTags.NIGHT); }
    public boolean isInstant() { return baseHP <= 0; }
    public boolean isFreePlant() { return cost == 0; }
    public boolean hasPlantFood() { return !plantFoodEffect.isNone(); }

    // --- Getters ---

    public int getId() { return id; }

    public String getName() { return name; }

    public PlantCategory getCategory() { return category; }

    public List<PlantTags> getTags() { return Collections.unmodifiableList(tags); }

    public int getCost() { return cost; }

    public int getBaseHP() { return baseHP; }

    public int getDamage() { return damage; }

    public float getRechargeTime() { return rechargeTime; }

    public float getActionInterval() { return actionInterval; }

    public PlantAbilityType getAbilityType() { return abilityType; }

    public float getAbilityValue() { return abilityValue; }

    public PlantFoodType getPlantFoodType() { return plantFoodEffect.getType(); }

    public float getPlantFoodValue() { return plantFoodEffect.getValue(); }

    public PlantLevels getLevels() { return levels; }

    // --- Setters ---

    public void setName(String name) { this.name = name; }

    public void setCategory(PlantCategory category) { this.category = category; }

    public void setTags(List<PlantTags> tags) {
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public void setCost(int cost) { this.cost = cost; }

    public void setBaseHP(int baseHP) { this.baseHP = baseHP; }

    public void setDamage(int damage) { this.damage = damage; }

    public void setRechargeTime(float rechargeTime) { this.rechargeTime = rechargeTime; }

    public void setActionInterval(float actionInterval) { this.actionInterval = actionInterval; }

    public void setAbilityType(PlantAbilityType abilityType) { this.abilityType = abilityType; }

    public void setAbilityValue(float abilityValue) { this.abilityValue = abilityValue; }

    public void setPlantFoodType(PlantFoodType plantFoodType) { this.plantFoodEffect.setType(plantFoodType); }

    public void setPlantFoodValue(float plantFoodValue) { this.plantFoodEffect.setValue(plantFoodValue); }

    public void setLevels(PlantLevels levels) { this.levels = levels; }

    @Override
    public String toString() {
        return "Plant{" + id + ":" + name + " (" + category + ")}";
    }
}
