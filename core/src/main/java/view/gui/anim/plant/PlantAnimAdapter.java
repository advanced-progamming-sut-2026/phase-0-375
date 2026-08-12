package view.gui.anim.plant;

import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;

/**
 * Global plant defaults: model → {@link PlantAnimRole} → PAM clip.
 *
 * <p><b>Ownership:</b> plant team. Exclusive plants go through {@link PlantAnimOverrides}.
 * Do not mutate the model here.
 */
public final class PlantAnimAdapter {
    private final PamCatalog catalog;
    private final PlantAnimOverrides overrides;

    public PlantAnimAdapter(PamCatalog catalog) {
        this(catalog, PlantAnimOverrides.createDefault());
    }

    public PlantAnimAdapter(PamCatalog catalog, PlantAnimOverrides overrides) {
        this.catalog = catalog;
        this.overrides = overrides != null ? overrides : PlantAnimOverrides.createDefault();
    }

    public AnimPose poseFor(PlantInstance plant) {
        if (plant == null || plant.getDefinition() == null) {
            return null;
        }
        PamCatalog.PamEntry entry = catalog.forPlant(plant.getDefinition().getName());
        if (entry == null) {
            return null;
        }
        PlantAnimRole role = roleFor(plant);
        AnimPose custom = overrides.tryResolve(plant, entry, role);
        if (custom != null) {
            return custom;
        }
        String clip = catalog.resolveClip(entry, preferredClips(role));
        if (clip == null) {
            return null;
        }
        return AnimPose.looping(entry.path(), clip, role);
    }

    /**
     * Global plant role mapping. Currently everything idles.
     *
     * <p>TODO: map {@link model.enums.PlantState#ATTACKING} → ATTACK,
     * {@link model.enums.PlantState#PLANT_FOOD} → PLANT_FOOD,
     * {@link model.enums.PlantState#ARMED}/{@code ARMING} → ARMED,
     * {@link model.enums.PlantState#GROWING} → GROWING,
     * {@link model.enums.PlantState#DYING} → DIE.
     */
    private PlantAnimRole roleFor(PlantInstance plant) {
        return PlantAnimRole.IDLE;
    }

    private static String[] preferredClips(PlantAnimRole role) {
        return switch (role) {
            case IDLE -> new String[]{"idle", "idle2", "idle1", "loop"};
            // TODO: case ATTACK -> new String[]{"attack", "idle"};
            // TODO: case PLANT_FOOD -> new String[]{"plantfood", "plantfood_idle", "idle"};
            // TODO: case GROWING -> new String[]{"idle", "idle_stage1"};
            // TODO: case ARMED -> new String[]{"ready_idle", "idle"};
            // TODO: case DIE -> new String[]{"die", "death"};
            // TODO: case SPECIAL -> ...
        };
    }
}
