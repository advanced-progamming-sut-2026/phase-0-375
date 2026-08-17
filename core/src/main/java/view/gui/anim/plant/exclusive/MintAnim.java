package view.gui.anim.plant.exclusive;

import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

/**
 * Power mints use {@code intro} / {@code loop} / {@code outro} instead of idle.
 */
public final class MintAnim {
    private static final String[] DEFINITION_NAMES = {
            "Enlighten-mint",
            "Appease-mint",
            "Arma-mint",
            "Bombard-mint",
            "Enforce-mint",
            "Reinforce-mint",
            "Enchant-mint",
            "Pierce-mint",
            "catTail-mint"
    };

    private MintAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        for (String name : DEFINITION_NAMES) {
            overrides.register(name, MintAnim::resolve);
        }
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null) {
            return null;
        }
        float remaining = plant.getLifespanRemaining();
        float total = plant.getLifespanTotal();
        float outroDur = PamCatalog.clipDurationSeconds(entry, "outro");
        if (outroDur > 0f && remaining >= 0f && remaining <= outroDur) {
            return AnimPose.once(entry.path(), "outro", role != null ? role : PlantAnimRole.IDLE);
        }
        float introDur = PamCatalog.clipDurationSeconds(entry, "intro");
        float elapsed = total > 0f && remaining >= 0f ? total - remaining : 0f;
        if (introDur > 0f && elapsed < introDur) {
            return AnimPose.once(entry.path(), "intro", role != null ? role : PlantAnimRole.IDLE);
        }
        return AnimPose.looping(entry.path(), "loop", role != null ? role : PlantAnimRole.IDLE);
    }
}
