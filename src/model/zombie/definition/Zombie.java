package model.zombie.definition;

import model.enums.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A concrete, data-driven definition of a zombie type.
 */
public class Zombie {
    private String name;
    private int baseHP;
    private float speed;
    private float eatDPS;
    private ZombieSize size;
    private Chapter chapter;        // which chapter this zombie belongs to; null = all chapters
    private int wavePointCost;      // spawning cost for model.game.wave balancing
    private int weight;             // spawn probability weight

    // --- Structural composition (what the zombie carries) ---
    private List<ArmorType> armorTypes;             // armor pieces; empty = no armor
    private PushableItemType pushableItemType;      // null = none
    private EquippedItemType equippedItemType;      // null = none
    private ImpType impType;                        // null = does not throw/spawn an imp

    // --- Damage modifiers ---
    private float fireDamageMultiplier = 1.0f; // Multiplier applied to incoming FIRE-elemental damage

    // --- Behavior identifiers ---
    private List<ZombieBehaviorType> behaviors;

    public Zombie(String name, int baseHP, float speed, float eatDPS,
                  ZombieSize size, Chapter chapter, int wavePointCost,
                  int weight, List<ArmorType> armorTypes,
                  PushableItemType pushableItemType, EquippedItemType equippedItemType,
                  ImpType impType, List<ZombieBehaviorType> behaviors) {
        this(name, baseHP, speed, eatDPS, size, chapter, wavePointCost, weight,
                armorTypes, pushableItemType, equippedItemType, impType,
                behaviors, 1.0f);
    }

    public Zombie(String name, int baseHP, float speed, float eatDPS,
                  ZombieSize size, Chapter chapter, int wavePointCost,
                  int weight, List<ArmorType> armorTypes,
                  PushableItemType pushableItemType, EquippedItemType equippedItemType,
                  ImpType impType, List<ZombieBehaviorType> behaviors,
                  float fireDamageMultiplier) {
        this.name = name;
        this.baseHP = baseHP;
        this.speed = speed;
        this.eatDPS = eatDPS;
        this.size = size;
        this.chapter = chapter;
        this.wavePointCost = wavePointCost;
        this.weight = weight;
        this.armorTypes = armorTypes != null ? armorTypes : new ArrayList<>();
        this.pushableItemType = pushableItemType;
        this.equippedItemType = equippedItemType;
        this.impType = impType;
        this.behaviors = behaviors != null ? behaviors : new ArrayList<>();
        this.fireDamageMultiplier = fireDamageMultiplier;
    }

    /**
     * @return true if this definition specify any armor
     */
    public boolean hasArmor() { return armorTypes != null && !armorTypes.isEmpty(); }

    /**
     * @return true if this definition push an item
     */
    public boolean isPusher() { return pushableItemType != null; }

    /**
     * @return true if this definition carry an equipped item
     */
    public boolean isEquipped() { return equippedItemType != null; }

    /**
     * @return true if this definition throw an Imp
     */
    public boolean throwsImp() {
        return behaviors.contains(ZombieBehaviorType.THROW_IMP);
    }

    /** Returns an unmodifiable list of this zombie's behaviors. */
    public List<ZombieBehaviorType> getBehaviors() {
        return Collections.unmodifiableList(behaviors);
    }

    /** Adds a behavior to this zombie's behavior list. */
    public void addBehavior(ZombieBehaviorType behavior) {
        behaviors.add(behavior);
    }

    /**
     * Removes the first behavior of the given type from this zombie.
     *
     * @return true if a behavior was removed
     */
    public boolean removeBehavior(ZombieBehaviorType behavior) {
        return behaviors.remove(behavior);
    }

    // --- Getters ---

    public String getName() {
        return name;
    }

    public int getBaseHP() {
        return baseHP;
    }

    public float getSpeed() {
        return speed;
    }

    public float getEatDPS() {
        return eatDPS;
    }

    public ZombieSize getSize() {
        return size;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public int getWavePointCost() {
        return wavePointCost;
    }

    public int getWeight() {
        return weight;
    }

    public List<ArmorType> getArmorTypes() {
        return armorTypes;
    }

    public PushableItemType getPushableItemType() {
        return pushableItemType;
    }

    public EquippedItemType getEquippedItemType() {
        return equippedItemType;
    }

    public ImpType getImpType() {
        return impType;
    }

    public float getFireDamageMultiplier() {
        return fireDamageMultiplier;
    }

    public boolean isImmuneToFire() {
        return fireDamageMultiplier <= 0f;
    }

    // --- Setters ---

    public void setName(String name) {
        this.name = name;
    }

    public void setBaseHP(int baseHP) {
        this.baseHP = baseHP;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setEatDPS(float eatDPS) {
        this.eatDPS = eatDPS;
    }

    public void setSize(ZombieSize size) {
        this.size = size;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public void setWavePointCost(int wavePointCost) {
        this.wavePointCost = wavePointCost;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void setArmorTypes(List<ArmorType> armorTypes) {
        this.armorTypes = armorTypes;
    }

    public void setPushableItemType(PushableItemType pushableItemType) {
        this.pushableItemType = pushableItemType;
    }

    public void setEquippedItemType(EquippedItemType equippedItemType) {
        this.equippedItemType = equippedItemType;
    }

    public void setImpType(ImpType impType) {
        this.impType = impType;
    }

    public void setFireDamageMultiplier(float fireDamageMultiplier) {
        this.fireDamageMultiplier = fireDamageMultiplier;
    }
}