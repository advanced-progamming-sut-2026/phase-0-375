package model.plant.ability;

import model.enums.PlantAbilityType;
import model.enums.PlantCategory;
import model.enums.PlantFoodType;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;

/**
 * Strategy for the {@link PlantCategory#MINT} family.
 */
public class MintAbility implements PlantAbility {

    @Override
    public PlantCategory getCategory() { return PlantCategory.MINT; }

    @Override
    public void execute(PlantInstance plant, PlantAbilityContext context) {
        // Mints act exactly once, on placement. The boost itself is
        // applied in onPlantFood flow. execute() is a no-op.
    }

    @Override
    public void onPlantFood(PlantInstance plant, PlantAbilityContext context) {
        Plant def = plant.getDefinition();
        PlantCategory family = resolveFamily(def);
        if (family != null) {
            context.triggerFamilyPlantFood(family);
        }
    }

    /** Determines which family this mint boosts. */
    private PlantCategory resolveFamily(Plant def) {
        if (def.getCategory() != PlantCategory.MINT) {
            return def.getCategory();
        }
        String name = def.getName().toLowerCase();
        if (name.contains("appease")) return PlantCategory.SHOOTER;
        if (name.contains("enlighten")) return PlantCategory.SUN_PRODUCER;
        if (name.contains("arma")) return PlantCategory.LOBBER;
        if (name.contains("bombard")) return PlantCategory.EXPLOSIVE;
        if (name.contains("enforce")) return PlantCategory.MELEE;
        if (name.contains("reinforce")) return PlantCategory.WALL_NUT;
        if (name.contains("enchant")) return PlantCategory.MODIFIER;
        if (name.contains("pierce")) return PlantCategory.STRIKE_THROUGH;
        if (name.contains("cattail") || name.contains("homing")) return PlantCategory.HOMING;
        return null;
    }
}
