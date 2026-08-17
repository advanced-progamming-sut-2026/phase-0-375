package view.gui.anim.plant.exclusive;

import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

public final class LilyPadAnim {
    private static final String[] IDLE_CLIPS = {"idle3", "idle4", "idle5"};

    private LilyPadAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Lily Pad", LilyPadAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role == null) {
            return null;
        }
        return switch (role) {
            case IDLE -> AnimPose.looping(entry.path(), idleClip(plant), role);
            case PLANT_FOOD_ON, PLANT_FOOD, PLANT_FOOD_OFF ->
                    AnimPose.looping(entry.path(), "plantfood", role);
            case ATTACK -> null;
        };
    }

    private static String idleClip(PlantInstance plant) {
        int index = Math.floorMod(System.identityHashCode(plant), IDLE_CLIPS.length);
        return IDLE_CLIPS[index];
    }
}
