package model.enums;

public enum LevelUpgradeType {
    STAT_DAMAGE,            // increases damage dealt
    STAT_HP,                // increases base HP
    STAT_COST,              // reduces sun cost
    STAT_COOLDOWN,          // reduces recharge time
    STAT_ATTACK_SPEED,      // increases attacks per second
    STAT_RANGE,             // increases range in tiles
    STAT_PRODUCTION_TIME,   // reduces time between sun productions
    STAT_ARM_TIME,          // reduces arming delay for traps
    STAT_AOE_DAMAGE,        // increases splash/AoE damage
    STAT_CHANCE,            // increases a proc chance (e.g. double sun, plant food)
    STAT_LIFESPAN,          // increases lifespan of temporary plants
    STAT_REFLECT_DAMAGE,    // increases reflected damage (Endurian)
    STAT_STUN_CHANCE,       // increases stun chance (Kernel-pult)
    STAT_FREEZE_TIME,       // increases freeze duration (Iceberg Lettuce)
    SPECIAL
}
