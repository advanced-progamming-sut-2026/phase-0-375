package view.gui.anim.plant;

import model.enums.PlantState;
import model.plant.instance.PlantInstance;
import view.gui.anim.AnimPose;
import view.gui.assets.PamCatalog;
import view.gui.assets.PlantSpritesheetCatalog;

import java.util.Set;

/**
 * Global plant defaults: model → {@link PlantAnimRole} → PAM clip (or spritesheet fallback).
 *
 * <p><b>Ownership:</b> plant team. Exclusive plants go through {@link PlantAnimOverrides}.
 * Do not mutate the model here.
 */
public final class PlantAnimAdapter {
    private static final String[] PLANT_FOOD_MAIN_CLIPS = {"plantfood_loop", "plantfood", "plantfood_idle", "pf"};

    /** Plants whose {@code PlantAnimRole.PLANT_FOOD} should loop for the full effect. */
    private static final Set<String> PLANT_FOOD_LOOP_BY_NAME = Set.of(
        "Peashooter",
        "Mega Gatling Pea",
        "Repeater",
        "Snow Pea",
        "Split Pea",
        "Threepeater",
        "Torchwood",
        "Bowling Bulb"
    );

    private final PamCatalog catalog;
    private final PlantSpritesheetCatalog sheets;
    private final PlantAnimOverrides overrides;

    public PlantAnimAdapter(PamCatalog catalog) {
        this(catalog, null, PlantAnimOverrides.createDefault());
    }

    public PlantAnimAdapter(PamCatalog catalog, PlantSpritesheetCatalog sheets) {
        this(catalog, sheets, PlantAnimOverrides.createDefault());
    }

    public PlantAnimAdapter(PamCatalog catalog, PlantAnimOverrides overrides) {
        this(catalog, null, overrides);
    }

    public PlantAnimAdapter(PamCatalog catalog, PlantSpritesheetCatalog sheets,
                            PlantAnimOverrides overrides) {
        this.catalog = catalog;
        this.sheets = sheets;
        this.overrides = overrides != null ? overrides : PlantAnimOverrides.createDefault();
    }

    public AnimPose poseFor(PlantInstance plant) {
        if (plant == null || plant.getDefinition() == null) {
            return null;
        }
        return poseFor(plant, plant.getState());
    }

    /**
     * Pose for an intended presentation, which may differ from {@code plant.getState()}
     * (e.g. sampling the attack muzzle after the plant has already left ATTACKING).
     */
    public AnimPose poseFor(PlantInstance plant, PlantState presentation) {
        if (plant == null || plant.getDefinition() == null || presentation == null) {
            return null;
        }
        return poseForPresentation(plant, presentation);
    }

    /**
     * Clip length for {@code presentation}, using the same exclusive/default mapping
     * as {@link #poseFor}.
     */
    public float durationFor(PlantInstance plant, PlantState presentation) {
        if (plant == null || plant.getDefinition() == null || presentation == null) {
            return 0f;
        }
        PamCatalog.PamEntry entry = catalog == null ? null : catalog.forPlant(plant.getDefinition().getName());
        if (entry == null) {
            return sheetDuration(plant, presentation);
        }
        PlantAnimRole role = roleForPresentation(plant, entry, presentation);
        float exclusive = overrides.tryDuration(plant, entry, role);
        if (exclusive > 0f) {
            return exclusive;
        }
        AnimPose custom = overrides.tryResolve(plant, entry, role);
        if (custom != null) {
            if (custom.isSpritesheet()) {
                return sheetDurationForPose(plant, role);
            }
            return PamCatalog.clipDurationSeconds(entry, custom.clipName());
        }
        for (String clip : durationClips(role)) {
            float seconds = PamCatalog.clipDurationSeconds(entry, clip);
            if (seconds > 0f) {
                return seconds;
            }
        }
        return 0f;
    }

