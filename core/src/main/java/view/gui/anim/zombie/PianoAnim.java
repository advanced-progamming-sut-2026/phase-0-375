package view.gui.anim.zombie;

import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Pianist: looping {@code play} while alive (no walk cycle). Die falls through
 * to the global default.
 */
public final class PianoAnim {
    private PianoAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register("ZombiePiano", PianoAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        return AnimPose.looping(entry.path(), "play", ZombieAnimRole.EATING);
    }
}
