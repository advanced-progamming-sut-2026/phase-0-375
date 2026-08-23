package view.gui.assets;

import model.enums.PlantState;
import model.plant.ability.PlantClipDurations;
import model.plant.ability.TimedPlantAction;
import model.plant.instance.PlantInstance;
import view.gui.anim.plant.PlantAnimAdapter;

/**
 * {@link PlantClipDurations} using {@link PlantAnimAdapter}'s model → clip mapping.
 */
public final class PamPlantClipDurations implements PlantClipDurations {
    private final PlantAnimAdapter adapter;

    public PamPlantClipDurations(PamCatalog catalog) {
        this(catalog == null ? null : new PlantAnimAdapter(catalog));
    }

    public PamPlantClipDurations(PamCatalog catalog, PlantSpritesheetCatalog sheets) {
        this(catalog == null && sheets == null ? null : new PlantAnimAdapter(catalog, sheets));
    }

    public PamPlantClipDurations(PlantAnimAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public float duration(PlantInstance plant, PlantState presentation) {
        if (adapter == null) {
            return 0f;
        }
        return adapter.durationFor(plant, presentation);
    }

    @Override
    public float attackImpactFraction(PlantInstance plant) {
        if (adapter == null) {
            return TimedPlantAction.DEFAULT_ATTACK_FIRE_FRACTION;
        }
        float fraction = adapter.attackImpactFraction(plant);
        return fraction > 0f ? fraction : TimedPlantAction.DEFAULT_ATTACK_FIRE_FRACTION;
    }
}
