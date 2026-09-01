package model.plant.instance;

import model.app.App;
import model.enums.PlacableLayer;
import model.enums.PlantCategory;
import model.enums.PlantState;
import model.enums.PlantTags;
import model.game.map.Cell;
import model.plant.PlantFactory;
import model.plant.definition.Plant;

/** Imitater / wizard morph logic extracted from {@link PlantInstance}. */
final class PlantMorph {

    private PlantMorph() {}

    static void transformInto(PlantInstance plant, Plant newDefinition) {
        if (newDefinition == null) {
            return;
        }
        PlacableLayer oldLayer = plant.getLayer();
        plant.definition = newDefinition;
        if (plant.position != null && App.getInstance().getCurrentGameModel() != null) {
            Cell cell = App.getInstance().getCurrentGameModel()
                    .getCellAt(plant.position.getY(), plant.position.getX());
            if (cell != null) {
                cell.rekeyPlaceable(plant, oldLayer);
            }
        }
        plant.state = PlantState.IDLE;
        plant.currentHP = newDefinition.getBaseHP();
        plant.armorHP = 0;
        plant.armorMaxHP = 0;
        plant.reflectDamageBonus = 0;
        plant.armorExplodesOnBreak = false;
        plant.pendingArmorExplosion = false;
        plant.deathDetonated = false;
        plant.currentRecharge = newDefinition.getRechargeTime();
        plant.isPlantFoodActive = false;
        plant.plantFoodDurationRemaining = 0f;
        plant.pendingPlantFoodEffect = false;
        plant.activeAction = null;
        plant.abilityStates.clear();
        resetAbility(plant, newDefinition);
        plant.abilityStrategy = null;
        plant.imitateTarget = null;
        plant.bumpActionEpoch();
        if (newDefinition.isShroom() && !PlantInstance.isImitater(newDefinition)) {
            plant.lifespanRemaining = PlantInstance.SHROOM_BASE_LIFESPAN;
        } else {
            plant.lifespanRemaining = -1f;
        }
    }

    static void transformIntoImitated(PlantInstance plant) {
        if (plant.imitateTarget == null || plant.imitateTarget.isEmpty()) {
            return;
        }
        Plant newDef = null;
        try {
            newDef = PlantFactory.getDefinition(plant.imitateTarget);
        } catch (IllegalStateException ignored) {
            return;
        }
        if (newDef != null) {
            transformInto(plant, newDef);
        }
    }

    private static void resetAbility(PlantInstance plant, Plant newDefinition) {
        if (newDefinition.getAbilityType() == null) {
            return;
        }
        AbilityState fresh = new AbilityState(newDefinition.getAbilityType());
        if (newDefinition.hasTag(PlantTags.TRAP)) {
            fresh.setArmed(false);
        } else if (newDefinition.hasTag(PlantTags.CHARGE)
                && newDefinition.getCategory() == PlantCategory.SHOOTER
                && newDefinition.getActionInterval() >= 5f) {
            fresh.setCooldownRemaining(newDefinition.getActionInterval());
        }
        plant.abilityStates.put(newDefinition.getAbilityType(), fresh);
    }
}
