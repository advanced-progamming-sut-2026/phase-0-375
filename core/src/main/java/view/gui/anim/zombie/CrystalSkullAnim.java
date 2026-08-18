package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.StealSunBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Crystal Skull (Turquoise): {@code power_up} → looping {@code power} → {@code power_down}
 * → {@code attack}. Walk / eat / die fall through to the global defaults.
 */
public final class CrystalSkullAnim {
    private CrystalSkullAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register("ZombieCrystalSkull", CrystalSkullAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        StealSunBehavior steal = (StealSunBehavior) zombie.getBehavior(ZombieBehaviorType.STEAL_SUN);
        if (steal == null) {
            return null;
        }
        return switch (steal.getTurquoisePhase()) {
            case POWER_UP -> AnimPose.once(entry.path(), "power_up", ZombieAnimRole.EATING, null);
            case POWER -> AnimPose.looping(entry.path(), "power", ZombieAnimRole.EATING);
            case POWER_DOWN -> AnimPose.once(entry.path(), "power_down", ZombieAnimRole.EATING, null);
            case ATTACK -> AnimPose.once(entry.path(), "attack", ZombieAnimRole.EATING, null);
            case WALKING -> null;
        };
    }
}
