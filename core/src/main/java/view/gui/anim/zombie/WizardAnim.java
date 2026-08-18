package view.gui.anim.zombie;

import model.enums.ZombieBehaviorType;
import model.zombie.behavior.TransformBehavior;
import model.zombie.instance.ZombieInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Dark Ages Wizard: default walk / die. One-shot {@code sheep} while
 * {@link TransformBehavior#isCasting()}. Never plays {@code eat}.
 */
public final class WizardAnim {
    public static final String DEFINITION_NAME = "ZombieWizard";
    public static final String SHEEP_CLIP = "sheep";
    /** EFFECTS PAM on the converted tile. */
    public static final String SHEEPENING_PAM = "DARK_WIZARD_SHEEPENING";
    public static final String APPEAR_CLIP = "animation";
    public static final String LEAVE_CLIP = "animation2";
    public static final String IDLE2_CLIP = "idle2";
    public static final String IDLE3_CLIP = "idle3";

    private WizardAnim() {}

    public static void register(ZombieAnimOverrides overrides) {
        overrides.register(DEFINITION_NAME, WizardAnim::resolve);
    }

    private static AnimPose resolve(ZombieInstance zombie, PamCatalog.PamEntry entry,
                                    ZombieAnimRole role) {
        if (entry == null || role == ZombieAnimRole.DIE) {
            return null;
        }
        TransformBehavior transform = (TransformBehavior) zombie.getBehavior(ZombieBehaviorType.TRANSFORM);
        if (transform == null) {
            return null;
        }
        if (transform.isCasting()) {
            return AnimPose.once(entry.path(), SHEEP_CLIP, ZombieAnimRole.EATING, null);
        }
        if (role == ZombieAnimRole.EATING) {
            return AnimPose.looping(entry.path(), "walk", ZombieAnimRole.EATING);
        }
        return null;
    }
}
