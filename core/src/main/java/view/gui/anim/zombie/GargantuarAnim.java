package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.SmashBehavior;
import model.zombie.behavior.ThrowImpBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

import java.util.HashMap;
import java.util.Map;

/**
 * Gargantuar smash and throw clips. Both cycles are one-shot; walking resumes
 * when the last clip ends.
 */
public final class GargantuarAnim {
    /**
     * Carried Imp parts on the Gargantuar PAM. Same list as
     * {@code HealthThresholdToImpAmmoLayers.ProjectileLayersToHide}.
     */
    private static final String[] CARRIED_IMP_PARTS = {
            "zombie_imp_skull", "zombie_imp_jaw", "_zombie_imp_head_top",
            "Zombie_gargantuar_whiterope", "Zombie_gargantuar_rope",
            "zombie_imp_arm_inner_lower", "zombie_imp_arm_inner_upper",
            "zombie_imp_arm_outer_lower", "zombie_imp_arm_outer_upper_01",
            "zombie_imp_arm_outer_upper_02", "zombie_imp_arms_outer_upper",
            "zombie_imp_eye", "zombie_imp_eye_sm",
            "zombie_imp_hand_inner", "zombie_imp_hand_outer",
            "zombie_imp_leg_inner_lower", "zombie_imp_leg_inner_upper",
            "zombie_imp_leg_outer_lower", "zombie_imp_leg_outer_upper",
            "zombie_imp_pupil", "zombie_imp_torso", "zombie_imp_waist",
            "zombie_imp_toe_outer", "zombie_imp_toe_inner"
    };

    private GargantuarAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register("ZombieGargantuar", GargantuarAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null) {
            return null;
        }
        ThrowImpBehavior toss = (ThrowImpBehavior) zombie.getBehavior(ZombieBehaviorType.THROW_IMP);
        boolean hideImp = toss != null && toss.hasReleasedImp();
        Map<String, Boolean> vis = hideCarriedImp(
                ZombieAnimAdapter.armorVisibility(zombie, entry), hideImp);

        if (toss != null && toss.isThrowing()) {
            String clip = toss.getThrowPhase() == ThrowImpBehavior.ThrowPhase.FIRE ? "fire" : "cannon_fire";
            return AnimPose.once(entry.path(), clip, ZombieAnimRole.EATING, vis);
        }
        if (role == ZombieAnimRole.DIE) {
            return hideImp ? AnimPose.once(entry.path(), "die", role, vis) : null;
        }
        SmashBehavior smash = (SmashBehavior) zombie.getBehavior(ZombieBehaviorType.SMASH);
        if (smash != null) {
            String clip = switch (smash.getGargantuarPhase()) {
                case WINDUP -> "eat";
                case SMASHING -> "smash_left";
                case WALKING -> null;
            };
            if (clip != null) {
                return AnimPose.once(entry.path(), clip, ZombieAnimRole.EATING, vis);
            }
        }
        if (!hideImp) {
            return null;
        }
        String walk = role == ZombieAnimRole.EATING ? "eat" : "walk";
        return AnimPose.looping(entry.path(), walk, role, vis);
    }

    /** Walk-clip times (seconds) when a foot hits the ground. */
    public static final float WALK_STOMP_A = 0.73f;
    public static final float WALK_STOMP_B = 1.9f;

    /**
     * True when walk-clip time crossed a foot-stomp between the previous sample and
     * {@code currTime}. First sample ({@code prevTime < 0}) never fires. Times wrap
     * on {@code clipDuration} so gait cycles and unbounded wall-clock both work.
     */
    public static boolean crossedWalkStomp(float prevTime, float currTime, float clipDuration) {
        if (prevTime < 0f) {
            return false;
        }
        if (clipDuration > 0f) {
            prevTime = cycleTime(prevTime, clipDuration);
            currTime = cycleTime(currTime, clipDuration);
        }
        if (currTime >= prevTime) {
            return stompIn(prevTime, currTime);
        }
        return stompIn(prevTime, Float.POSITIVE_INFINITY) || stompIn(-1f, currTime);
    }

    private static boolean stompIn(float prev, float curr) {
        return (prev < WALK_STOMP_A && curr >= WALK_STOMP_A)
                || (prev < WALK_STOMP_B && curr >= WALK_STOMP_B);
    }

    private static float cycleTime(float t, float duration) {
        float m = t % duration;
        return m < 0f ? m + duration : m;
    }

    static Map<String, Boolean> hideCarriedImp(Map<String, Boolean> base, boolean hide) {
        if (!hide) {
            return base;
        }
        Map<String, Boolean> vis = base == null ? new HashMap<>() : new HashMap<>(base);
        for (String part : CARRIED_IMP_PARTS) {
            vis.put(part, Boolean.FALSE);
        }
        return vis;
    }
}
