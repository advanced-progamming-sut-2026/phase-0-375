package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.JumpBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

import java.util.HashMap;
import java.util.Map;

/**
 * Prospector: burning dynamite layers, then {@code blastoff} → {@code fly} (0.4s)
 * → reversed {@code land}. After landing, walk / eat / die are mirrored.
 */
public final class ProspectorAnim {
    private ProspectorAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register("ZombieProspector", ProspectorAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null) {
            return null;
        }
        JumpBehavior jump = (JumpBehavior) zombie.getBehavior(ZombieBehaviorType.JUMP);
        if (jump == null) {
            return null;
        }
        Map<String, Boolean> vis = dynamiteVis(jump);
        if (jump.getPhase() == JumpBehavior.JumpPhase.JUMPING) {
            return jumpingPose(entry, jump, vis);
        }
        if (role == ZombieAnimRole.DIE) {
            return null;
        }
        String clip = role == ZombieAnimRole.EATING ? "eat"
                : role == ZombieAnimRole.IDLE ? "idle" : "walk";
        AnimPose pose = AnimPose.looping(entry.path(), clip, role, vis);
        return facingRight(jump) ? pose.flipped() : pose;
    }

    private static AnimPose jumpingPose(PamCatalog.PamEntry entry, JumpBehavior jump,
                                        Map<String, Boolean> vis) {
        float t = jump.getTravelTimer();
        if (t < JumpBehavior.BLASTOFF_DURATION) {
            return AnimPose.once(entry.path(), "blastoff", ZombieAnimRole.EATING, vis);
        }
        if (t < JumpBehavior.BLASTOFF_DURATION + JumpBehavior.FLY_DURATION) {
            return AnimPose.once(entry.path(), "fly", ZombieAnimRole.EATING, vis);
        }
        return AnimPose.once(entry.path(), "land", ZombieAnimRole.EATING, vis)
                .reversed()
                .flipped();
    }

    private static boolean facingRight(JumpBehavior jump) {
        return jump.hasLaunched() || jump.getPhase() == JumpBehavior.JumpPhase.REVERSED_WALK;
    }

    private static Map<String, Boolean> dynamiteVis(JumpBehavior jump) {
        Map<String, Boolean> vis = new HashMap<>();
        String on = jump.dynamitePart();
        for (String part : JumpBehavior.DYNAMITE_PARTS) {
            vis.put(part, part.equals(on));
        }
        return vis;
    }
}
