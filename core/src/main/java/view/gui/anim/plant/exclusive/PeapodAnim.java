package view.gui.anim.plant.exclusive;

import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

public final class PeapodAnim {
    private PeapodAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Pea Pod", PeapodAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role == null) {
            return null;
        }

        int stackCount = plant.getStackCount();

        if (role == PlantAnimRole.IDLE) {
            String clipName = (stackCount == 1) ? "idle" : "idle" + stackCount;
            return AnimPose.looping(entry.path(), clipName, role);
        }

        if (role == PlantAnimRole.ATTACK) {
            String clipName = (stackCount == 1) ? "attack" : "attack " + stackCount;
            return AnimPose.once(entry.path(), clipName, role);
        }

        return null;
    }
}
