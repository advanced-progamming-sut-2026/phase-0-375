package view.gui.anim.plant.exclusive;

import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

/**
 * Fume-shroom has no {@code attack} clip; the spray lives on {@code special}.
 */
public final class FumeShroomAnim {
    private FumeShroomAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Fume-shroom", FumeShroomAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role != PlantAnimRole.ATTACK) {
            return null;
        }
        return AnimPose.once(entry.path(), "special", role);
    }
}
