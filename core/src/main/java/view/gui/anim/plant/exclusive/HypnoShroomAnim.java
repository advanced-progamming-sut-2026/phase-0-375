package view.gui.anim.plant.exclusive;

import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

public final class HypnoShroomAnim {
    private HypnoShroomAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Hypno-shroom", HypnoShroomAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role == null) {
            return null;
        }
        return switch (role) {
            case IDLE -> AnimPose.looping(entry.path(), "idle", role);
            case PLANT_FOOD_ON -> AnimPose.once(entry.path(), "plantfood_on", role);
            case PLANT_FOOD, PLANT_FOOD_OFF -> AnimPose.looping(entry.path(), "plantfood", role);
            case ATTACK -> null;
        };
    }
}
