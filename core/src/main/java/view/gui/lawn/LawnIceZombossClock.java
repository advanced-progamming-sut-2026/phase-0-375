package view.gui.lawn;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.zomboss.IceZombossBehavior;
import model.zombie.behavior.zomboss.ZombossAction;
import model.zombie.behavior.zomboss.ZombossBehavior;
import model.zombie.behavior.zomboss.ZombossPhase;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import view.gui.anim.AnimPose;
import view.gui.anim.zombie.IceZombossAnim;

final class LawnIceZombossClock {
    private final LawnEntityRenderer r;

    LawnIceZombossClock(LawnEntityRenderer r) {
        this.r = r;
    }

    void restart(ZombieInstance zombie, AnimPose pose) {
        if (pose == null || !IceZombossAnim.isIceZomboss(zombie)) {
            return;
        }
        String clip = pose.clipName();
        if (clip == null || !isActionClip(clip)) {
            return;
        }
        ZombossBehavior boss = (ZombossBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (boss == null) {
            return;
        }
        boolean phaseJustStarted = LawnZombossClock.phaseJustStarted(boss);
        boolean match = switch (boss.getPhase()) {
            case INTRO -> IceZombossAnim.INTRO_CLIP.equals(clip) && phaseJustStarted;
            case ACTION -> phaseJustStarted && isActionClip(clip)
                    && !IceZombossAnim.INTRO_CLIP.equals(clip);
            default -> false;
        };
        if (!match) {
            return;
        }
        LawnZombossClock.rewind(r, zombie);
    }

    float clipPhase(ZombieInstance zombie, AnimPose pose, ClipRef ref) {
        if (pose == null || ref == null || ref.duration <= 0f
                || !IceZombossAnim.isIceZomboss(zombie)) {
            return LawnEntityDrawConstants.NO_PHASE;
        }
        ZombossBehavior boss = (ZombossBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOSS);
        if (boss == null) {
            return LawnEntityDrawConstants.NO_PHASE;
        }
        String clip = pose.clipName();
        float total = boss.currentPhaseDurationSeconds();
        float elapsed = boss.phaseProgress01() * total;
        if (boss.getPhase() == ZombossPhase.INTRO && IceZombossAnim.INTRO_CLIP.equals(clip)) {
            return LawnZombossClock.clip01(elapsed, ref.duration);
        }
        if (boss.getPhase() != ZombossPhase.ACTION) {
            return LawnEntityDrawConstants.NO_PHASE;
        }
        ZombossAction action = boss.getCurrentAction();
        if (action == ZombossAction.ICE_MISSILE && IceZombossAnim.SLINGSHOT_CLIP.equals(clip)) {
            return LawnZombossClock.clip01(elapsed, IceZombossBehavior.SLINGSHOT_SECONDS);
        }
        if (action == ZombossAction.ICE_WIND && clip != null
                && clip.startsWith(IceZombossAnim.WIND_CLIP_PREFIX)) {
            return LawnZombossClock.clip01(elapsed, Math.max(0.05f, ref.duration));
        }
        if (action == ZombossAction.FREEZE_COLUMN && clip != null
                && clip.startsWith(IceZombossAnim.GLACIER_CLIP_PREFIX)) {
            return LawnZombossClock.clip01(elapsed, Math.max(0.05f, ref.duration));
        }
        return LawnEntityDrawConstants.NO_PHASE;
    }

    private static boolean isActionClip(String clip) {
        return IceZombossAnim.INTRO_CLIP.equals(clip)
                || IceZombossAnim.SLINGSHOT_CLIP.equals(clip)
                || clip.startsWith(IceZombossAnim.WIND_CLIP_PREFIX)
                || clip.startsWith(IceZombossAnim.GLACIER_CLIP_PREFIX);
    }
}
