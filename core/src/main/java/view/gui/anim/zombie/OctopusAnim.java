package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.ShootBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

import java.util.HashMap;
import java.util.Map;

/**
 * Beach Octopus: default walk / eat / die. One-shot {@code toss} while
 * {@link ShootBehavior#isOctopusThrowing()}. Held octopus hides after release.
 */
public final class OctopusAnim {
    public static final String DEFINITION_NAME = "ZombieBeachOctopus";
    public static final String TOSS_CLIP = "toss";
    public static final String PROJECTILE_PAM = "ZOMBIE_OCTOPUS_PROJECTILE";
    public static final String FLY_CLIP = "animation";
    public static final String IMPACT_CLIP = "animation2";
    public static final String LOOP_CLIP = "animation3";
    public static final String DIE_CLIP = "die";
    public static final String HELD_PART = "zombie_beach_octopus_66x76";

    private OctopusAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, OctopusAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null) {
            return null;
        }
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        if (shoot == null || !shoot.isBeachOctopus(zombie)) {
            return null;
        }
        Map<String, Boolean> vis = hideHeld(
                ZombieAnimAdapter.armorVisibility(zombie, entry), shoot.hasReleasedOctopus());
        if (role == ZombieAnimRole.DIE) {
            return vis == null ? null : AnimPose.once(entry.path(), "die", role, vis);
        }
        if (!shoot.isOctopusThrowing()) {
            return vis == null ? null : AnimPose.looping(
                    entry.path(), role == ZombieAnimRole.EATING ? "eat" : "walk", role, vis);
        }
        return AnimPose.once(entry.path(), TOSS_CLIP, ZombieAnimRole.EATING, vis);
    }

    static Map<String, Boolean> hideHeld(Map<String, Boolean> base, boolean hide) {
        if (!hide) {
            return base;
        }
        Map<String, Boolean> vis = base == null ? new HashMap<>() : new HashMap<>(base);
        vis.put(HELD_PART, Boolean.FALSE);
        return vis;
    }
}
