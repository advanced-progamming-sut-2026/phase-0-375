package model.shop;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a daily offer in the shop.
 * Each day, a special seed packet deal is offered for a random
 * unlocked plant at a 20% discount. The offer can only be
 * purchased once per day and refreshes at 00:00.
 */
public class DailyOffer {
    private ShopItem item;
    private int basePrice;
    private int discountedPrice;
    private LocalDate offerDate;
    private boolean purchased;

    public DailyOffer(ShopItem item,
                      int basePrice, LocalDate offerDate) {
        this.item = item;
        this.basePrice = basePrice;
        this.discountedPrice = (int) (basePrice * 0.8);
        this.offerDate = offerDate;
        this.purchased = false;
    }

    /**
     * Purchases this daily offer. Can only be done once per day.
     *
     * @return true if the purchase was successful
     */
    public boolean purchase() {
        if (purchased || isExpired()) {
            return false;
        }
        purchased = true;
        return true;
    }

    /**
     * Checks whether this daily offer has already been purchased today.
     *
     * @return true if already purchased
     */
    public boolean isPurchased() {
        return purchased;
    }

    /**
     * Checks whether this daily offer has expired
     * (i.e., the current date is past the offer date).
     *
     * @return true if the offer is expired
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(offerDate);
    }

    /** Seconds until this offer rolls at midnight (00:00 after {@link #offerDate}). */
    public long secondsUntilExpiry() {
        if (isExpired()) {
            return 0;
        }
        LocalDateTime expiry = offerDate.plusDays(1).atStartOfDay();
        return Math.max(0, Duration.between(LocalDateTime.now(), expiry).getSeconds());
    }

    // --- Getters ---

    public ShopItem getItem() { return item;}

    public int getBasePrice() {
        return basePrice;
    }

    public int getDiscountedPrice() {
        return discountedPrice;
    }

    public LocalDate getOfferDate() {
        return offerDate;
    }

    // --- Setters ---

    public void setItem(ShopItem item) {
        this.item = item;
    }

    public void setBasePrice(int basePrice) {
        this.basePrice = basePrice;
    }

    public void setDiscountedPrice(int discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public void setOfferDate(LocalDate offerDate) {
        this.offerDate = offerDate;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }
}
