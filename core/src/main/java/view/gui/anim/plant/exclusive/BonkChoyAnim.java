package view.gui.anim.plant.exclusive;

import model.plant.ability.MeleeAbility;
import model.plant.ability.PlantAbility;
import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.anim.plant.PlantAnimOverrides;
import view.gui.anim.plant.PlantAnimRole;
import view.gui.assets.PamCatalog;

public final class BonkChoyAnim {
    private BonkChoyAnim() {}

    public static void register(PlantAnimOverrides overrides) {
        overrides.register("Bonk Choy", BonkChoyAnim::resolve);
    }

    private static AnimPose resolve(PlantInstance plant, PamCatalog.PamEntry entry, PlantAnimRole role) {
        if (plant.getDefinition() == null || entry == null || role != PlantAnimRole.ATTACK) {
            return null;
        }

        PlantAbility ability = plant.getAbilityStrategy();
        if (!(ability instanceof MeleeAbility melee)) return null;

        return AnimPose.once(entry.path(), DirectionalMeleeClips.clipName(plant, melee.getTargets()), role);
    }
}
