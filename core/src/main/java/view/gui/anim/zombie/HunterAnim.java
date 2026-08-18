package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.ShootBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Ice Age Hunter: default walk / eat / die. One-shot {@code throw} while
 * {@link ShootBehavior#isThrowing()}.
 */
public final class HunterAnim {
    public static final String DEFINITION_NAME = "ZombieIceAgeHunter";
    public static final String THROW_CLIP = "throw";
    /** EFFECTS PAM played on the plant at snowball impact. Clip is {@code animation}. */
    public static final String SPLAT_PAM = "ZOMBIE_HUNTER_SNOWBALL_SPLAT";
    public static final String[] DEATH_PARTICLE_PARTS = {"particle_head", "particle_hand"};

    private HunterAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, HunterAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        ShootBehavior shoot = (ShootBehavior) zombie.getBehavior(ZombieBehaviorType.SHOOT);
        if (shoot == null || !shoot.isIceAgeHunter(zombie) || !shoot.isThrowing()) {
            return null;
        }
        return AnimPose.once(entry.path(), THROW_CLIP, ZombieAnimRole.EATING, null);
    }
}
