package model.enums;

/**
 * Kind of plant-food effect a plant triggers when fed.
 */
public enum PlantFoodType {
    /** No plant-food effect (Cherry Bomb, Jalapeno, all *-mints, …). */
    NONE,
    /** Drops a burst of sun items on the field (Sunflower, Twin Sunflower, …). */
    SPAWN_SUN_ITEMS,
    /** Rapid-fires a large volley of pellets (Peashooter, Repeater, …). */
    PROJECTILE_BURST,
    /** Hypnotises random zombies on the map (Caulipower, Hypno-shroom, …). */
    RANDOM_HYPNOTIZE,
    /** Knockback + damage burst around the plant (Fume-shroom, Garlic, …). */
    KNOCKBACK_BLAST,
    /** Freezes every zombie on the map (Iceberg Lettuce, Kernel-pult, …). */
    MAP_WIDE_FREEZE,
    /** Spawns temporary clone plants on adjacent tiles (Potato Mine, Lily Pad, …). */
    SPAWN_CLONES,
    /** AoE melee attack around the plant (Bonk Choy, Phat Beet, …). */
    LOCAL_AOE_ATTACK,
    /** Drags underwater / pulls in zombies in a radius (Tangle Kelp, Chomper, …). */
    PULL_UNDERWATER,
    /** Grants a permanent HP armor to the plant (Wall-nut, Tall-nut, Pumpkin, …). */
    GRANT_PERMANENT_ARMOR,
    /** Pulls nearby zombies onto this lane and fully restores HP (Sweet Potato). */
    ATTRACT_AND_HEAL
}
