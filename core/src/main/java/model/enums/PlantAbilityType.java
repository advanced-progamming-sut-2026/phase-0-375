package model.enums;

public enum PlantAbilityType {
    /** Periodically produces sun (Sunflower, Sun-shroom, …). */
    PRODUCE_SUN,
    /** One-shot sun burst on planting (Gold Bloom). */
    INSTANT_SUN_BURST,
    /** Fires one or more pellets per action (Peashooter, Repeater, …). */
    SHOOT_PROJECTILE,
    /** Explodes the moment it is planted (Cherry Bomb, Jalapeno, …). */
    INSTANT_EXPLOSIVE,
    /** Arms and explodes on trigger (Potato Mine, Squash, Tangle Kelp, …). */
    DELAYED_EXPLOSIVE,
    /** Hits adjacent zombies on a cooldown (Bonk Choy, Wasabi Whip, …). */
    MELEE_ATTACK,
    /** Pure HP sponge; no active action (Wall-nut, Tall-nut, Pumpkin, …). */
    PASSIVE_SHIELD,
    /** Field-modifier utility (Torchwood, Magnet-shroom, Hypno-shroom, …). */
    MODIFIER_UTILITY,
    /** Boosts every plant of the same family for a short time (*-mint). */
    MINT_FAMILY_BOOST
}
