package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.JuggleBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Dark Ages Jester: default walk / eat / die. {@code spinup} → looping {@code spin}
 * while walking → {@code spindown}, keyed off {@link JuggleBehavior#getPhase()}.
 */
public final class JugglerAnim {
    public static final String DEFINITION_NAME = "ZombieDarkJuggler";
    public static final String SPINUP_CLIP = "spinup";
    public static final String SPIN_CLIP = "spin";
    public static final String SPINDOWN_CLIP = "spindown";

    private JugglerAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, JugglerAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        JuggleBehavior juggle = (JuggleBehavior) zombie.getBehavior(ZombieBehaviorType.JUGGLE);
        if (juggle == null) {
            return null;
        }
        return switch (juggle.getPhase()) {
            case SPINUP -> AnimPose.once(entry.path(), SPINUP_CLIP, ZombieAnimRole.EATING, null);
            case SPIN -> AnimPose.looping(entry.path(), SPIN_CLIP, ZombieAnimRole.EATING);
            case SPINDOWN -> AnimPose.once(entry.path(), SPINDOWN_CLIP, ZombieAnimRole.EATING, null);
            case IDLE -> null;
        };
    }
}
