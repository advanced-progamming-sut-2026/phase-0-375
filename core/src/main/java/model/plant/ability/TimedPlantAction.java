package model.plant.ability;

import model.enums.PlantState;
import model.plant.instance.PlantInstance;

/**
 * Holds a presentation {@link PlantState} for a fixed duration, optionally
 * running an effect on {@link #start}.
 */
public final class TimedPlantAction implements PlantAction {

    /** Fallback only when {@link PlantAbilityContext#plantPresentationDuration} returns 0. */
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

    /** Presentation-only attack window using the view's attack-clip length. */
    public static TimedPlantAction attackHold(PlantInstance plant, PlantAbilityContext context) {
        return new TimedPlantAction(PlantState.ATTACKING, presentationDurationFor(
                plant, context, PlantState.ATTACKING, DEFAULT_ATTACK_DURATION));
    }

    /** Attack window that fires {@code onStart} when the action begins. */
    public static TimedPlantAction attack(PlantInstance plant, PlantAbilityContext context, Effect onStart) {
        return new TimedPlantAction(PlantState.ATTACKING, presentationDurationFor(
                plant, context, PlantState.ATTACKING, DEFAULT_ATTACK_DURATION), onStart);
    }

    /**
     * Length of a presentation state from the view mapping. Falls back to
     * {@code fallback} when no clip data is wired (headless / TUI).
     */
    public static float presentationDurationFor(PlantInstance plant, PlantAbilityContext context,
                                               PlantState presentation, float fallback) {
        float duration = 0f;
        if (context != null && plant != null && presentation != null) {
            duration = context.plantPresentationDuration(plant, presentation);
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
