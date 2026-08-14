package view.gui.anim.zombie;

import model.zombie.behavior.ThrowImpBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Thrown Imp: one-frame {@code fly} in the air, then one-shot {@code land}.
 */
public final class ImpAnim {
    private ImpAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register("ZombieImp", ImpAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        ThrowImpBehavior.Flight flight = ThrowImpBehavior.flightOf(zombie);
        if (flight == null) {
            return null;
        }
        if (flight.isFlying()) {
            return AnimPose.once(entry.path(), "fly", ZombieAnimRole.IDLE, null);
        }
        if (flight.isLanding()) {
            return AnimPose.once(entry.path(), "land", ZombieAnimRole.IDLE, null);
        }
        return null;
    }
}
