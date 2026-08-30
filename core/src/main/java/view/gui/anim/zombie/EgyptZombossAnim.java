package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.zomboss.EgyptZombossBehavior;
import model.zombie.behavior.zomboss.ZombossAction;
import model.zombie.behavior.zomboss.ZombossBehavior;
import model.zombie.behavior.zomboss.ZombossPhase;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

public final class EgyptZombossAnim {
    public static final String DEFINITION_NAME = EgyptZombossBehavior.DEFINITION_NAME;

    public static final String INTRO_CLIP = "intro";
    public static final String STUN_START_CLIP = "stun_start";
    public static final String STUN_LOOP_CLIP = "stun_loop";
    public static final String STUN_END_CLIP = "stun_end";
    public static final String DIE_CLIP = "die_idle";
    public static final String MISSILE_START_CLIP = "missile_start";
    public static final String ROCKET_LAUNCH_CLIP = "rocket_launch";
    public static final String WALK_FORWARD_CLIP = "walk_forward";
    public static final String WALK_BACKWARDS_CLIP = "walk_backwards";
    public static final String WALK_DOWN_CLIP = "walk_down";
    public static final String WALK_UP_CLIP = "walk_up";
    public static final String PORTAL_START_CLIP = "zombie_portal_start";
    public static final String PORTAL_END_CLIP = "zombie_portal_end";

    private EgyptZombossAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, EgyptZombossAnim::resolve);
    }

    public static boolean isEgyptZomboss(ZombieInstance zombie) {
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
            return AnimPose.looping(entry.path(), "idle", ZombieAnimRole.IDLE);
        }
        return switch (boss.getPhase()) {
            case INTRO -> AnimPose.once(entry.path(), INTRO_CLIP, ZombieAnimRole.EATING, null);
            case STUNNED -> stunPose(entry, boss);
            case DYING -> AnimPose.once(entry.path(), DIE_CLIP, ZombieAnimRole.DIE, null);
            case ACTION -> actionPose(entry, boss);
            case IDLE -> AnimPose.looping(entry.path(), "idle", ZombieAnimRole.IDLE);
        };
    }

    private static AnimPose stunPose(PamCatalog.PamEntry entry, ZombossBehavior boss) {
        float total = Math.max(0.001f, boss.currentPhaseDurationSeconds());
        float elapsed = boss.phaseProgress01() * total;
        float startDur = PamCatalog.clipDurationSeconds(entry, STUN_START_CLIP);
        float endDur = PamCatalog.clipDurationSeconds(entry, STUN_END_CLIP);
        if (startDur <= 0f) {
            startDur = 0.4667f;
        }
        if (endDur <= 0f) {
            endDur = 0.1f;
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
            return AnimPose.looping(entry.path(), "idle", ZombieAnimRole.IDLE);
        }
        EgyptZombossBehavior egypt = boss instanceof EgyptZombossBehavior e ? e : null;
        return switch (action) {
            case MISSILE -> missilePose(entry, boss);
            case CHARGE -> {
                boolean forward = egypt == null || egypt.isChargingForward();
                yield AnimPose.looping(entry.path(),
                        forward ? WALK_FORWARD_CLIP : WALK_BACKWARDS_CLIP,
                        ZombieAnimRole.IDLE);
            }
            case CHANGE_LANE -> {
                int delta = egypt == null ? 0 : egypt.laneChangeDelta();
                String clip = delta >= 0 ? WALK_DOWN_CLIP : WALK_UP_CLIP;
                yield AnimPose.looping(entry.path(), clip, ZombieAnimRole.IDLE);
            }
            case SUMMON -> summonPose(entry, boss);
            default -> AnimPose.looping(entry.path(), "idle", ZombieAnimRole.IDLE);
        };
    }

    private static AnimPose summonPose(PamCatalog.PamEntry entry, ZombossBehavior boss) {
        float total = Math.max(0.001f, boss.currentPhaseDurationSeconds());
        float elapsed = boss.phaseProgress01() * total;
        if (elapsed < EgyptZombossBehavior.PORTAL_START_SECONDS) {
            return AnimPose.once(entry.path(), PORTAL_START_CLIP, ZombieAnimRole.EATING, null);
        }
        return AnimPose.once(entry.path(), PORTAL_END_CLIP, ZombieAnimRole.EATING, null);
    }

    private static AnimPose missilePose(PamCatalog.PamEntry entry, ZombossBehavior boss) {
        float total = Math.max(0.001f, boss.currentPhaseDurationSeconds());
        float elapsed = boss.phaseProgress01() * total;
        if (elapsed < EgyptZombossBehavior.MISSILE_START_SECONDS) {
            return AnimPose.once(entry.path(), MISSILE_START_CLIP, ZombieAnimRole.EATING, null);
        }
        float launchEnd = EgyptZombossBehavior.MISSILE_START_SECONDS
                + EgyptZombossBehavior.ROCKET_LAUNCH_SECONDS;
        if (elapsed < launchEnd) {
            return AnimPose.once(entry.path(), ROCKET_LAUNCH_CLIP, ZombieAnimRole.EATING, null);
        }
        return AnimPose.looping(entry.path(), "idle", ZombieAnimRole.IDLE);
    }
}