    /** Fraction of the attack presentation at which the ability effect should fire. */
    public float attackImpactFraction(PlantInstance plant) {
        if (plant == null || plant.getDefinition() == null || catalog == null) {
            return 0f;
        }
        PamCatalog.PamEntry entry = catalog.forPlant(plant.getDefinition().getName());
        if (entry == null) {
            return 0f;
        }
        return overrides.tryImpactFraction(plant, entry, PlantAnimRole.ATTACK);
    }

    private AnimPose poseForPresentation(PlantInstance plant, PlantState presentation) {
        PamCatalog.PamEntry entry = catalog == null ? null : catalog.forPlant(plant.getDefinition().getName());
        if (entry == null) {
            return sheetPose(plant, presentation);
        }
        PlantAnimRole role = roleForPresentation(plant, entry, presentation);
        AnimPose custom = overrides.tryResolve(plant, entry, role);
        AnimPose pose = custom;
        if (pose == null) {
            String clip = catalog.resolveClip(entry, preferredClips(role));
            if (clip == null) {
                return sheetPose(plant, presentation);
            }
            pose = (role.isLooping())
                ? AnimPose.looping(entry.path(), clip, role)
                : AnimPose.once(entry.path(), clip, role);
        }
        return applyPlantFoodPlaybackMode(plant, role, pose);
    }

    private AnimPose sheetPose(PlantInstance plant, PlantState presentation) {
        if (sheets == null || plant.getDefinition() == null) {
            return null;
        }
        String name = plant.getDefinition().getName();
        PlantAnimRole role = roleForSheet(presentation);
        PlantSpritesheetCatalog.ClipSpec spec = resolveSheet(name, role);
        if (spec == null) {
            return null;
        }
        return role.isLooping()
                ? AnimPose.sheetLooping(spec.relativePath(), spec.cacheKey(), role)
                : AnimPose.sheetOnce(spec.relativePath(), spec.cacheKey(), role);
    }

    private float sheetDuration(PlantInstance plant, PlantState presentation) {
        if (sheets == null || plant.getDefinition() == null) {
            return 0f;
        }
        PlantAnimRole role = roleForSheet(presentation);
        return sheetDurationForPose(plant, role);
    }

    private float sheetDurationForPose(PlantInstance plant, PlantAnimRole role) {
        PlantSpritesheetCatalog.ClipSpec spec = resolveSheet(plant.getDefinition().getName(), role);
        return spec == null ? 0f : spec.durationSeconds();
    }

    private PlantSpritesheetCatalog.ClipSpec resolveSheet(String definitionName, PlantAnimRole role) {
        PlantSpritesheetCatalog.ClipSpec spec = sheets.resolveClip(definitionName, preferredClips(role));
        if (spec != null) {
            return spec;
        }
        if (role == PlantAnimRole.IDLE || role == PlantAnimRole.PLANT_FOOD
                || role == PlantAnimRole.PLANT_FOOD_ON || role == PlantAnimRole.PLANT_FOOD_OFF) {
            return sheets.idleFallback(definitionName);
        }
        return sheets.anyClip(definitionName);
    }

    private static PlantAnimRole roleForSheet(PlantState presentation) {
        if (presentation == PlantState.ATTACKING) {
            return PlantAnimRole.ATTACK;
        }
        if (presentation == PlantState.PRODUCING) {
            return PlantAnimRole.SPECIAL;
        }
        if (presentation == PlantState.PLANT_FOOD) {
            return PlantAnimRole.PLANT_FOOD;
        }
        return PlantAnimRole.IDLE;
    }

