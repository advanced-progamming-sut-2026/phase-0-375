package model.enums;

/**
 * Identifies a special, non-numeric behavioural modifier attached to a
 * {@code SPECIAL_MECHANIC} level upgrade.
 */
public enum PlantSpecialTag {
    // --- Sun producers ---
    /** Chance (0..1) per production tick to drop double sun. */
    DOUBLE_SUN_CHANCE,
    /** Per-level reduction (seconds) of the warm-up / growth time. */
    GROW_TIME_REDUCTION,
    /** Per-level increase of the sun amount dropped per tick. */
    SUN_AMOUNT_BUFF,
    /** Per-level increase of the bonus sun dropped on plant-food. */
    SUN_DROP_INCREMENT,

    // --- Shooters ---
    /** Adds N extra pierce targets per pellet (Cactus, Fume-shroom line). */
    ADDITIONAL_PIERCE,
    /** Per-level reduction (seconds) of the charge wind-up (Citron, …). */
    CHARGE_REDUCTION,
    /** Adds N to the bounce count of lobbed bulbs (Bowling Bulb). */
    GRAPE_BOUNCE_EXT,
    /** Adds N to the splash damage radius of lobbed projectiles. */
    SPLASH_DAMAGE_BUFF,
    /** Adds N to the chill duration of ice-element pellets (Snow Pea, …). */
    CHILL_DURATION_EXT,
    /** Adds N to the freeze duration of freezing projectiles (Winter Melon, …). */
    FREEZE_DURATION_EXT,
    /** Adds N to the per-tick poison damage (Goo Peashooter, …). */
    POISON_TICK_BUFF,

    // --- Lobbers ---
    /** Adds N to the chance per shot to throw a butter stun (Kernel-pult). */
    BUTTER_CHANCE_BUFF,

    // --- Explosives ---
    /** Adds N to the explosion damage (Potato Mine, Cherry Bomb line). */
    EXPLODE_DAMAGE_BUFF,
    /** Adds N tiles to the explosion AoE radius. */
    DEATH_EXPLOSION_AOE,
    /** Plant explodes when its lifespan/duration ends (Explode-o-nut line). */
    EXPLODE_ON_FINISH,
    /** Per-level reduction (seconds) of the arm delay (Potato Mine, …). */
    ARM_TIME_REDUCTION,
    /** Adds N to the 3x3 melt area (Hot Potato, …). */
    MELT_AREA_3X3,
    /** Adds N tiles to the lane-length of a row-clearing explosion (Jalapeno, …). */
    TILE_RANGE_EXT,

    // --- Melee ---
    /** Per-level reduction (seconds) of the digestion time (Chomper). */
    DIGEST_REDUCTION,
    /** Per-level reduction (seconds) of the eat cooldown (Chomper, Bonk Choy). */
    EAT_TIME_REDUCTION,
    /** Adds N to the reflected damage when eaten (Endurian). */
    REFLECT_DAMAGE_BUFF,
    /** Adds N to the max growth stage (Kiwibeast, Sun-shroom). */
    GROWTH_STAGE_MAX_UP,
    /** Adds N seconds to the lifespan of temporary plants (Puff-shroom, …). */
    LIFESPAN_EXT,
    /** Speeds up the post-eat regeneration rate (Kiwibeast, …). */
    REGEN_SPEEDUP,

    // --- Wall-nuts ---
    /** Adds N seconds to the duration of any defensive buff (Tall-nut jump-block). */
    DURATION_EXT,
    /** Adds N to the warm-radius / detection radius (Sweet Potato, Garlic line). */
    WARM_RADIUS_EXT,

    // --- Modifiers / Homing ---
    /** Multiplier applied to zombie HP while the modifier is active. */
    ZOMBIE_HEALTH_MULTIPLIER,
    /** Multiplier applied to incoming zombie damage while the modifier is active. */
    ZOMBIE_DAMAGE_MULTIPLIER,
    /** Chance per second to auto-trigger plant-food on nearby allies. */
    AUTO_PLANT_FOOD_CHANCE,
    /** Auto-triggers plant-food the first time a zombie enters the lane. */
    AUTO_PLANTFOOD_ON_ENTER,
    /** Adds N to the number of grabbable targets (Magnet-shroom, Cat-tail). */
    BONUS_GRAB_TARGETS,
    /** Adds N to the number of smash charges (Citron, Magnifying Grass). */
    BONUS_SMASH_CHARGES,
    /** Resets cooldowns of every plant in the same family (used by *-mints). */
    RESET_FAMILY_COOLDOWNS,
    /** Tells the AI to prefer Gargantuars as the target (Caulipower, …). */
    PRIORITIZE_GARGANTUARS,

    /** Sentinel for "no special mechanic". */
    NONE
}
