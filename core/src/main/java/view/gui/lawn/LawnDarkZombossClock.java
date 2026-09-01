package view.gui.lawn;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.zomboss.DarkZombossBehavior;
import model.zombie.behavior.zomboss.ZombossAction;
import model.zombie.behavior.zomboss.ZombossBehavior;
import model.zombie.behavior.zomboss.ZombossPhase;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import view.gui.anim.AnimPose;
import view.gui.anim.zombie.DarkZombossAnim;

final class LawnDarkZombossClock {
    private final LawnEntityRenderer r;

    LawnDarkZombossClock(LawnEntityRenderer r) {
        this.r = r;
    }

    void restart(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !DarkZombossAnim.isDarkZomboss(zombie)) {
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
                || !DarkZombossAnim.isDarkZomboss(zombie)) {
            return LawnEntityDrawConstants.NO_PHASE;
        }
        ZombossBehavior boss = (ZombossBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (boss == null) {
            return LawnEntityDrawConstants.NO_PHASE;
        }
        String clip = pose.clipName();
        float total = boss.currentPhaseDurationSeconds();
        float elapsed = boss.phaseProgress01() * total;
        if (boss.getPhase() == ZombossPhase.INTRO && DarkZombossAnim.INTRO_CLIP.equals(clip)) {
            return LawnZombossClock.clip01(elapsed, ref.duration);
        }
        if (boss.getPhase() == ZombossPhase.STUNNED) {
            return stunPhase(clip, elapsed, total);
        }
        return burnPhase(boss, clip, elapsed);
    }

    private static boolean isRestartClip(String clip) {
        return DarkZombossAnim.INTRO_CLIP.equals(clip)
                || DarkZombossAnim.STUN_START_CLIP.equals(clip)
                || DarkZombossAnim.STUN_END_CLIP.equals(clip)
                || "fire_attack".equals(clip)
                || "fire_attack_end".equals(clip)
                || "fire_bomb".equals(clip)
                || "summoning".equals(clip);
    }

    private boolean shouldRestart(ZombossBehavior boss, String clip) {
        boolean phaseJustStarted = LawnZombossClock.phaseJustStarted(boss);
        return switch (boss.getPhase()) {
            case INTRO -> DarkZombossAnim.INTRO_CLIP.equals(clip) && phaseJustStarted;
            case STUNNED -> stunRestart(boss, clip);
            case ACTION -> phaseJustStarted && (
                    "fire_attack".equals(clip)
                            || "fire_bomb".equals(clip)
                            || "summoning".equals(clip));
            default -> false;
        };
    }

    private boolean stunRestart(ZombossBehavior boss, String clip) {
        float total = boss.currentPhaseDurationSeconds();
        float elapsed = boss.phaseProgress01() * total;
        float[] dur = LawnZombossClock.stunDurations(r, DarkZombossAnim.DEFINITION_NAME,
                DarkZombossAnim.STUN_START_CLIP, DarkZombossAnim.STUN_END_CLIP,
                0.4333f, 0.4667f);
        return LawnZombossClock.stunStartJustStarted(clip, DarkZombossAnim.STUN_START_CLIP, elapsed)
                || LawnZombossClock.stunEndJustStarted(clip, DarkZombossAnim.STUN_END_CLIP,
                elapsed, total, dur[0], dur[1]);
    }

    private float stunPhase(String clip, float elapsed, float total) {
        float[] dur = LawnZombossClock.stunDurations(r, DarkZombossAnim.DEFINITION_NAME,
                DarkZombossAnim.STUN_START_CLIP, DarkZombossAnim.STUN_END_CLIP,
                0.4333f, 0.4667f);
        return LawnZombossClock.stunClipPhase(clip, DarkZombossAnim.STUN_START_CLIP,
                DarkZombossAnim.STUN_END_CLIP, elapsed, total, dur[0], dur[1]);
    }

    private static float burnPhase(ZombossBehavior boss, String clip, float elapsed) {
        if (boss.getPhase() != ZombossPhase.ACTION
                || boss.getCurrentAction() != ZombossAction.BURN_ROWS) {
            return LawnEntityDrawConstants.NO_PHASE;
        }
        if ("fire_attack".equals(clip)) {
            return LawnZombossClock.clip01(elapsed, DarkZombossBehavior.FIRE_ATTACK_START_SECONDS);
        }
        if ("fire_attack_end".equals(clip)) {
            float endAt = DarkZombossBehavior.burnRowsDurationSeconds()
                    - DarkZombossBehavior.FIRE_ATTACK_END_SECONDS;
            return LawnZombossClock.clip01(elapsed - endAt, DarkZombossBehavior.FIRE_ATTACK_END_SECONDS);
        }
        return LawnEntityDrawConstants.NO_PHASE;
    }
}
