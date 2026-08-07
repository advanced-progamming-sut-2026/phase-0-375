package model.enums;

public enum LevelUpgradeType {
    /** Increases damage dealt. */
    BUFF_DAMAGE,
    /** Increases the plant's max HP. */
    BUFF_HP,
    /** Reduces the sun cost of planting. */
    BUFF_COST,
    /** Reduces the seed recharge time. */
    BUFF_RECHARGE,
    /** Reduces the action interval (faster shooting / producing / etc.). */
    BUFF_ACTION_INTERVAL,
    /** A non-numeric behavioral change; see {@link PlantSpecialTag}. */
    SPECIAL_MECHANIC
}
