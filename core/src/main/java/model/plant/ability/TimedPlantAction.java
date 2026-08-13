package model.plant.ability;

import model.enums.PlantState;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;

/**
 * Holds a presentation {@link PlantState} for a fixed duration, optionally
 * running an effect on {@link #start}.
 */
public final class TimedPlantAction implements PlantAction {

    /** Fallback only when {@link PlantAbilityContext#plantClipDuration} returns 0. */
    public static final float DEFAULT_ATTACK_DURATION = 0.6f;

    @FunctionalInterface
    public interface Effect {
        void apply(PlantInstance plant, PlantAbilityContext context);
    }

    private final PlantState presentation;
    private final float duration;
    private final Effect onStart;

    private float remaining;
    private PlantState restoreState;

    public TimedPlantAction(PlantState presentation, float duration) {
        this(presentation, duration, null);
    }

    public TimedPlantAction(PlantState presentation, float duration, Effect onStart) {
        if (presentation == null) {
            throw new IllegalArgumentException("presentation state required");
        }
        this.presentation = presentation;
        this.duration = Math.max(0f, duration);
        this.onStart = onStart;
    }

    /** Presentation-only attack window using the plant's {@code attack} clip length. */
    public static TimedPlantAction attackHold(PlantInstance plant, PlantAbilityContext context) {
        return new TimedPlantAction(PlantState.ATTACKING, attackDurationFor(plant, context));
    }

    /** Attack window that fires {@code onStart} when the action begins. */
    public static TimedPlantAction attack(PlantInstance plant, PlantAbilityContext context, Effect onStart) {
        return new TimedPlantAction(PlantState.ATTACKING, attackDurationFor(plant, context), onStart);
    }

    /**
     * Resolves attack presentation length from the catalog ({@code attack}, then
     * common aliases). Falls back to {@link #DEFAULT_ATTACK_DURATION} when no
     * clip data is wired (headless / TUI).
     */
    public static float attackDurationFor(PlantInstance plant, PlantAbilityContext context) {
        return clipDurationFor(plant, context, DEFAULT_ATTACK_DURATION,
            "attack", "special_stage1", "special", "special2", "special3");
    }

    /**
     * Generic clip-length helper for any presentation action.
     *
     * @param fallback used only when the catalog returns {@code 0}
     * @param preferredClips clip names tried in order
     */
    public static float clipDurationFor(PlantInstance plant, PlantAbilityContext context,
                                        float fallback, String... preferredClips) {
        float duration = 0f;
        Plant def = plant != null ? plant.getDefinition() : null;
        if (context != null && def != null && def.getName() != null && preferredClips != null) {
            duration = context.plantClipDuration(def.getName(), preferredClips);
        }
        if (duration <= 0f) {
            duration = Math.max(0f, fallback);
        }
        return duration;
    }

    @Override
    public void start(PlantInstance plant, PlantAbilityContext context) {
        restoreState = plant.getState();
        if (restoreState == presentation) {
            restoreState = PlantState.IDLE;
        }
        plant.setState(presentation);
        plant.bumpActionEpoch();
        remaining = duration;
        if (onStart != null) {
            onStart.apply(plant, context);
        }
    }

    @Override
    public boolean tick(PlantInstance plant, PlantAbilityContext context, float deltaTime) {
        remaining -= deltaTime;
        if (remaining > 0f) {
            return false;
        }
        finish(plant);
        return true;
    }

    @Override
    public void cancel(PlantInstance plant) {
        finish(plant);
    }

    private void finish(PlantInstance plant) {
        if (plant.getState() == presentation) {
            plant.setState(restoreState != null ? restoreState : PlantState.IDLE);
        }
    }
}
