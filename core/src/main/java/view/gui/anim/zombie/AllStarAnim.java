package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.SmashBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * All-Star charge: looping {@code run}, then one-shot {@code tackle} + {@code kick}.
 * After the charge, walk / eat / die fall through to the global defaults.
 */
public final class AllStarAnim {
    private AllStarAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register("ZombieModernAllStar", AllStarAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        SmashBehavior smash = (SmashBehavior) zombie.getBehavior(ZombieBehaviorType.SMASH);
        if (smash == null) {
            return null;
        }
        return switch (smash.getAllStarPhase()) {
            case RUNNING -> AnimPose.looping(entry.path(), "run", ZombieAnimRole.WALK);
            case TACKLING -> AnimPose.once(entry.path(), "tackle", ZombieAnimRole.EATING, null);
            case KICKING -> AnimPose.once(entry.path(), "kick", ZombieAnimRole.EATING, null);
            case WALKING -> null;
        };
    }
}
