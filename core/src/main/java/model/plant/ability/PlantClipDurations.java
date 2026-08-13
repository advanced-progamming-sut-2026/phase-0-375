package model.plant.ability;

/**
 * Lookup for plant PAM clip lengths (seconds). Implemented by the GUI from
 * {@code animations.json}; returns {@code 0} when unknown so callers can fall back.
 */
@FunctionalInterface
public interface PlantClipDurations {

    PlantClipDurations NONE = (plantDefinitionName, preferredClipNames) -> 0f;

    /**
     * @param plantDefinitionName plant definition display name (e.g. {@code "Peashooter"})
     * @param preferredClipNames  clip names to try in order (e.g. {@code "attack"})
     * @return duration in seconds, or {@code 0} if not found
     */
    float duration(String plantDefinitionName, String... preferredClipNames);
}
