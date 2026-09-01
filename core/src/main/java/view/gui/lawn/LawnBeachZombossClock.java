package view.gui.lawn;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.zomboss.BeachZombossBehavior;
import model.zombie.behavior.zomboss.ZombossAction;
import model.zombie.behavior.zomboss.ZombossBehavior;
import model.zombie.behavior.zomboss.ZombossPhase;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import view.gui.anim.AnimPose;
import view.gui.anim.zombie.BeachZombossAnim;

final class LawnBeachZombossClock {
    private final LawnEntityRenderer r;

    LawnBeachZombossClock(LawnEntityRenderer r) {
        this.r = r;
    }

    void restart(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !BeachZombossAnim.isBeachZomboss(zombie)) {
            return;
        }
        String clip = pose.clipName();
        if (clip == null || !isRestartClip(clip)) {
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
                || !BeachZombossAnim.isBeachZomboss(zombie)) {
            return LawnEntityDrawConstants.NO_PHASE;
        }
        ZombossBehavior boss = (ZombossBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (boss == null) {
            return LawnEntityDrawConstants.NO_PHASE;
        }
        String clip = pose.clipName();
        float total = boss.currentPhaseDurationSeconds();
        float elapsed = boss.phaseProgress01() * total;
        if (boss.getPhase() == ZombossPhase.INTRO && BeachZombossAnim.INTRO_CLIP.equals(clip)) {
            return LawnZombossClock.clip01(elapsed, ref.duration);
        }
        if (boss.getPhase() == ZombossPhase.STUNNED) {
            return stunPhase(clip, elapsed, total);
        }
        if (boss.getPhase() == ZombossPhase.ACTION && boss instanceof BeachZombossBehavior beach) {
            return actionPhase(beach, clip, elapsed);
        }
        return LawnEntityDrawConstants.NO_PHASE;
    }

    private static boolean isRestartClip(String clip) {
        return BeachZombossAnim.INTRO_CLIP.equals(clip)
                || BeachZombossAnim.STUN_START_CLIP.equals(clip)
                || BeachZombossAnim.STUN_END_CLIP.equals(clip)
                || BeachZombossAnim.SPAWN_CLIP.equals(clip)
                || BeachZombossAnim.SUBMERGE_CLIP.equals(clip)
                || BeachZombossAnim.EMERGE_CLIP.equals(clip)
                || BeachZombossAnim.SUCTION_ON_CLIP.equals(clip)
                || BeachZombossAnim.SUCTION_OFF_CLIP.equals(clip);
    }

    private boolean shouldRestart(ZombossBehavior boss, String clip) {
        boolean phaseJustStarted = LawnZombossClock.phaseJustStarted(boss);
        return switch (boss.getPhase()) {
            case INTRO -> BeachZombossAnim.INTRO_CLIP.equals(clip) && phaseJustStarted;
            case STUNNED -> stunRestart(boss, clip);
            case ACTION -> actionRestart(boss, clip, phaseJustStarted);
            default -> false;
        };
    }

    private boolean stunRestart(ZombossBehavior boss, String clip) {
        float total = boss.currentPhaseDurationSeconds();
        float elapsed = boss.phaseProgress01() * total;
        float[] dur = LawnZombossClock.stunDurations(r, BeachZombossAnim.DEFINITION_NAME,
                BeachZombossAnim.STUN_START_CLIP, BeachZombossAnim.STUN_END_CLIP,
                0.1333f, 1.1f);
        return LawnZombossClock.stunStartJustStarted(clip, BeachZombossAnim.STUN_START_CLIP, elapsed)
                || LawnZombossClock.stunEndJustStarted(clip, BeachZombossAnim.STUN_END_CLIP,
                elapsed, total, dur[0], dur[1]);
    }

    private static boolean actionRestart(ZombossBehavior boss, String clip, boolean phaseJustStarted) {
        if (phaseJustStarted && (
                BeachZombossAnim.SPAWN_CLIP.equals(clip)
                        || BeachZombossAnim.SUBMERGE_CLIP.equals(clip)
                        || BeachZombossAnim.SUCTION_ON_CLIP.equals(clip))) {
            return true;
        }
        float total = boss.currentPhaseDurationSeconds();
        float elapsed = boss.phaseProgress01() * total;
        if (boss.getCurrentAction() == ZombossAction.CHANGE_LANE
                && BeachZombossAnim.EMERGE_CLIP.equals(clip)
                && boss instanceof BeachZombossBehavior) {
            return Math.abs(elapsed - BeachZombossBehavior.SUBMERGE_SECONDS) < 1f / 30f;
        }
        if (boss.getCurrentAction() == ZombossAction.TURBINE
                && BeachZombossAnim.SUCTION_OFF_CLIP.equals(clip)
                && boss instanceof BeachZombossBehavior beach) {
            float turbineElapsed = beach.turbineElapsedSeconds();
            return Math.abs(turbineElapsed
                    - (BeachZombossBehavior.SUCTION_ON_SECONDS
                    + BeachZombossBehavior.SUCTION_LOOP_SECONDS)) < 1f / 30f;
        }
        return false;
    }

    private float stunPhase(String clip, float elapsed, float total) {
        float[] dur = LawnZombossClock.stunDurations(r, BeachZombossAnim.DEFINITION_NAME,
                BeachZombossAnim.STUN_START_CLIP, BeachZombossAnim.STUN_END_CLIP,
                0.1333f, 1.1f);
        return LawnZombossClock.stunClipPhase(clip, BeachZombossAnim.STUN_START_CLIP,
                BeachZombossAnim.STUN_END_CLIP, elapsed, total, dur[0], dur[1]);
    }

    private static float actionPhase(BeachZombossBehavior beach, String clip, float elapsed) {
        ZombossAction action = beach.getCurrentAction();
        if (action == ZombossAction.SUMMON || action == ZombossAction.BABY_SHARK) {
            if (BeachZombossAnim.SPAWN_CLIP.equals(clip)) {
                return LawnZombossClock.clip01(elapsed, BeachZombossBehavior.SPAWN_SECONDS);
            }
        }
        if (action == ZombossAction.CHANGE_LANE) {
            if (BeachZombossAnim.SUBMERGE_CLIP.equals(clip)) {
                return LawnZombossClock.clip01(elapsed, BeachZombossBehavior.SUBMERGE_SECONDS);
            }
            if (BeachZombossAnim.EMERGE_CLIP.equals(clip)) {
                return LawnZombossClock.clip01(elapsed - BeachZombossBehavior.SUBMERGE_SECONDS,
                        BeachZombossBehavior.EMERGE_SECONDS);
            }
        }
        if (action == ZombossAction.TURBINE) {
            return turbinePhase(beach, clip);
        }
        return LawnEntityDrawConstants.NO_PHASE;
    }

    private static float turbinePhase(BeachZombossBehavior beach, String clip) {
        float turbineElapsed = beach.turbineElapsedSeconds();
        if (BeachZombossAnim.SUCTION_ON_CLIP.equals(clip)) {
            return LawnZombossClock.clip01(turbineElapsed, BeachZombossBehavior.SUCTION_ON_SECONDS);
        }
        if (BeachZombossAnim.SUCTION_OFF_CLIP.equals(clip)) {
            float into = turbineElapsed - BeachZombossBehavior.SUCTION_ON_SECONDS
                    - BeachZombossBehavior.SUCTION_LOOP_SECONDS;
            return LawnZombossClock.clip01(into, BeachZombossBehavior.SUCTION_OFF_SECONDS);
        }
        return LawnEntityDrawConstants.NO_PHASE;
    }
}
