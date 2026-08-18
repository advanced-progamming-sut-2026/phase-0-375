package model.plant.ability;

import model.enums.PlantState;
import model.plant.instance.PlantInstance;

/**
 * Holds a presentation {@link PlantState} for a fixed duration, optionally
 * running an effect on {@link #start}, at a fraction of the clip, and/or when
 * the window completes.
 */
public final class TimedPlantAction implements PlantAction {

    /** Fallback only when {@link PlantAbilityContext#plantPresentationDuration} returns 0. */
    public static final float DEFAULT_ATTACK_DURATION = 0.6f;
    public static final float DEFAULT_PRODUCING_DURATION = 0.5f;

    /** Default fractions used for different states. */
    public static final float DEFAULT_ATTACK_FIRE_FRACTION = 0.4f;
    public static final float DEFAULT_PRODUCING_FRACTION = 0.3f;

    @FunctionalInterface
    public interface Effect {
        void apply(PlantInstance plant, PlantAbilityContext context);
    }

    private final PlantState presentation;
    private final float duration;
    private final Effect onStart;
    private final Effect onAtFraction;
    private final float fireFraction;
    private final Effect onFinish;

    private float remaining;
    private float elapsed;
    private PlantState restoreState;
    private boolean fractionEffectApplied;
    private boolean finishEffectApplied;

    public TimedPlantAction(PlantState presentation, float duration) {
        this(presentation, duration, null, null, 0f, null);
    }

    public TimedPlantAction(PlantState presentation, float duration, Effect onStart) {
        this(presentation, duration, onStart, null, 0f, null);
    }

    public TimedPlantAction(PlantState presentation, float duration, Effect onStart, Effect onFinish) {
        this(presentation, duration, onStart, null, 0f, onFinish);
    }

    public TimedPlantAction(PlantState presentation, float duration, Effect onStart,
                            Effect onAtFraction, float fireFraction, Effect onFinish) {
        if (presentation == null) {
            throw new IllegalArgumentException("presentation state required");
        }
        this.presentation = presentation;
        this.duration = Math.max(0f, duration);
        this.onStart = onStart;
        this.onAtFraction = onAtFraction;
        this.fireFraction = clamp01(fireFraction);
        this.onFinish = onFinish;
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

    /** Attack window that fires {@code onFinish} after the clip completes (not on cancel). */
    public static TimedPlantAction attackThen(PlantInstance plant, PlantAbilityContext context, Effect onFinish) {
        return new TimedPlantAction(PlantState.ATTACKING, presentationDurationFor(
                plant, context, PlantState.ATTACKING, DEFAULT_ATTACK_DURATION), null, onFinish);
    }

    /**
     * Attack window that fires {@code onFire} after {@link #DEFAULT_ATTACK_FIRE_FRACTION}
     * of the clip, then holds the attack pose until the clip ends.
     */
    public static TimedPlantAction attackAt(PlantInstance plant, PlantAbilityContext context, Effect onFire) {
        return attackAt(plant, context, DEFAULT_ATTACK_FIRE_FRACTION, onFire);
    }

    /**
     * Attack window that fires {@code onFire} after {@code fireFraction} of the clip
     * (0 = start, 1 = end), then holds the attack pose until the clip ends.
     * Cancel (freeze, plant-food, death) does not fire.
     */
    public static TimedPlantAction attackAt(PlantInstance plant, PlantAbilityContext context,
                                           float fireFraction, Effect onFire) {
        return new TimedPlantAction(PlantState.ATTACKING, presentationDurationFor(
                plant, context, PlantState.ATTACKING, DEFAULT_ATTACK_DURATION),
                null, onFire, fireFraction, null);
    }

    /**
     * Same as {@link TimedPlantAction#attackAt(PlantInstance, PlantAbilityContext, float, Effect)}
     * but with {@link PlantState#PRODUCING} state.
     */
    public static TimedPlantAction produceAt(PlantInstance plant, PlantAbilityContext context,
                                             float fireFraction, Effect onFire) {
        return new TimedPlantAction(PlantState.PRODUCING, presentationDurationFor(
            plant, context, PlantState.PRODUCING, DEFAULT_PRODUCING_DURATION),
            null, onFire, fireFraction, null);
    }


    /**
     * Same as {@link TimedPlantAction#attack(PlantInstance, PlantAbilityContext, Effect)}
     * but with {@link PlantState#PRODUCING} state.
     */
    public static TimedPlantAction produceAt(PlantInstance plant, PlantAbilityContext context, Effect onFire) {
        return produceAt(plant, context, DEFAULT_PRODUCING_FRACTION, onFire);
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

    private static float clamp01(float value) {
        if (value <= 0f) {
            return 0f;
        }
        return Math.min(value, 1f);
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
        elapsed = 0f;
        fractionEffectApplied = false;
        finishEffectApplied = false;
        if (onStart != null) {
            onStart.apply(plant, context);
        }
        if (onAtFraction != null && fireFraction <= 0f) {
            applyFractionEffect(plant, context);
        }
    }

    @Override
    public boolean tick(PlantInstance plant, PlantAbilityContext context, float deltaTime) {
        remaining -= deltaTime;
        elapsed += deltaTime;
        if (onAtFraction != null && !fractionEffectApplied) {
            float threshold = duration * fireFraction;
            if (elapsed >= threshold || remaining <= 0f) {
                applyFractionEffect(plant, context);
            }
        }
        if (remaining > 0f) {
            return false;
        }
        applyFinishEffect(plant, context);
        finish(plant);
        return true;
    }

    @Override
    public void cancel(PlantInstance plant) {
        finish(plant);
    }

    private void applyFractionEffect(PlantInstance plant, PlantAbilityContext context) {
        if (fractionEffectApplied || onAtFraction == null) {
            return;
        }
        fractionEffectApplied = true;
        onAtFraction.apply(plant, context);
    }

    private void applyFinishEffect(PlantInstance plant, PlantAbilityContext context) {
        if (finishEffectApplied || onFinish == null) {
            return;
        }
        finishEffectApplied = true;
        onFinish.apply(plant, context);
    }

    private void finish(PlantInstance plant) {
        if (plant.getState() == presentation) {
            plant.setState(restoreState != null ? restoreState : PlantState.IDLE);
        }
    }
}
