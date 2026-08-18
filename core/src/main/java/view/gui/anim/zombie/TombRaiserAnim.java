package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.SummonBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Tomb Raiser: default walk / eat / die. One-shot {@code power} while
 * {@link SummonBehavior#isRaising()}.
 */
public final class TombRaiserAnim {
    public static final String DEFINITION_NAME = "ZombieTombRaiser";

    private TombRaiserAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, TombRaiserAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        SummonBehavior summon = (SummonBehavior) zombie.getBehavior(ZombieBehaviorType.SUMMON);
        if (summon == null || !summon.isRaising()) {
            return null;
        }
        return AnimPose.once(entry.path(), "power", ZombieAnimRole.EATING, null);
    }
}
