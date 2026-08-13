package view.gui.anim.plant;

import model.enums.PlantState;
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
        PlantAnimRole role = roleFor(plant, entry);
        AnimPose custom = overrides.tryResolve(plant, entry, role);
        if (custom != null) {
            return custom;
        }
        String clip = catalog.resolveClip(entry, preferredClips(role));
        if (clip == null) {
            return null;
        }
        return (role.isLooping())
            ? AnimPose.looping(entry.path(), clip, role)
            : AnimPose.once(entry.path(), clip, role);
    }

    /**
     * Global plant role mapping. Currently idle + plant-food intro/loop/outro.
     *
     * <p>TODO: map {@link model.enums.PlantState#ATTACKING} → ATTACK,
     * {@link model.enums.PlantState#ARMED}/{@code ARMING} → ARMED,
     * {@link model.enums.PlantState#GROWING} → GROWING,
     * {@link model.enums.PlantState#DYING} → DIE.
     */
    private PlantAnimRole roleFor(PlantInstance plant, PamCatalog.PamEntry entry) {
        if (plant == null || plant.getDefinition() == null) {
            return PlantAnimRole.IDLE;
        }

        if (plant.getState() == PlantState.PLANT_FOOD) {
            return plantFoodRole(plant, entry);
        }

        return PlantAnimRole.IDLE;
    }

    /**
     * {@code plantfood_on} for the first clip-length of the effect,
     * {@code plantfood_off} for the last, looping {@code plantfood} in between.
     * Missing on/off clips are skipped (duration {@code 0} in the catalog).
     */
    private PlantAnimRole plantFoodRole(PlantInstance plant, PamCatalog.PamEntry entry) {
        float remaining = plant.getPlantFoodDurationRemaining();
        float offDur = catalog.clipDurationSeconds(entry, "plantfood_off");
        if (offDur > 0f && remaining <= offDur) {
            return PlantAnimRole.PLANT_FOOD_OFF;
        }
        float onDur = catalog.clipDurationSeconds(entry, "plantfood_on");
        float elapsed = PlantInstance.PLANT_FOOD_DURATION - remaining;
        if (onDur > 0f && elapsed < onDur) {
            return PlantAnimRole.PLANT_FOOD_ON;
        }
        return PlantAnimRole.PLANT_FOOD;
    }

    private static String[] preferredClips(PlantAnimRole role) {
        return switch (role) {
            case IDLE -> new String[]{"idle", "idle2", "idle1", "loop"};
            case PLANT_FOOD_ON -> new String[]{"plantfood_on"};
            case PLANT_FOOD -> new String[]{"plantfood_loop", "plantfood", "plantfood_idle", "idle"};
            case PLANT_FOOD_OFF -> new String[]{"plantfood_off"};
            // TODO: case ATTACK -> new String[]{"attack", "idle"};
            // TODO: case GROWING -> new String[]{"idle", "idle_stage1"};
            // TODO: case ARMED -> new String[]{"ready_idle", "idle"};
            // TODO: case DIE -> new String[]{"die", "death"};
            // TODO: case SPECIAL -> ...
        };
    }
}
