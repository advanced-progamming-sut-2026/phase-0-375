package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.zomboss.BeachZombossBehavior;
import model.zombie.behavior.zomboss.ZombossAction;
import model.zombie.behavior.zomboss.ZombossBehavior;
import model.zombie.behavior.zomboss.ZombossPhase;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

public final class BeachZombossAnim {
    public static final String DEFINITION_NAME = BeachZombossBehavior.DEFINITION_NAME;

    public static final String INTRO_CLIP = "intro";
    public static final String IDLE_CLIP = "idle";
    public static final String STUN_START_CLIP = "stun_start";
    public static final String STUN_LOOP_CLIP = "stun_loop";
    public static final String STUN_END_CLIP = "stun_end";
    public static final String DIE_CLIP = "die";
    public static final String SPAWN_CLIP = "spawn";
    public static final String SUBMERGE_CLIP = "submerge";
    public static final String EMERGE_CLIP = "emerge";
    public static final String SUCTION_ON_CLIP = "suction_on";
    public static final String SUCTION_LOOP_CLIP = "suction_loop";
    public static final String SUCTION_OFF_CLIP = "suction_off";

    private BeachZombossAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, BeachZombossAnim::resolve);
    }

    public static boolean isBeachZomboss(ZombieInstance zombie) {
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
            return AnimPose.once(entry.path(), DIE_CLIP, ZombieAnimRole.DIE, null);
        }
        ZombossBehavior boss = (ZombossBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (boss == null) {
            return AnimPose.looping(entry.path(), IDLE_CLIP, ZombieAnimRole.IDLE);
        }
        return switch (boss.getPhase()) {
            case INTRO -> AnimPose.once(entry.path(), INTRO_CLIP, ZombieAnimRole.EATING, null);
            case STUNNED -> stunPose(entry, boss);
            case DYING -> AnimPose.once(entry.path(), DIE_CLIP, ZombieAnimRole.DIE, null);
            case ACTION -> actionPose(entry, boss);
            case IDLE -> AnimPose.looping(entry.path(), IDLE_CLIP, ZombieAnimRole.IDLE);
        };
    }

    private static AnimPose stunPose(PamCatalog.PamEntry entry, ZombossBehavior boss) {
        float total = Math.max(0.001f, boss.currentPhaseDurationSeconds());
        float elapsed = boss.phaseProgress01() * total;
        float startDur = PamCatalog.clipDurationSeconds(entry, STUN_START_CLIP);
        float endDur = PamCatalog.clipDurationSeconds(entry, STUN_END_CLIP);
        if (startDur <= 0f) {
            startDur = 0.1333f;
        }
        if (endDur <= 0f) {
            endDur = 1.1f;
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

    private static AnimPose actionPose(PamCatalog.PamEntry entry, ZombossBehavior boss) {
        ZombossAction action = boss.getCurrentAction();
        if (action == null) {
            return AnimPose.looping(entry.path(), IDLE_CLIP, ZombieAnimRole.IDLE);
        }
        BeachZombossBehavior beach = boss instanceof BeachZombossBehavior b ? b : null;
        return switch (action) {
            case BABY_SHARK, SUMMON -> spawnPose(entry, boss);
            case TURBINE -> turbinePose(entry, beach);
            case CHANGE_LANE -> laneChangePose(entry, beach);
            default -> AnimPose.looping(entry.path(), IDLE_CLIP, ZombieAnimRole.IDLE);
        };
    }

    private static AnimPose spawnPose(PamCatalog.PamEntry entry, ZombossBehavior boss) {
        float total = Math.max(0.001f, boss.currentPhaseDurationSeconds());
        float elapsed = boss.phaseProgress01() * total;
        if (actionUsesSpawnOnly(boss) && elapsed < BeachZombossBehavior.SPAWN_SECONDS) {
            return AnimPose.once(entry.path(), SPAWN_CLIP, ZombieAnimRole.EATING, null);
        }
        return AnimPose.looping(entry.path(), IDLE_CLIP, ZombieAnimRole.IDLE);
    }

    private static boolean actionUsesSpawnOnly(ZombossBehavior boss) {
        ZombossAction action = boss.getCurrentAction();
        return action == ZombossAction.SUMMON || action == ZombossAction.BABY_SHARK;
    }

    private static AnimPose turbinePose(PamCatalog.PamEntry entry, BeachZombossBehavior beach) {
        float elapsed = beach == null ? 0f : beach.turbineElapsedSeconds();
        if (elapsed < BeachZombossBehavior.SUCTION_ON_SECONDS) {
            return AnimPose.once(entry.path(), SUCTION_ON_CLIP, ZombieAnimRole.EATING, null);
        }
        float loopEnd = BeachZombossBehavior.SUCTION_ON_SECONDS
                + BeachZombossBehavior.SUCTION_LOOP_SECONDS;
        if (elapsed < loopEnd) {
            return AnimPose.looping(entry.path(), SUCTION_LOOP_CLIP, ZombieAnimRole.IDLE);
        }
        return AnimPose.once(entry.path(), SUCTION_OFF_CLIP, ZombieAnimRole.EATING, null);
    }

    private static AnimPose laneChangePose(PamCatalog.PamEntry entry, BeachZombossBehavior beach) {
        float elapsed = beach == null ? 0f
                : beach.phaseProgress01() * (BeachZombossBehavior.SUBMERGE_SECONDS
                + BeachZombossBehavior.EMERGE_SECONDS);
        if (elapsed < BeachZombossBehavior.SUBMERGE_SECONDS) {
            return AnimPose.once(entry.path(), SUBMERGE_CLIP, ZombieAnimRole.EATING, null);
        }
        return AnimPose.once(entry.path(), EMERGE_CLIP, ZombieAnimRole.EATING, null);
    }
}
