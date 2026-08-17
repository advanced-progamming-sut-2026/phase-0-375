package view.gui.anim.plant.exclusive;

import model.enums.PlantAbilityType;
import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

public final class PotatoMineAnim {
    private PotatoMineAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Potato Mine", PotatoMineAnim::resolve);
        overrides.register("Primal Potato Mine", PotatoMineAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role == null) {
            return null;
        }
        return switch (role) {
            case IDLE -> AnimPose.looping(entry.path(), isArmed(plant) ? "idle" : "plant_idle", role);
            case ATTACK -> AnimPose.once(entry.path(), "attack", role);
            case PLANT_FOOD_ON -> AnimPose.once(entry.path(), "plantfood_on", role);
            case PLANT_FOOD -> AnimPose.looping(entry.path(), "plantfood", role);
            case PLANT_FOOD_OFF -> AnimPose.once(entry.path(), "plantfood_off", role);
        };
    }

    private static boolean isArmed(PlantInstance plant) {
        AbilityState state = plant.getAbilityState(PlantAbilityType.DELAYED_EXPLOSIVE);
        return state != null && state.isArmed();
    }
}
