package view.gui.anim.zombie;

import model.item.pushable.Pushable;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Barrel Roller: barrel is baked into {@code walk}/{@code eat}/{@code die}. After the
 * barrel breaks, {@code walk2}/{@code eat2}/{@code die2}. Die-with-barrel keeps only
 * the barrel PAM parts via {@link #BARREL_PARTS}.
 */
public final class BarrelRollerAnim {
    public static final String DEFINITION_NAME = "ZombieBarrelRoller";
    public static final String BARREL_PAM = "ZOMBIE_PIRATE_BARREL_PUSHER_BARREL";

    public static final String[] BARREL_PARTS = {
            "barrel_side", "barrel_slat_01", "barrel_slat_02", "barrel_side_strap",
            "barrel_front_shad", "barrel_eyes", "barrel_front"
    };

    private BarrelRollerAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, BarrelRollerAnim::resolve);
    }

    public static boolean hasBarrel(ZombieInstance zombie) {
        Pushable pushable = zombie.getPushableItem();
        return pushable != null && !pushable.isDestroyed();
    }

    public static boolean isUnarmedClip(String clip) {
        return clip != null && clip.endsWith("2");
    }

    public static boolean isPusherPam(String pamPath) {
        if (pamPath == null) {
            return false;
        }
        String upper = pamPath.toUpperCase();
        return upper.contains("ZOMBIE_PIRATE_BARREL_PUSHER")
                && !upper.contains("BARREL_PUSHER_BARREL");
    }

    public static boolean isBarrelPropPam(String pamPath) {
        return pamPath != null && pamPath.toUpperCase().contains("BARREL_PUSHER_BARREL");
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || hasBarrel(zombie)) {
            return null;
        }
        return switch (role) {
            case DIE -> AnimPose.once(entry.path(), "die2", role, null);
            case EATING -> AnimPose.looping(entry.path(), "eat2", role);
            case WALK, IDLE -> AnimPose.looping(entry.path(),
                    role == ZombieAnimRole.IDLE ? "idle2" : "walk2", role);
        };
    }
}
