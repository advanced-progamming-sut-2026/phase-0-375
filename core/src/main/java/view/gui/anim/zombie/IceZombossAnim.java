package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.zomboss.IceZombossBehavior;
import model.zombie.behavior.zomboss.ZombossAction;
import model.zombie.behavior.zomboss.ZombossBehavior;
import model.zombie.behavior.zomboss.ZombossPhase;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

public final class IceZombossAnim {
    public static final String DEFINITION_NAME = IceZombossBehavior.DEFINITION_NAME;

    public static final String INTRO_CLIP = "intro";
    public static final String IDLE_CLIP = "idle";
    public static final String STUN_CLIP = "stun";
    public static final String DIE_CLIP = "die";
    public static final String SLINGSHOT_CLIP = "slingshot";
    public static final String WIND_CLIP_PREFIX = "wind_";
    public static final String GLACIER_CLIP_PREFIX = "glacier_column_";

    private IceZombossAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, IceZombossAnim::resolve);
    }

    public static boolean isIceZomboss(ZombieInstance zombie) {
        return zombie != null
                && zombie.getDefinition() != null
                && DEFINITION_NAME.equals(zombie.getDefinition().getName());
    }

    public static String windClip(int pamIndex) {
        int i = Math.max(IceZombossBehavior.WIND_PAM_INDEX_MIN,
                Math.min(IceZombossBehavior.WIND_PAM_INDEX_MAX, pamIndex));
        return WIND_CLIP_PREFIX + i;
    }

    public static String glacierClip(int pamIndex) {
        int i = Math.max(IceZombossBehavior.GLACIER_PAM_INDEX_MIN,
                Math.min(IceZombossBehavior.GLACIER_PAM_INDEX_MAX, pamIndex));
        return GLACIER_CLIP_PREFIX + i;
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null) {
            return null;
        }
        if (role == ZombieAnimRole.DIE) {
            return AnimPose.once(entry.path(), DIE_CLIP, ZombieAnimRole.DIE, null);
        }
        ZombossBehavior boss = (ZombossBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (boss == null) {
            return AnimPose.looping(entry.path(), IDLE_CLIP, ZombieAnimRole.IDLE);
        }
        return switch (boss.getPhase()) {
            case INTRO -> AnimPose.once(entry.path(), INTRO_CLIP, ZombieAnimRole.EATING, null);
            case STUNNED -> AnimPose.looping(entry.path(), STUN_CLIP, ZombieAnimRole.IDLE);
            case DYING -> AnimPose.once(entry.path(), DIE_CLIP, ZombieAnimRole.DIE, null);
            case ACTION -> actionPose(entry, boss);
            case IDLE -> AnimPose.looping(entry.path(), IDLE_CLIP, ZombieAnimRole.IDLE);
        };
    }

    private static AnimPose actionPose(PamCatalog.PamEntry entry, ZombossBehavior boss) {
        ZombossAction action = boss.getCurrentAction();
        if (action == null) {
            return AnimPose.looping(entry.path(), IDLE_CLIP, ZombieAnimRole.IDLE);
        }
        IceZombossBehavior ice = boss instanceof IceZombossBehavior i ? i : null;
        return switch (action) {
            case ICE_MISSILE -> {
                float elapsed = boss.phaseProgress01() * boss.currentPhaseDurationSeconds();
                if (elapsed < IceZombossBehavior.SLINGSHOT_SECONDS) {
                    yield AnimPose.once(entry.path(), SLINGSHOT_CLIP, ZombieAnimRole.EATING, null);
                }
                yield AnimPose.looping(entry.path(), IDLE_CLIP, ZombieAnimRole.IDLE);
            }
            case ICE_WIND -> AnimPose.once(entry.path(),
                    windClip(ice == null ? 1 : ice.getWindPamIndex()),
                    ZombieAnimRole.EATING, null);
            case FREEZE_COLUMN -> AnimPose.once(entry.path(),
                    glacierClip(ice == null ? 1 : ice.getGlacierPamIndex()),
                    ZombieAnimRole.EATING, null);
            default -> AnimPose.looping(entry.path(), IDLE_CLIP, ZombieAnimRole.IDLE);
        };
    }
}
