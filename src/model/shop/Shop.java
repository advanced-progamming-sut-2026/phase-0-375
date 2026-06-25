package model.shop;

import model.enums.ShopCategory;
import model.enums.ShopItemType;
import model.user.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
    private User customer;
    private final Random random = new Random();

    public static final int MAX_POTS = 20;
    public static final int MAX_PLANT_FOOD = 3;
    public static final int DAILY_OFFER_PACKET_AMOUNT = 10;
    public static final int DAILY_OFFER_BASE_PRICE = 2000;
    public static final int RANDOM_SEED_PACKET_AMOUNT = 5;
    public static final int CHOSEN_SEED_PACKET_AMOUNT = 10;
    public static final int CURRENCY_CONVERSION_COINS = 500;

    /** Item ID constants for the permanent items + daily offer. */
    public static final int ITEM_ID_POT = 1;
    public static final int ITEM_ID_PLANT_FOOD = 2;
    public static final int ITEM_ID_SEED_RANDOM = 3;
    public static final int ITEM_ID_SEED_CHOSEN = 4;
    public static final int ITEM_ID_CURRENCY_CONVERSION = 5;
    public static final int ITEM_ID_DAILY_OFFER = 6;

    public Shop(User customer) {
        this.customer = customer;
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
    private void initializePermanentItems() {
        permanentItems.add(new ShopItem(
                ITEM_ID_POT,
                ShopItemType.POT,
                ShopCategory.PERMANENT,
                2000,
                "coin",
                MAX_POTS,
                null,
                "Opens a greenhouse pot slot (max 20 pots)"
        ));
        permanentItems.add(new ShopItem(
                ITEM_ID_PLANT_FOOD,
                ShopItemType.PLANT_FOOD,
                ShopCategory.PERMANENT,
                3,
                "gem",
                MAX_PLANT_FOOD,
                null,
                "Plant Food usable at level start (max 3 stored)"
        ));
        permanentItems.add(new ShopItem(
                ITEM_ID_SEED_RANDOM,
                ShopItemType.SEED_PACKET_RANDOM,
                ShopCategory.PERMANENT,
                1000,
                "coin",
                Integer.MAX_VALUE,
                null,
                "5 seed packets for a random unlocked plant"
        ));
        permanentItems.add(new ShopItem(
                ITEM_ID_SEED_CHOSEN,
                ShopItemType.SEED_PACKET_CHOSEN,
                ShopCategory.PERMANENT,
                5,
                "gem",
                Integer.MAX_VALUE,
                null,
                "10 seed packets for a chosen unlocked plant"
        ));
        permanentItems.add(new ShopItem(
                ITEM_ID_CURRENCY_CONVERSION,
                ShopItemType.CURRENCY_CONVERSION,
                ShopCategory.PERMANENT,
                5,
                "gem",
                Integer.MAX_VALUE,
                null,
                "Convert 5 gems to 500 coins"
        ));
    }


    /**
     * Refreshes the daily offer if the date has changed.
     * Generates a new random seed packet offer with 20% discount.
     */
    public void refreshDailyOffer() {
        LocalDate today = LocalDate.now();
        // If we already have an offer created today, keep it.
        if (dailyOffer != null && today.equals(lastRefreshDate)) {
            return;
        }
        // Otherwise, generate a new offer for a random unlocked plant.
        String plant = pickRandomUnlockedPlant();
        if (plant == null) {
            // No unlocked plants available — no daily offer possible.
            dailyOffer = null;
            lastRefreshDate = today;
            return;
        }
        // TODO
//        dailyOffer = new DailyOffer(
//
//        );
        lastRefreshDate = today;

        // If the player already purchased this date's offer in a previous session,
        // restore the purchased flag from the user's persistence map.
        if (customer.getPurchasedDailyDeals() != null
                && Boolean.TRUE.equals(customer.getPurchasedDailyDeals().get(today.toString()))) {
            dailyOffer.setPurchased(true);
        }
    }

    public boolean buy(int itemId, int count, String plantType) {
        // TODO
        return false;
    }


    /**
     * Convenience method for buying the daily offer.
     * Equivalent to {@code buy(ITEM_ID_DAILY_OFFER, 1, null)} but
     * also enforces the once-per-day rule and uses the discounted price.
     *
     * @return true if the daily offer was purchased successfully
     */
    public boolean buyDailyOffer() {
        if (customer == null) {
            return false;
        }
        // Ensure today's offer is loaded.
        if (needsRefresh()) {
            refreshDailyOffer();
        }
        if (dailyOffer == null) {
            return false;
        }
        if (dailyOffer.isPurchased()) {
            return false;
        }

        int cost = dailyOffer.getDiscountedPrice();
        if (customer.getCoins() < cost) {
            return false;
        }

        // Deduct coins.
        customer.setCoins(customer.getCoins() - cost);

        // Grant the seed packets.
        // TODO

        // Mark as purchased (both in-memory and in the user's persistence map).
        dailyOffer.setPurchased(true);
        if (customer.getPurchasedDailyDeals() != null) {
            customer.getPurchasedDailyDeals()
                    .put(dailyOffer.getOfferDate().toString(), true);
        }
        return true;
    }

    /**
     * Validates whether the player can afford the given item in the given
     * quantity. Checks the player's coin/gem balance against the item's
     * price × count.
     *
     * @param item  the shop item to check
     * @param count the number of units
     * @return true if the player has enough currency
     */
    public boolean canAfford(ShopItem item, int count) {
        if (item == null || count <= 0 || customer == null) {
            return false;
        }
        int totalCost = item.getPrice() * count;
        if ("coin".equalsIgnoreCase(item.getCurrency())) {
            return customer.getCoins() >= totalCost;
        } else if ("gem".equalsIgnoreCase(item.getCurrency())) {
            return customer.getGems() >= totalCost;
        }
        return false;
    }

    /**
     * Validates whether the player has enough remaining capacity
     * for the given item type. Only meaningful for POT (max 20) and
     * PLANT_FOOD (max 3). Other item types always return true.
     *
     * @param itemType the type of item to check
     * @param count    the number of units to add
     * @return true if the player has enough capacity
     */
    public boolean hasCapacity(ShopItemType itemType, int count) {
        if (count <= 0 || customer == null) {
            return false;
        }
        switch (itemType) {
            case POT:
                int currentPots = customer.getUnlockedPots();
                return currentPots + count <= MAX_POTS;
            case PLANT_FOOD:
                int currentFood = customer.getPlantFoodCount();
                return currentFood + count <= MAX_PLANT_FOOD;
            default:
                // Seed packets and currency conversion have no cap.
                return true;
        }
    }

    /**
     * Deducts the cost of an item from the player's wallet.
     * Handles both coin and gem deductions.
     *
     * @param item  the item being purchased
     * @param count the number of units
     */
    private void deductCurrency(ShopItem item, int count) {
        int totalCost = item.getPrice() * count;
        if ("coin".equalsIgnoreCase(item.getCurrency())) {
            customer.setCoins(customer.getCoins() - totalCost);
        } else if ("gem".equalsIgnoreCase(item.getCurrency())) {
            customer.setGems(customer.getGems() - totalCost);
        }
    }

    /**
     * Applies the effect of a purchased item to the player's profile.
     * Delegates to the appropriate handler based on item type.
     *
     * @param item      the purchased item
     * @param count     the number of units
     * @param plantType the target plant type (for seed packets)
     */
    private void applyItemEffect(ShopItem item, int count, String plantType) {
        switch (item.getItemType()) {
            case POT:
                applyPotPurchase(count);
                break;
            case PLANT_FOOD:
                applyPlantFoodPurchase(count);
                break;
            case SEED_PACKET_RANDOM:
                // Each purchase grants RANDOM_SEED_PACKET_AMOUNT packets
                // for one randomly chosen unlocked plant.
                String randomPlant = pickRandomUnlockedPlant();
                if (randomPlant != null) {
                    applySeedPacketPurchase(randomPlant, RANDOM_SEED_PACKET_AMOUNT * count);
                }
                break;
            case SEED_PACKET_CHOSEN:
                // Each purchase grants CHOSEN_SEED_PACKET_AMOUNT packets
                // for the specified plant.
                applySeedPacketPurchase(plantType, CHOSEN_SEED_PACKET_AMOUNT * count);
                break;
            case CURRENCY_CONVERSION:
                applyCurrencyConversion(count);
                break;
        }
    }

    /**
     * Unlocks {@code count} greenhouse pot slots for the player.
     * Used when buying the POT item.
     *
     * @param count the number of pots to unlock
     */
    private void applyPotPurchase(int count) {
        customer.setUnlockedPots(customer.getUnlockedPots() + count);
    }

    /**
     * Adds {@code count} plant food units to the player's profile.
     * Used when buying the PLANT_FOOD item.
     *
     * @param count the number of plant food units to add
     */
    private void applyPlantFoodPurchase(int count) {
        customer.setPlantFoodCount(customer.getPlantFoodCount() + count);
    }

    /**
     * Adds seed packets for a specific plant to the player's profile.
     * Used for both RANDOM and CHOSEN seed packet purchases.
     *
     * @param plantName   the target plant name
     * @param packetCount the number of seed packets to add
     */
    private void applySeedPacketPurchase(String plantName, int packetCount) {
        if (plantName == null || customer.getSeedPackets() == null) {
            return;
        }
        Integer current = customer.getSeedPackets().get(plantName);
        int newCount = (current == null ? 0 : current) + packetCount;
        customer.getSeedPackets().put(plantName, newCount);
    }

    /**
     * Converts gems to coins at the fixed rate (5 gems → 500 coins per unit).
     * Used when buying the CURRENCY_CONVERSION item.
     *
     * @param count the number of conversion units
     */
    private void applyCurrencyConversion(int count) {
        customer.setCoins(customer.getCoins() + CURRENCY_CONVERSION_COINS * count);
    }

    /**
     * Finds a permanent ShopItem by its ID.
     *
     * @param itemId the ID to search for
     * @return the matching ShopItem, or null if not found
     */
    public ShopItem findItemById(int itemId) {
        for (ShopItem item : permanentItems) {
            if (item.getId() == itemId) {
                return item;
            }
        }
        return null;
    }

    /**
     * Picks a random plant from the player's unlocked plants.
     * Used when generating the daily offer and when applying
     * the RANDOM seed packet purchase.
     *
     * @return the name of a random unlocked plant, or null if the player has none
     */
    private String pickRandomUnlockedPlant() {
        Set<String> unlocked = customer.getUnlockedPlants();
        if (unlocked == null || unlocked.isEmpty()) {
            return null;
        }
        int idx = random.nextInt(unlocked.size());
        int i = 0;
        for (String plant : unlocked) {
            if (i == idx) {
                return plant;
            }
            i++;
        }
        return null;
    }

    /**
     * Returns all purchasable items (both permanent and daily).
     *
     * @return combined list of all shop items
     */
    public List<ShopItem> getAllPurchasableItems() {
        // TODO
        return null;
    }

    /**
     * Checks whether the daily offer has been purchased today.
     *
     * @return true if today's offer is already purchased (or no offer exists)
     */
    public boolean isDailyOfferPurchased() {
        return dailyOffer != null && dailyOffer.isPurchased();
    }

    /**
     * Checks whether the daily offer needs to be refreshed
     * (i.e., the system date is past the last refresh date).
     *
     * @return true if a refresh is needed
     */
    public boolean needsRefresh() {
        LocalDate today = LocalDate.now();
        return lastRefreshDate == null || !today.equals(lastRefreshDate);
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

    public User getCustomer() {
        return customer;
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

    public void setCustomer(User customer) {
        this.customer = customer;
    }
}
