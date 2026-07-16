package model.enums;

/** Outcome of a shop purchase attempt. */
public enum PurchaseResult {
    SUCCESS,
    INVALID_ITEM,
    INVALID_COUNT,
    PLANT_TYPE_REQUIRED,
    PLANT_NOT_UNLOCKED,
    INSUFFICIENT_FUNDS,
    CAPACITY_REACHED,
    NO_DAILY_OFFER,
    ALREADY_PURCHASED
}
