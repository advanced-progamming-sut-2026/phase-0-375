package model.plant.definition;

import model.enums.PlantAbilityType;
import model.enums.PlantTags;
import model.item.placeable.Placeable;
import model.plant.ability.PlantAbility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A concrete, data-driven definition of a plant type.
 */
public abstract class Plant {
    private String name;
    private PlantAbilityType category;
    private List<PlantTags> tags;
    private int cost;
    private int baseHP;
    private float rechargeTime;         // seconds before the seed is available again
    private float actionInterval;       // seconds between ability actions
    private List<PlantAbility> abilities;
    private PlantFoodEffect plantFoodEffect;
    private PlantLevels levels;

    public Plant(String name, PlantAbilityType category, List<PlantTags> tags,
                 int cost, int baseHP, float rechargeTime, float actionInterval,
                 List<PlantAbility> abilities, PlantFoodEffect plantFoodEffect, PlantLevels levels) {
        this.name = name;
        this.category = category;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.cost = cost;
        this.baseHP = baseHP;
        this.rechargeTime = rechargeTime;
        this.actionInterval = actionInterval;
        this.abilities = abilities != null ? abilities : new ArrayList<>();
        this.plantFoodEffect = plantFoodEffect;
        this.levels = levels;
    }

    // --- Ability lookup helpers ---

    /** Finds the first ability that matches the input in this plant's ability list */
    public PlantAbility getAbility(PlantAbilityType type) {
        return null;
    }

    /** Checks whether this plant has at least one ability of the given type. */
    public boolean hasAbility(PlantAbilityType type) {
        return false;
    }

    /** Returns an unmodifiable list of this plant's abilities. */
    public List<PlantAbility> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    /** Adds an ability to this plant's ability list. */
    public void addAbility(PlantAbility ability) {

    }

    /**
     * Removes the first ability of the given type from this plant.
     *
     * @return true if an ability was removed
     */
    public boolean removeAbility(PlantAbilityType type) {
        return false;
    }

    // --- Getters ---

    public String getName() {
        return name;
    }

    public PlantAbilityType getCategory() {
        return category;
    }

    public List<PlantTags> getTags() {
        return tags;
    }

    public int getCost() {
        return cost;
    }

    public int getBaseHP() {
        return baseHP;
    }

    public float getRechargeTime() {
        return rechargeTime;
    }

    public float getActionInterval() {
        return actionInterval;
    }

    public PlantFoodEffect getPlantFoodEffect() {
        return plantFoodEffect;
    }

    public PlantLevels getLevels() {
        return levels;
    }

    // --- Setters ---


    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(PlantAbilityType category) {
        this.category = category;
    }

    public void setTags(List<PlantTags> tags) {
        this.tags = tags;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void setBaseHP(int baseHP) {
        this.baseHP = baseHP;
    }

    public void setRechargeTime(float rechargeTime) {
        this.rechargeTime = rechargeTime;
    }

    public void setActionInterval(float actionInterval) {
        this.actionInterval = actionInterval;
    }

    public void setPlantFoodEffect(PlantFoodEffect plantFoodEffect) {
        this.plantFoodEffect = plantFoodEffect;
    }

    public void setLevels(PlantLevels levels) {
        this.levels = levels;
    }
}
