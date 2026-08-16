package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.BuffBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Dark Ages King: {@code intro} on spawn, weighted {@code idle}/{@code idle2}
 * loops, {@code special} while knighting. {@code die} falls through.
 */
public final class DarkKingAnim {
    public static final String DEFINITION_NAME = "ZombieDarkKing";
    public static final String INTRO_CLIP = "intro";
    public static final String IDLE_CLIP = "idle";
    public static final String IDLE2_CLIP = "idle2";
    public static final String SPECIAL_CLIP = "special";

    private DarkKingAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, DarkKingAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        BuffBehavior buff = (BuffBehavior) zombie.getBehavior(ZombieBehaviorType.BUFF);
        if (buff == null) {
            return AnimPose.looping(entry.path(), IDLE_CLIP, ZombieAnimRole.IDLE);
        }
        return switch (buff.getPhase()) {
            case INTRO -> AnimPose.once(entry.path(), INTRO_CLIP, ZombieAnimRole.EATING, null);
            case SPECIAL -> AnimPose.once(entry.path(), SPECIAL_CLIP, ZombieAnimRole.EATING, null);
            case IDLE -> AnimPose.looping(entry.path(),
                    buff.isIdle2() ? IDLE2_CLIP : IDLE_CLIP, ZombieAnimRole.IDLE);
        };
    }
}
