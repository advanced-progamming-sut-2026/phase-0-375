package view.gui.anim.plant.exclusive;

import model.enums.PlantAbilityType;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

public final class SunshroomAnim {
    private SunshroomAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Sun-shroom", SunshroomAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role == null) {
            return null;
        }
        int stage = visualStage(plant);
        return switch (role) {
            case IDLE -> AnimPose.looping(entry.path(), "idle_stage" + stage, role);
            case SPECIAL -> AnimPose.once(entry.path(), "special_stage" + stage, role);
            case PLANT_FOOD_ON, PLANT_FOOD, PLANT_FOOD_OFF ->
                    AnimPose.once(entry.path(), "plantfood_stage" + stage, role);
            default -> null;
        };
    }

    private static int visualStage(PlantInstance plant) {
        AbilityState state = plant.getAbilityState(PlantAbilityType.PRODUCE_SUN);
        int growth = state == null ? 0 : Math.max(0, state.getGrowthStage());
        return Math.min(3, growth + 1);
    }
}
