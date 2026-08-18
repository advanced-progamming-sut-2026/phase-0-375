package view.gui.anim.plant.exclusive;

import model.enums.PlantState;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.PamVisibility;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

import java.util.Map;

public final class MagnetShroomAnim {
    private static final String ITEM_PLACEHOLDER = "Magnet_Item";

    private MagnetShroomAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Magnet-shroom", MagnetShroomAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role == null) {
            return null;
        }

        boolean holding = isHoldingMetal(plant);
        Map<String, Boolean> vis = PamVisibility.hide(ITEM_PLACEHOLDER);

        return switch (role) {
            case ATTACK -> AnimPose.once(entry.path(), "catch", role, vis);
            case IDLE -> AnimPose.looping(entry.path(), holding ? "busy" : "idle", role, vis);
            case PLANT_FOOD_ON -> AnimPose.once(entry.path(), "plantfood_on", role, vis);
            case PLANT_FOOD -> AnimPose.looping(entry.path(), "plantfood", role, vis);
            case PLANT_FOOD_OFF -> AnimPose.once(entry.path(), "plantfood_off", role, vis);
            default -> null;
        };
    }

    /** Metal is visible during the catch clip and while the pull cooldown remains. */
    private static boolean isHoldingMetal(PlantInstance plant) {
        if (plant.getState() == PlantState.ATTACKING || plant.getState() == PlantState.PLANT_FOOD) {
            return true;
        }
        AbilityState state = plant.getAbilityState(plant.getDefinition().getAbilityType());
        return state != null && state.getHeldMetal() != null && state.getCooldownRemaining() > 0f;
    }
}
