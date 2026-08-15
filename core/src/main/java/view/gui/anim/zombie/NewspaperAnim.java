package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.EnrageBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Newspaper: {@code walk_newspaper} / {@code eat_newspaper} while the paper lives,
 * then one-shot {@code newspaper_defeat}. Walk / eat / die after that fall through.
 */
public final class NewspaperAnim {
    private NewspaperAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register("ZombieNewspaper", NewspaperAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        EnrageBehavior enrage = (EnrageBehavior) zombie.getBehavior(ZombieBehaviorType.ENRAGE);
        if (enrage == null) {
            return null;
        }
        if (enrage.isDefeating()) {
            return AnimPose.once(entry.path(), "newspaper_defeat", ZombieAnimRole.EATING, null);
        }
        if (enrage.isEnraged()) {
            return null;
        }
        return switch (role) {
            case EATING -> AnimPose.looping(entry.path(), "eat_newspaper", role,
                    ZombieAnimAdapter.armorVisibility(zombie, entry));
            case WALK -> AnimPose.looping(entry.path(), "walk_newspaper", role,
                    ZombieAnimAdapter.armorVisibility(zombie, entry));
            default -> null;
        };
    }
}
