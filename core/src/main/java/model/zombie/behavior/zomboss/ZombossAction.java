package model.zombie.behavior.zomboss;

/**
 * Attack kinds a Zomboss may roll.
 */
public enum ZombossAction {
    /** Spawn a few chapter zombies onto the lawn. */
    SUMMON,
    /** Move the two-row body to another pair of lanes. */
    CHANGE_LANE,

    // --- Dark Ages ---
    /** Lob fireballs onto random tiles (burn + destroy plant + Dragon Imp). */
    FIREBALLS,
    /** Burn every tile on the boss's two occupied rows. */
    BURN_ROWS,

    // --- Ancient Egypt ---
    /** Missile that destroys a plant and plants graves nearby. */
    MISSILE,
    /** Dash forward, destroy plants on both rows, return. */
    CHARGE,

    // --- Frostbite Caves ---
    /** Ice missile that destroys a plant. */
    ICE_MISSILE,
    /** Chill wind on two random rows. */
    ICE_WIND,
    /** Freeze a random column and plant frozen zombies. */
    FREEZE_COLUMN,

    // --- Big Wave Beach ---
    /** Baby sharks that eat a plant on water. */
    BABY_SHARK,
    /** Pull plants/zombies on the two facing rows toward the boss. */
    TURBINE
}
