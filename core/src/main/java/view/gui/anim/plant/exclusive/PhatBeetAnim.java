package view.gui.anim.plant.exclusive;

import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

public final class PhatBeetAnim {
    private PhatBeetAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Phat Beet", PhatBeetAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role == null) {
            return null;
        }
        return switch (role) {
            case IDLE -> AnimPose.looping(entry.path(), "idle", role);
            case ATTACK -> AnimPose.once(entry.path(), "attack", role);
            case PLANT_FOOD_ON, PLANT_FOOD, PLANT_FOOD_OFF ->
                    AnimPose.looping(entry.path(), "plantfood", role);
            default -> null;
        };
    }
}
