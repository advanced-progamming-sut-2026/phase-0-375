package view.gui.anim.plant.exclusive;

import model.plant.ability.LobberAbility;
import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

public final class KernelPultAnim {
    private KernelPultAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Kernel-pult", KernelPultAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role != PlantAnimRole.ATTACK) {
            return null;
        }
        if (!(plant.getAbilityStrategy() instanceof LobberAbility lobber) || !lobber.isButterShot()) {
            return null;
        }
        return AnimPose.once(entry.path(), "attack2", role);
    }
}
