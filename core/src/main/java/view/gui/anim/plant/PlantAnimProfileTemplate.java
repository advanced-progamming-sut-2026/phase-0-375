package view.gui.anim.plant;

import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Template for a plant-specific animation profile.
 *
 * <p>Copy into a new class (e.g. {@code SunshroomAnim}), implement {@link #resolve},
 * and call {@code register} from {@link PlantAnimProfiles#registerAll}.
 *
 * <p>Returning {@code null} keeps {@link PlantAnimAdapter}'s global default.
 */
public final class PlantAnimProfileTemplate {
    private PlantAnimProfileTemplate() {}

    public static final String DEFINITION_NAME = "Example Plant";

    public static void register(PlantAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, PlantAnimProfileTemplate::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        // TODO: map plant state / HP / tags → exclusive clip or visibility.
        return null;
    }
}
