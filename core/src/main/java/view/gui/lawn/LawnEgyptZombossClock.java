package view.gui.lawn;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.zomboss.EgyptZombossBehavior;
import model.zombie.behavior.zomboss.ZombossAction;
import model.zombie.behavior.zomboss.ZombossBehavior;
import model.zombie.behavior.zomboss.ZombossPhase;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import view.gui.anim.AnimPose;
import view.gui.anim.zombie.EgyptZombossAnim;

final class LawnEgyptZombossClock {
    private final LawnEntityRenderer r;

    LawnEgyptZombossClock(LawnEntityRenderer r) {
        this.r = r;
    }

    void restart(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !EgyptZombossAnim.isEgyptZomboss(zombie)) {
            return;
        }
        String clip = pose.clipName();
        if (!isRestartClip(clip)) {
            return;
        }
        ZombossBehavior boss = (ZombossBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (boss == null || !shouldRestart(boss, clip)) {
            return;
        }
        LawnZombossClock.rewind(r, zombie);
    }

    float clipPhase(ZombieInstance zombie, AnimPose pose, ClipRef ref) {
        if (pose == null || ref == null || ref.duration <= 0f
                || !EgyptZombossAnim.isEgyptZomboss(zombie)) {
            return LawnEntityDrawConstants.NO_PHASE;
        }
        ZombossBehavior boss = (ZombossBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (boss == null) {
            return LawnEntityDrawConstants.NO_PHASE;
        }
        String clip = pose.clipName();
        float total = boss.currentPhaseDurationSeconds();
        float elapsed = boss.phaseProgress01() * total;
        if (boss.getPhase() == ZombossPhase.INTRO && EgyptZombossAnim.INTRO_CLIP.equals(clip)) {
            return LawnZombossClock.clip01(elapsed, ref.duration);
        }
        if (boss.getPhase() == ZombossPhase.STUNNED) {
            return stunPhase(clip, elapsed, total);
        }
        return actionPhase(boss, clip, elapsed, ref);
    }

    private static boolean isRestartClip(String clip) {
        return EgyptZombossAnim.INTRO_CLIP.equals(clip)
                || EgyptZombossAnim.STUN_START_CLIP.equals(clip)
                || EgyptZombossAnim.STUN_END_CLIP.equals(clip)
                || EgyptZombossAnim.MISSILE_START_CLIP.equals(clip)
                || EgyptZombossAnim.ROCKET_LAUNCH_CLIP.equals(clip)
                || EgyptZombossAnim.WALK_FORWARD_CLIP.equals(clip)
                || EgyptZombossAnim.WALK_BACKWARDS_CLIP.equals(clip)
                || EgyptZombossAnim.WALK_DOWN_CLIP.equals(clip)
                || EgyptZombossAnim.WALK_UP_CLIP.equals(clip)
                || EgyptZombossAnim.PORTAL_START_CLIP.equals(clip)
                || EgyptZombossAnim.PORTAL_END_CLIP.equals(clip);
    }

    private boolean shouldRestart(ZombossBehavior boss, String clip) {
        boolean phaseJustStarted = LawnZombossClock.phaseJustStarted(boss);
        return switch (boss.getPhase()) {
            case INTRO -> EgyptZombossAnim.INTRO_CLIP.equals(clip) && phaseJustStarted;
            case STUNNED -> stunRestart(boss, clip);
            case ACTION -> actionRestart(boss, clip, phaseJustStarted);
            default -> false;
        };
    }

    private boolean stunRestart(ZombossBehavior boss, String clip) {
        float total = boss.currentPhaseDurationSeconds();
        float elapsed = boss.phaseProgress01() * total;
        float[] dur = LawnZombossClock.stunDurations(r, EgyptZombossAnim.DEFINITION_NAME,
                EgyptZombossAnim.STUN_START_CLIP, EgyptZombossAnim.STUN_END_CLIP,
                0.4667f, 0.1f);
        return LawnZombossClock.stunStartJustStarted(clip, EgyptZombossAnim.STUN_START_CLIP, elapsed)
                || LawnZombossClock.stunEndJustStarted(clip, EgyptZombossAnim.STUN_END_CLIP,
                elapsed, total, dur[0], dur[1]);
    }

    private static boolean actionRestart(ZombossBehavior boss, String clip, boolean phaseJustStarted) {
        if (phaseJustStarted && (
                EgyptZombossAnim.MISSILE_START_CLIP.equals(clip)
                        || EgyptZombossAnim.WALK_FORWARD_CLIP.equals(clip)
                        || EgyptZombossAnim.WALK_DOWN_CLIP.equals(clip)
                        || EgyptZombossAnim.WALK_UP_CLIP.equals(clip)
                        || EgyptZombossAnim.PORTAL_START_CLIP.equals(clip))) {
            return true;
        }
        float total = boss.currentPhaseDurationSeconds();
        float elapsed = boss.phaseProgress01() * total;
        return rocketJustStarted(boss, clip, elapsed)
                || portalEndJustStarted(boss, clip, elapsed)
                || chargeBackJustStarted(boss, clip, elapsed);
    }

    private static boolean rocketJustStarted(ZombossBehavior boss, String clip, float elapsed) {
        return boss.getCurrentAction() == ZombossAction.MISSILE
                && EgyptZombossAnim.ROCKET_LAUNCH_CLIP.equals(clip)
                && Math.abs(elapsed - EgyptZombossBehavior.MISSILE_START_SECONDS) < 1f / 30f;
    }

    private static boolean portalEndJustStarted(ZombossBehavior boss, String clip, float elapsed) {
        return boss.getCurrentAction() == ZombossAction.SUMMON
                && EgyptZombossAnim.PORTAL_END_CLIP.equals(clip)
                && Math.abs(elapsed - EgyptZombossBehavior.PORTAL_START_SECONDS) < 1f / 30f;
    }

    private static boolean chargeBackJustStarted(ZombossBehavior boss, String clip, float elapsed) {
        return boss.getCurrentAction() == ZombossAction.CHARGE
                && EgyptZombossAnim.WALK_BACKWARDS_CLIP.equals(clip)
                && boss instanceof EgyptZombossBehavior egypt
                && !egypt.isChargingForward()
                && Math.abs(elapsed - EgyptZombossBehavior.WALK_SECONDS) < 1f / 30f;
    }

    private float stunPhase(String clip, float elapsed, float total) {
        float[] dur = LawnZombossClock.stunDurations(r, EgyptZombossAnim.DEFINITION_NAME,
                EgyptZombossAnim.STUN_START_CLIP, EgyptZombossAnim.STUN_END_CLIP,
                0.4667f, 0.1f);
        return LawnZombossClock.stunClipPhase(clip, EgyptZombossAnim.STUN_START_CLIP,
                EgyptZombossAnim.STUN_END_CLIP, elapsed, total, dur[0], dur[1]);
    }

    private static float actionPhase(ZombossBehavior boss, String clip, float elapsed, ClipRef ref) {
        if (boss.getPhase() != ZombossPhase.ACTION) {
            return LawnEntityDrawConstants.NO_PHASE;
        }
        ZombossAction action = boss.getCurrentAction();
        if (action == ZombossAction.MISSILE) {
            return missilePhase(clip, elapsed);
        }
        if (action == ZombossAction.CHARGE) {
            return chargePhase(clip, elapsed);
        }
        if (action == ZombossAction.CHANGE_LANE
                && (EgyptZombossAnim.WALK_DOWN_CLIP.equals(clip)
                || EgyptZombossAnim.WALK_UP_CLIP.equals(clip))) {
            return LawnZombossClock.clip01(elapsed, Math.max(0.05f, ref.duration));
        }
        if (action == ZombossAction.SUMMON) {
            return summonPhase(clip, elapsed);
        }
        return LawnEntityDrawConstants.NO_PHASE;
    }

    private static float missilePhase(String clip, float elapsed) {
        if (EgyptZombossAnim.MISSILE_START_CLIP.equals(clip)) {
            return LawnZombossClock.clip01(elapsed, EgyptZombossBehavior.MISSILE_START_SECONDS);
        }
        if (EgyptZombossAnim.ROCKET_LAUNCH_CLIP.equals(clip)) {
            return LawnZombossClock.clip01(elapsed - EgyptZombossBehavior.MISSILE_START_SECONDS,
                    EgyptZombossBehavior.ROCKET_LAUNCH_SECONDS);
        }
        return LawnEntityDrawConstants.NO_PHASE;
    }

    private static float chargePhase(String clip, float elapsed) {
        if (EgyptZombossAnim.WALK_FORWARD_CLIP.equals(clip)) {
            return LawnZombossClock.clip01(elapsed, EgyptZombossBehavior.WALK_SECONDS);
        }
        if (EgyptZombossAnim.WALK_BACKWARDS_CLIP.equals(clip)) {
            return LawnZombossClock.clip01(elapsed - EgyptZombossBehavior.WALK_SECONDS,
                    EgyptZombossBehavior.WALK_SECONDS);
        }
        return LawnEntityDrawConstants.NO_PHASE;
    }

    private static float summonPhase(String clip, float elapsed) {
        if (EgyptZombossAnim.PORTAL_START_CLIP.equals(clip)) {
            return LawnZombossClock.clip01(elapsed, EgyptZombossBehavior.PORTAL_START_SECONDS);
        }
        if (EgyptZombossAnim.PORTAL_END_CLIP.equals(clip)) {
            return LawnZombossClock.clip01(elapsed - EgyptZombossBehavior.PORTAL_START_SECONDS,
                    EgyptZombossBehavior.PORTAL_END_SECONDS);
        }
        return LawnEntityDrawConstants.NO_PHASE;
    }
}
