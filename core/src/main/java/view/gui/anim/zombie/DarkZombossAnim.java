package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.zomboss.DarkZombossBehavior;
import model.zombie.behavior.zomboss.ZombossAction;
import model.zombie.behavior.zomboss.ZombossBehavior;
import model.zombie.behavior.zomboss.ZombossPhase;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

public final class DarkZombossAnim {
    public static final String DEFINITION_NAME = DarkZombossBehavior.DEFINITION_NAME;

    public static final String INTRO_CLIP = "intro";
    public static final String STUN_START_CLIP = "stun_start";
    public static final String STUN_LOOP_CLIP = "stun_loop";
    public static final String STUN_END_CLIP = "stun_end";

    private DarkZombossAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, DarkZombossAnim::resolve);
    }

    public static boolean isDarkZomboss(ZombieInstance zombie) {
        return zombie != null
                && zombie.getDefinition() != null
                && DEFINITION_NAME.equals(zombie.getDefinition().getName());
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null) {
            return null;
        }
        if (role == ZombieAnimRole.DIE) {
            return AnimPose.once(entry.path(), "die", ZombieAnimRole.DIE, null);
        }
        ZombossBehavior boss = (ZombossBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (boss == null) {
            return AnimPose.looping(entry.path(), "idle", ZombieAnimRole.IDLE);
        }
        return switch (boss.getPhase()) {
            case INTRO -> AnimPose.once(entry.path(), INTRO_CLIP, ZombieAnimRole.EATING, null);
            case STUNNED -> stunPose(entry, boss);
            case DYING -> AnimPose.once(entry.path(), "die", ZombieAnimRole.DIE, null);
            case ACTION -> actionPose(entry, boss.getCurrentAction(), boss);
            case IDLE -> AnimPose.looping(entry.path(), "idle", ZombieAnimRole.IDLE);
        };
    }

    private static AnimPose stunPose(PamCatalog.PamEntry entry, ZombossBehavior boss) {
        float total = Math.max(0.001f, boss.currentPhaseDurationSeconds());
        float elapsed = boss.phaseProgress01() * total;
        float startDur = PamCatalog.clipDurationSeconds(entry, STUN_START_CLIP);
        float endDur = PamCatalog.clipDurationSeconds(entry, STUN_END_CLIP);
        if (startDur <= 0f) {
            startDur = 0.4333f;
        }
        if (endDur <= 0f) {
            endDur = 0.4667f;
        }
        if (elapsed < startDur) {
            return AnimPose.once(entry.path(), STUN_START_CLIP, ZombieAnimRole.EATING, null);
        }
        float endAt = Math.max(startDur, total - endDur);
        if (elapsed >= endAt) {
            return AnimPose.once(entry.path(), STUN_END_CLIP, ZombieAnimRole.EATING, null);
        }
        return AnimPose.looping(entry.path(), STUN_LOOP_CLIP, ZombieAnimRole.IDLE);
    }

    private static AnimPose actionPose(PamCatalog.PamEntry entry, ZombossAction action,
                                       ZombossBehavior boss) {
        if (action == null) {
            return AnimPose.looping(entry.path(), "idle", ZombieAnimRole.IDLE);
        }
        return switch (action) {
            case FIREBALLS -> AnimPose.once(entry.path(), "fire_bomb", ZombieAnimRole.EATING, null);
            case BURN_ROWS -> burnRowsPose(entry, boss);
            case SUMMON -> AnimPose.once(entry.path(), "summoning", ZombieAnimRole.EATING, null);
            case CHANGE_LANE -> AnimPose.looping(entry.path(), "idle", ZombieAnimRole.IDLE);
            default -> AnimPose.looping(entry.path(), "idle", ZombieAnimRole.IDLE);
        };
    }

    private static AnimPose burnRowsPose(PamCatalog.PamEntry entry, ZombossBehavior boss) {
        float total = DarkZombossBehavior.burnRowsDurationSeconds();
        float elapsed = boss.phaseProgress01() * total;
        if (elapsed < DarkZombossBehavior.FIRE_ATTACK_START_SECONDS) {
            return AnimPose.once(entry.path(), "fire_attack", ZombieAnimRole.EATING, null);
        }
        float endAt = total - DarkZombossBehavior.FIRE_ATTACK_END_SECONDS;
        if (elapsed < endAt) {
            return AnimPose.looping(entry.path(), "fire_attack_idle", ZombieAnimRole.IDLE);
        }
        return AnimPose.once(entry.path(), "fire_attack_end", ZombieAnimRole.EATING, null);
    }
}
