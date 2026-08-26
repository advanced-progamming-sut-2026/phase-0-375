package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.zombotany.ZombotanyJalapenoBehavior;
import model.zombie.behavior.zombotany.ZombotanyPeashooterBehavior;
import model.zombie.behavior.zombotany.ZombotanySquashBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

public final class ZombotanyAnim {
    public static final String PEASHOOTER = "ZombotanyPeashooter";
    public static final String WALLNUT = "ZombotanyWallnut";
    public static final String JALAPENO = "ZombotanyJalapeno";
    public static final String SQUASH = "ZombotanySquash";

    private static final String[] WALLNUT_BODY = {"idle", "damage", "damage2", "damage3"};

    private ZombotanyAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(PEASHOOTER, ZombotanyAnim::resolve);
        overrides.register(WALLNUT, ZombotanyAnim::resolve);
        overrides.register(JALAPENO, ZombotanyAnim::resolve);
        overrides.register(SQUASH, ZombotanyAnim::resolve);
    }

    public static boolean isPlantHead(ZombieInstance zombie) {
        return zombie != null && zombie.getDefinition() != null
                && isPlantHeadName(zombie.getDefinition().getName());
    }

    public static boolean isPlantHeadName(String definitionName) {
        return definitionName != null && definitionName.startsWith("Zombotany");
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || zombie == null) {
            return null;
        }
        AnimPose pose = poseFor(zombie, entry, role);
        return pose == null ? null : pose.flipped();
    }

    private static AnimPose poseFor(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        ZombotanyPeashooterBehavior pea =
                (ZombotanyPeashooterBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_PEASHOOTER);
        if (pea != null && pea.isAttacking()) {
            return AnimPose.once(entry.path(), "attack", ZombieAnimRole.EATING, null);
        }
        ZombotanyJalapenoBehavior jala =
                (ZombotanyJalapenoBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_JALAPENO);
        if (jala != null && jala.isAttacking()) {
            return AnimPose.once(entry.path(), "attack", ZombieAnimRole.EATING, null);
        }
        ZombotanySquashBehavior squash =
                (ZombotanySquashBehavior) zombie.getBehavior(ZombieBehaviorType.ZOMBOTANY_SQUASH);
        if (squash != null && squash.isSquashing()) {
            return squashPose(squash, entry);
        }
        if (role == ZombieAnimRole.DIE) {
            return idlePose(zombie, entry, role, false);
        }
        return idlePose(zombie, entry, role, true);
    }

    private static AnimPose squashPose(ZombotanySquashBehavior squash, PamCatalog.PamEntry entry) {
        float up = PamCatalog.clipDurationSeconds(entry, "jump_up_right");
        String clip = (up <= 0f || squash.getAttackElapsed() < up)
                ? "jump_up_right"
                : "jump_down_right";
        return AnimPose.once(entry.path(), clip, ZombieAnimRole.EATING, null);
    }

    private static AnimPose idlePose(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                     ZombieAnimRole role, boolean loop) {
        String clip = idleClip(zombie);
        return loop
                ? AnimPose.looping(entry.path(), clip, role)
                : AnimPose.once(entry.path(), clip, role);
    }

    private static String idleClip(ZombieInstance zombie) {
        if (WALLNUT.equals(zombie.getDefinition().getName())) {
            return WALLNUT_BODY[healthStage(zombie, WALLNUT_BODY.length)];
        }
        return "idle";
    }

    static int healthStage(ZombieInstance zombie, int stages) {
        if (zombie == null || stages <= 1) {
            return 0;
        }
        int max = zombie.getDefinition() != null ? zombie.getDefinition().getBaseHP() : 0;
        float frac = max <= 0 ? 0f : zombie.getCurrentHP() / (float) max;
        frac = Math.max(0f, Math.min(1f, frac));
        if (frac >= 0.999f) {
            return 0;
        }
        int stage = (int) ((1f - frac) * stages);
        return Math.min(stages - 1, Math.max(0, stage));
    }
}
