package model.plant.instance;

import model.enums.PlantAbilityType;
import model.plant.definition.LevelUpgrade;
import model.plant.definition.Plant;
import model.plant.definition.PlantLevels;

import java.util.Map;

/** Level-upgrade application extracted from {@link PlantInstance}. */
final class PlantUpgradeApplier {

    private PlantUpgradeApplier() {}

    static void apply(PlantInstance plant, int targetLevel) {
        Plant definition = plant.definition;
        if (targetLevel <= 1 || definition.getLevels() == null) {
            plant.level = Math.max(1, targetLevel);
            return;
        }
        PlantLevels levels = definition.getLevels();
        int newHP = definition.getBaseHP();
        int newDamage = definition.getDamage();
        int newCost = definition.getCost();
        float newRecharge = definition.getRechargeTime();
        float newActionInterval = definition.getActionInterval();
        for (Map.Entry<Integer, LevelUpgrade> entry : levels.cumulativeUpgrades(targetLevel).entrySet()) {
            LevelUpgrade upgrade = entry.getValue();
            if (upgrade == null) {
                continue;
            }
            switch (upgrade.getType()) {
                case BUFF_HP -> newHP += (int) upgrade.getValue();
                case BUFF_DAMAGE -> newDamage += (int) upgrade.getValue();
                case BUFF_COST -> newCost = Math.max(0, newCost + (int) upgrade.getValue());
                case BUFF_RECHARGE -> newRecharge = Math.max(0, newRecharge + upgrade.getValue());
                case BUFF_ACTION_INTERVAL ->
                        newActionInterval = Math.max(0, newActionInterval + upgrade.getValue());
                case SPECIAL_MECHANIC -> applySpecialMechanic(plant, upgrade);
                default -> { }
            }
        }
        int hpDelta = newHP - definition.getBaseHP();
        plant.currentHP = Math.min(newHP, plant.currentHP + Math.max(0, hpDelta));
        plant.definition = new Plant(definition);
        plant.definition.setBaseHP(newHP);
        plant.definition.setDamage(newDamage);
        plant.definition.setCost(newCost);
        plant.definition.setRechargeTime(newRecharge);
        plant.definition.setActionInterval(newActionInterval);
        plant.level = targetLevel;
    }

    private static void applySpecialMechanic(PlantInstance plant, LevelUpgrade upgrade) {
        switch (upgrade.getSpecialTag()) {
            case LIFESPAN_EXT -> applyLifespanExt(plant, upgrade);
            case GROWTH_STAGE_MAX_UP -> {
                AbilityState state = plant.abilityStates.get(plant.definition.getAbilityType());
                if (state != null) {
                    state.setGrowthStage(state.getGrowthStage() + (int) upgrade.getValue());
                }
            }
            case GROW_TIME_REDUCTION -> {
                AbilityState prodState = plant.abilityStates.get(PlantAbilityType.PRODUCE_SUN);
                if (prodState != null && prodState.getCooldownRemaining() > 0) {
                    prodState.setCooldownRemaining(
                            Math.max(0f, prodState.getCooldownRemaining() - upgrade.getValue()));
                }
            }
            case ARM_TIME_REDUCTION -> {
                AbilityState trapState = plant.abilityStates.get(PlantAbilityType.DELAYED_EXPLOSIVE);
                if (trapState != null && !trapState.isArmed() && trapState.getCooldownRemaining() > 0) {
                    trapState.setCooldownRemaining(
                            Math.max(0f, trapState.getCooldownRemaining() - upgrade.getValue()));
                }
            }
            case DURATION_EXT -> {
                plant.plantFoodDurationRemaining += upgrade.getValue();
                if (plant.lifespanRemaining > 0f) {
                    plant.lifespanRemaining += upgrade.getValue();
                    if (plant.lifespanTotal > 0f) {
                        plant.lifespanTotal += upgrade.getValue();
                    }
                }
            }
            default -> { }
        }
    }

    private static void applyLifespanExt(PlantInstance plant, LevelUpgrade upgrade) {
        if (plant.lifespanRemaining > 0) {
            plant.lifespanRemaining += upgrade.getValue();
        } else if (plant.lifespanRemaining < 0 && plant.definition.isShroom()) {
            plant.lifespanRemaining = PlantInstance.SHROOM_BASE_LIFESPAN + upgrade.getValue();
        }
    }
}
