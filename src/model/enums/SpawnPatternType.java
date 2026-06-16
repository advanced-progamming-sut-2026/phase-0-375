package model.enums;

/**
 * The pattern controls how the entry's count is distributed over
 * time and across lanes.
 */
public enum SpawnPatternType {

    /**
     * Zombies appear one at a time.
     */
    SINGLE,

    /**
     * All zombies in the entry spawn at the same instant.
     */
    GROUP,

    /**
     * Zombies spawn as a continuous stream over a duration specified by
     * {@code streamDurationSeconds} on the entry.
     */
    STREAM,

    /**
     * Like {@link #GROUP}, but zombies are guaranteed to occupy distinct
     * lanes (no two on the same lane at the same instant).
     */
    AMBUSH
}