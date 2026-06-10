package model.shop;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The shop where players can spend coins and gems.
 * The shop has two sections: permanent items (always available)
 * and daily offers (refreshed at 00:00 each day, one-time purchase).
 * The shop is accessible from the greenhouse.
 */
public class Shop {
    private List<ShopItem> permanentItems;
    private DailyOffer dailyOffer;
    private LocalDate lastRefreshDate;

    public Shop() {
        this.permanentItems = new ArrayList<>();
        this.dailyOffer = null;
        this.lastRefreshDate = null;
        initializePermanentItems();
    }

    /**
     * Initializes the permanent shop items:
     * - Pot (2000 coins)
     * - Plant Food (3 gems)
     * - Random Seed Packet (1000 coins)
     * - Chosen Seed Packet (5 gems)
     * - Currency Conversion (5 gems = 500 coins)
     */
    private void initializePermanentItems() {}

    /**
     * Purchases an item from the shop by its item ID.
     * Deducts the appropriate currency and applies the item effect.
     * For chosen seed packets, the plantType parameter is required.
     *
     * @param itemId the ID of the item to buy
     * @param count the number of units to buy
     * @param plantType the target plant type (for chosen seed packets)
     * @return true if the purchase was successful
     */
    public boolean buy(int itemId, int count, String plantType) {
        return false;
    }

    /**
     * Refreshes the daily offer if the date has changed.
     * Generates a new random seed packet offer with 20% discount.
     */
    public void refreshDailyOffer() {}

    /**
     * Returns all purchasable items (both permanent and daily).
     *
     * @return combined list of all shop items
     */
    public List<ShopItem> getAllPurchasableItems() {
        return Collections.emptyList();
    }

    // --- Getters ---

    public List<ShopItem> getPermanentItems() {
        return Collections.unmodifiableList(permanentItems);
    }

    public DailyOffer getDailyOffer() {
        return dailyOffer;
    }

    public LocalDate getLastRefreshDate() {
        return lastRefreshDate;
    }

    // --- Setters ---

    public void setPermanentItems(List<ShopItem> permanentItems) {
        this.permanentItems = permanentItems;
    }

    public void setDailyOffer(DailyOffer dailyOffer) {
        this.dailyOffer = dailyOffer;
    }

    public void setLastRefreshDate(LocalDate lastRefreshDate) {
        this.lastRefreshDate = lastRefreshDate;
    }
}