    /**
     * Global plant role mapping. Idle, attack, and plant-food intro/loop/outro.
     *
     * {@link PlantState#ARMING} and {@link PlantState#ARMED} use IDLE so
     * exclusive charge plants (Potato Mine) can pick buried vs ready clips.
     * {@link PlantState#GROWING} and {@link PlantState#DYING} still fall through.
     */
    private PlantAnimRole roleForPresentation(PlantInstance plant, PamCatalog.PamEntry entry,
                                             PlantState presentation) {
        if (plant == null || plant.getDefinition() == null || presentation == null) {
            return PlantAnimRole.IDLE;
        }

        if (presentation == PlantState.PLANT_FOOD) {
            return plantFoodRole(plant, entry);
        }

        if (presentation == PlantState.ATTACKING) {
            return PlantAnimRole.ATTACK;
        }

        if (presentation == PlantState.PRODUCING) {
            return PlantAnimRole.SPECIAL;
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
        float offDur = PamCatalog.clipDurationSeconds(entry, "plantfood_off");
        if (offDur > 0f && remaining <= offDur) {
            return PlantAnimRole.PLANT_FOOD_OFF;
        }
        float onDur = PamCatalog.clipDurationSeconds(entry, "plantfood_on");
        float elapsed = PlantInstance.PLANT_FOOD_DURATION - remaining;
        if (onDur > 0f && elapsed < onDur) {
            return PlantAnimRole.PLANT_FOOD_ON;
        }
        float mainElapsed = Math.max(0f, elapsed - Math.max(0f, onDur));
        if (!shouldLoopPlantFood(plant) && hasFinishedOncePlantFoodClip(entry, mainElapsed)) {
            if (offDur > 0f) {
                plant.beginPlantFoodOffWindowNow(offDur);
                return PlantAnimRole.PLANT_FOOD_OFF;
            }
            plant.finishPlantFoodNow();
            return PlantAnimRole.IDLE;
        }
        return PlantAnimRole.PLANT_FOOD;
    }

    private static String[] preferredClips(PlantAnimRole role) {
        return switch (role) {
            case IDLE -> new String[]{"idle", "idle2", "idle1", "loop"};
            case PLANT_FOOD_ON -> new String[]{"plantfood_on"};
            case PLANT_FOOD -> new String[]{"plantfood_loop", "plantfood", "plantfood_idle", "pf", "idle"};
            case PLANT_FOOD_OFF -> new String[]{"plantfood_off"};
            case ATTACK -> new String[]{"attack", "idle"};
            case SPECIAL ->  new String[]{"special"};
        };
    }

    /** Clips whose catalog duration may time a presentation. */
    private static String[] durationClips(PlantAnimRole role) {
        return switch (role) {
            case ATTACK -> new String[]{"attack"};
            default -> preferredClips(role);
        };
    }

    private static AnimPose applyPlantFoodPlaybackMode(PlantInstance plant, PlantAnimRole role, AnimPose pose) {
        if (pose == null || role != PlantAnimRole.PLANT_FOOD || !pose.loop() || shouldLoopPlantFood(plant)) {
            return pose;
        }
        AnimPose once = pose.isSpritesheet()
                ? AnimPose.sheetOnce(pose.pamPath(), pose.clipName(), pose.role())
                : AnimPose.once(pose.pamPath(), pose.clipName(), pose.role(), pose.visibility());
        once = once.withScale(pose.scale()).withFlipX(pose.flipX());
        if (pose.reverse()) {
            once = once.reversed();
        }
        return once;
    }

    private static boolean shouldLoopPlantFood(PlantInstance plant) {
        if (plant == null || plant.getDefinition() == null) {
            return false;
        }
        return PLANT_FOOD_LOOP_BY_NAME.contains(plant.getDefinition().getName());
    }

    private boolean hasFinishedOncePlantFoodClip(PamCatalog.PamEntry entry, float elapsedInMainPhase) {
        String plantFoodClip = catalog.resolveClip(entry, PLANT_FOOD_MAIN_CLIPS);
        float plantFoodDur = PamCatalog.clipDurationSeconds(entry, plantFoodClip);
        return plantFoodDur > 0f && elapsedInMainPhase >= plantFoodDur;
    }
}
