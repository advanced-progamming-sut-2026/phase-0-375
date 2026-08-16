package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.FlyBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Dodo Rider: default walk / eat / die. {@code fly_start} → looping {@code fly_loop}
 * → {@code fly_end} while {@link FlyBehavior#isFlying()}.
 */
public final class DodoAnim {
    public static final String DEFINITION_NAME = "ZombieIceAgeDodo";

    private DodoAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, DodoAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        FlyBehavior fly = (FlyBehavior) zombie.getBehavior(ZombieBehaviorType.FLY);
        if (fly == null || !fly.isFlying()) {
            return null;
        }
        return switch (fly.getPhase()) {
            case TAKEOFF -> AnimPose.once(entry.path(), "fly_start", ZombieAnimRole.EATING, null);
            case LANDING -> AnimPose.once(entry.path(), "fly_end", ZombieAnimRole.EATING, null);
            case FLYING -> AnimPose.looping(entry.path(), "fly_loop", ZombieAnimRole.EATING);
            default -> null;
        };
    }

}
