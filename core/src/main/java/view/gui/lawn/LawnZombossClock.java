package view.gui.lawn;

import model.zombie.behavior.zomboss.ZombossBehavior;
import model.zombie.instance.ZombieInstance;
import pvz.libpvz.pam.ClipRef;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Zomboss clip rewind + phase mapping, split per biome boss.
 */
final class LawnZombossClock {
    private final LawnDarkZombossClock dark;
    private final LawnEgyptZombossClock egypt;
    private final LawnIceZombossClock ice;
    private final LawnBeachZombossClock beach;

    LawnZombossClock(LawnEntityRenderer r) {
        this.dark = new LawnDarkZombossClock(r);
        this.egypt = new LawnEgyptZombossClock(r);
        this.ice = new LawnIceZombossClock(r);
        this.beach = new LawnBeachZombossClock(r);
    }

    void restartDarkZombossClock(ZombieInstance zombie, AnimPose pose) {
        dark.restart(zombie, pose);
    }

    float darkZombossClipPhase(ZombieInstance zombie, AnimPose pose, ClipRef ref) {
        return dark.clipPhase(zombie, pose, ref);
    }

    void restartEgyptZombossClock(ZombieInstance zombie, AnimPose pose) {
        egypt.restart(zombie, pose);
    }

    float egyptZombossClipPhase(ZombieInstance zombie, AnimPose pose, ClipRef ref) {
        return egypt.clipPhase(zombie, pose, ref);
    }

    void restartIceZombossClock(ZombieInstance zombie, AnimPose pose) {
        ice.restart(zombie, pose);
    }

    float iceZombossClipPhase(ZombieInstance zombie, AnimPose pose, ClipRef ref) {
        return ice.clipPhase(zombie, pose, ref);
    }

    void restartBeachZombossClock(ZombieInstance zombie, AnimPose pose) {
        beach.restart(zombie, pose);
    }

    float beachZombossClipPhase(ZombieInstance zombie, AnimPose pose, ClipRef ref) {
        return beach.clipPhase(zombie, pose, ref);
    }

    static void rewind(LawnEntityRenderer r, ZombieInstance zombie) {
        AnimClock clock = r.clockFor(zombie);
        clock.clipKey = "";
        clock.time = 0f;
    }

    static boolean phaseJustStarted(ZombossBehavior boss) {
        float total = boss.currentPhaseDurationSeconds();
        return total > 0f && Math.abs(boss.getPhaseTimer() - total) < 1e-3f;
    }

    static float clip01(float elapsed, float duration) {
        if (duration <= 0f) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, elapsed / duration));
    }

    static float[] stunDurations(LawnEntityRenderer r, String definition,
                                 String startClip, String endClip,
                                 float fallbackStart, float fallbackEnd) {
        PamCatalog.PamEntry entry = r.catalog.forZombie(definition);
        float startDur = PamCatalog.clipDurationSeconds(entry, startClip);
        float endDur = PamCatalog.clipDurationSeconds(entry, endClip);
        if (startDur <= 0f) {
            startDur = fallbackStart;
        }
        if (endDur <= 0f) {
            endDur = fallbackEnd;
        }
        return new float[]{startDur, endDur};
    }

    static boolean stunStartJustStarted(String clip, String startClip, float elapsed) {
        return startClip.equals(clip) && elapsed < 1e-3f;
    }

    static boolean stunEndJustStarted(String clip, String endClip, float elapsed,
                                      float total, float startDur, float endDur) {
        float endAt = Math.max(startDur, total - endDur);
        return endClip.equals(clip)
                && elapsed >= endAt
                && elapsed < endAt + 1e-3f + 1f / 60f;
    }

    static float stunClipPhase(String clip, String startClip, String endClip,
                               float elapsed, float total, float startDur, float endDur) {
        if (startClip.equals(clip)) {
            return clip01(elapsed, startDur);
        }
        if (endClip.equals(clip)) {
            float endAt = Math.max(startDur, total - endDur);
            return clip01(elapsed - endAt, endDur);
        }
        return LawnEntityDrawConstants.NO_PHASE;
    }
}
