package view.gui.anim.plant.exclusive;

import model.plant.instance.AbilityState;
import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

public final class CaulipowerAnim {
    private static final int CHARGE_STAGES = 4;

    private CaulipowerAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Caulipower", CaulipowerAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role != PlantAnimRole.IDLE) {
            return null;
        }

        AbilityState state = plant.getAbilityState(plant.getDefinition().getAbilityType());
        float interval = plant.getDefinition().getActionInterval();
        int stage = CHARGE_STAGES;
        if (state != null && interval > 0f) {
            float elapsed = interval - state.getCooldownRemaining();
            float quarter = interval / CHARGE_STAGES;
            stage = 1 + (int) Math.floor(Math.max(0f, elapsed) / quarter);
            stage = Math.min(CHARGE_STAGES, Math.max(1, stage));
        }
        return AnimPose.looping(entry.path(), "idle" + stage + "_1", role);
    }
}
