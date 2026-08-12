package view.gui.anim.zombie;

import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Template for a zombie-specific animation profile.
 *
 * <p>Copy into a new class (e.g. {@code NewspaperAnim}), implement {@link #resolve},
 * and call {@code register} from {@link ZombieAnimProfiles#registerAll}.
 *
 * <p>Returning {@code null} keeps {@link ZombieAnimAdapter}'s global default.
 */
public final class ZombieAnimProfileTemplate {
    private ZombieAnimProfileTemplate() {}

    public static final String DEFINITION_NAME = "ExampleZombie";

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, ZombieAnimProfileTemplate::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry, ZombieAnimRole role) {
        // TODO: map zombie state / armor / specials → exclusive clip or visibility.
        return null;
    }
}
