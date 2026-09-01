package model.plant.instance;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantState;
import model.enums.PlantTags;
import model.plant.definition.Plant;

import java.util.EnumMap;
import java.util.Map;

/** Constructor logic extracted from {@link PlantInstance}. */
final class PlantInstanceInit {

    private PlantInstanceInit() {}

    static void initialize(PlantInstance plant, Plant definition) {
        plant.definition = definition;
        plant.state = PlantState.IDLE;
        plant.currentHP = definition.getBaseHP();
        plant.armorHP = 0;
        plant.armorMaxHP = 0;
        plant.reflectDamageBonus = 0;
        plant.armorExplodesOnBreak = false;
        plant.pendingArmorExplosion = false;
        plant.armorBreakEpoch = 0;
        plant.deathDetonated = false;
        plant.level = 1;
        plant.currentRecharge = definition.getRechargeTime();
        plant.isPlantFoodActive = false;
        plant.plantFoodDurationRemaining = 0f;
        plant.pendingPlantFoodEffect = false;
        plant.activeAction = null;
        plant.actionEpoch = 0;
        plant.lifespanRemaining = -1f;
        plant.lifespanTotal = 0f;
        plant.freezeHitCount = 0;
        plant.stateBeforeFreeze = null;
        plant.stateBeforeTransform = null;
        plant.stackCount = 1;
        plant.imitateTarget = null;
        plant.transformCountdown = -1f;
        initAbility(plant, definition);
        initLifespan(plant, definition);
    }

    static Map<PlantAbilityType, AbilityState> newAbilityMap() {
        return new EnumMap<>(PlantAbilityType.class);
    }

    private static void initAbility(PlantInstance plant, Plant definition) {
        if (definition.getAbilityType() == null) {
            return;
        }
        AbilityState abilityState = new AbilityState(definition.getAbilityType());
        if (definition.hasTag(PlantTags.TRAP)) {
            abilityState.setArmed(false);
            if (definition.hasTag(PlantTags.CHARGE) && definition.getActionInterval() > 0) {
                abilityState.setCooldownRemaining(definition.getActionInterval());
                plant.state = PlantState.ARMING;
            }
        } else if (definition.hasTag(PlantTags.CHARGE)
                && definition.getCategory() == PlantCategory.SHOOTER
                && definition.getActionInterval() >= 5f) {
            abilityState.setCooldownRemaining(definition.getActionInterval());
            plant.state = PlantState.ARMING;
        }
        plant.abilityStates.put(definition.getAbilityType(), abilityState);
        if (definition.getAbilityType() == PlantAbilityType.PRODUCE_SUN
                && definition.getActionInterval() > 0f) {
            abilityState.setCooldownRemaining(definition.getActionInterval());
        }
    }

    private static void initLifespan(PlantInstance plant, Plant definition) {
        if (definition.hasTag(PlantTags.WARM_UP)) {
            plant.lifespanRemaining = -1f;
        } else if (definition.isShroom() && !PlantInstance.isImitater(definition)) {
            plant.lifespanRemaining = PlantInstance.SHROOM_BASE_LIFESPAN;
        }
        if (PlantInstance.isImitater(definition)) {
            plant.transformCountdown = PlantInstance.IMITATER_TRANSFORM_DELAY;
            if (plant.currentHP <= 0) {
                plant.currentHP = 300;
            }
        }
        if (PlantInstance.isMint(definition)) {
            if (plant.currentHP <= 0) {
                plant.currentHP = PlantInstance.MINT_BASE_HP;
            }
            float stay = definition.getAbilityValue();
            if (stay <= 0f) {
                stay = PlantInstance.MINT_DEFAULT_DURATION;
            }
            plant.lifespanRemaining = stay;
            plant.lifespanTotal = stay;
        }
    }
}
