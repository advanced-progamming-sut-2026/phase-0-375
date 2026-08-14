package model.plant.ability;

import model.enums.PlantState;
import model.plant.instance.PlantInstance;

/**
 * Lookup for plant presentation lengths (seconds).
 * returns {@code 0} when unknown so callers can fall back.
 */
@FunctionalInterface
public interface PlantClipDurations {

    PlantClipDurations NONE = (plant, presentation) -> 0f;

    /**
     * @param plant         the plant whose clip should be timed
     * @param presentation  intended {@link PlantState} (e.g. {@link PlantState#ATTACKING}),
     *                      which may differ from {@code plant.getState()}
     * @return duration in seconds, or {@code 0} if unknown
     */
    float duration(PlantInstance plant, PlantState presentation);
}
