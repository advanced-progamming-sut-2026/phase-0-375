package view.gui.anim.plant.exclusive;

import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

public final class ImitaterAnim {
    private ImitaterAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Imitater", ImitaterAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null) {
            return null;
        }
        if (plant.isImitating()) {
            return AnimPose.once(entry.path(), "attack", role != null ? role : PlantAnimRole.IDLE);
        }
        if (role == PlantAnimRole.IDLE) {
            return AnimPose.looping(entry.path(), "idle", role);
        }
        return null;
    }
}
