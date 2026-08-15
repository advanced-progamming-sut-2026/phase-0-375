package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.PushBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Arcade Zombie: one-shot {@code push} while shoving the cabinet. Walk / eat / die
 * fall through to the global defaults.
 */
public final class ArcadeAnim {
    private ArcadeAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register("ZombieArcade", ArcadeAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        PushBehavior push = (PushBehavior) zombie.getBehavior(ZombieBehaviorType.PUSH);
        if (push == null || !push.isPushing()) {
            return null;
        }
        return AnimPose.once(entry.path(), "push", ZombieAnimRole.EATING, null);
    }
}
