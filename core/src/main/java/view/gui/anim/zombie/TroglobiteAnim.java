package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.PushBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Ice Age Troglobite: one-shot {@code push} while shoving ice. Walk / eat / die
 * fall through to the global defaults.
 */
public final class TroglobiteAnim {
    public static final String DEFINITION_NAME = "ZombieIceAgeTroglobite";
    public static final String PUSH_CLIP = "push";
    /** Overlay on a frozen occupant or a pushed ice cube. Clip is {@code idle}. */
    public static final String ICE_PAM = "FROSTBITE_ICE_BLOCK_ZOMBIE";
    /** Shatter FX. Clip is {@code animation}. */
    public static final String ICE_BREAK_PAM = "FROSTBITE_ICE_BLOCK_PARTICLES";

    private TroglobiteAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, TroglobiteAnim::resolve);
    }

    public static boolean isIcePropPam(String pamPath) {
        return pamPath != null && pamPath.toUpperCase().contains("FROSTBITE_ICE_BLOCK_ZOMBIE")
                && !pamPath.toUpperCase().contains("BEHIND")
                && !pamPath.toUpperCase().contains("PARTICLES");
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
        return AnimPose.once(entry.path(), PUSH_CLIP, ZombieAnimRole.EATING, null);
    }
}
