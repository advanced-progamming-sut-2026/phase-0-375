package model.shop;

import model.enums.CurrencyType;
import model.enums.PurchaseResult;
import model.enums.ShopCategory;
import model.enums.ShopItemType;
import model.greenhouse.Greenhouse;
import model.user.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * The shop where players spend coins and gems.
 * Two sections: permanent items (always available) and a daily offer
 * (refreshed at 00:00, purchasable once per day).
 */
public class Shop {
    private static Shop instance = null;

    private List<ShopItem> permanentItems;
    private DailyOffer dailyOffer;
    private LocalDate lastRefreshDate;
    private User customer;
    private final Random random = new Random();

    public static final int MAX_POTS = Greenhouse.TOTAL_POTS;
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

    private Shop(User customer) {
        this.customer = customer;
        this.permanentItems = new ArrayList<>();
        this.dailyOffer = null;
        this.lastRefreshDate = null;
        initializePermanentItems();
    }

    /** Singleton accessor; switching users resets the in-memory daily offer. */
    public static Shop getInstance(User customer) {
        if (instance == null) {
            instance = new Shop(customer);
        } else {
            instance.setCustomer(customer);
        }
        return instance;
    }

    private void initializePermanentItems() {
        permanentItems.add(new ShopItem(
                ITEM_ID_POT,
                ShopItemType.POT,
                ShopCategory.PERMANENT,
                2000,
                CurrencyType.COIN,
                MAX_POTS,
                null, "Opens a greenhouse pot slot (max " + Greenhouse.TOTAL_POTS + " pots)"
        ));
        permanentItems.add(new ShopItem(
                ITEM_ID_PLANT_FOOD,
                ShopItemType.PLANT_FOOD,
                ShopCategory.PERMANENT,
                3,
                CurrencyType.GEM,
                MAX_PLANT_FOOD,
                null, "Plant Food usable at level start (max 3 stored)"
        ));
        permanentItems.add(new ShopItem(
                ITEM_ID_SEED_RANDOM,
                ShopItemType.SEED_PACKET_RANDOM,
                ShopCategory.PERMANENT,
                1000,
                CurrencyType.COIN,
                Integer.MAX_VALUE,
                null, "5 seed packets for a random unlocked plant"
        ));
        permanentItems.add(new ShopItem(
                ITEM_ID_SEED_CHOSEN,
                ShopItemType.SEED_PACKET_CHOSEN,
                ShopCategory.PERMANENT,
                5,
                CurrencyType.GEM,
                Integer.MAX_VALUE,
                null, "10 seed packets for a chosen unlocked plant"
        ));
        permanentItems.add(new ShopItem(
                ITEM_ID_CURRENCY_CONVERSION,
                ShopItemType.CURRENCY_CONVERSION,
                ShopCategory.PERMANENT,
                5,
                CurrencyType.GEM,
                Integer.MAX_VALUE,
                null, "Convert 5 gems to 500 coins"
        ));
    }

    /**
     * Refreshes the daily offer if the date has changed.
     * Reuses today's persisted offer (plant + date) so it survives restarts.
     */
    public void refreshDailyOffer() {
        LocalDate today = LocalDate.now();
        if (dailyOffer != null && today.equals(lastRefreshDate)) {
            return;
        }
        if (customer == null) {
            return;
        }

        // Reuse today's saved offer if it exists and is still unlocked.
        String plant = null;
        if (today.toString().equals(customer.getDailyOfferDate())) {
            plant = resolveUnlockedPlantName(customer.getDailyOfferPlant());
        }
        if (plant == null) {
            plant = pickRandomUnlockedPlant();
        }
        if (plant == null) {
            // No unlocked plants — no daily offer possible.
            dailyOffer = null;
            lastRefreshDate = today;
            return;
        }

        ShopItem offerItem = new ShopItem(
                ITEM_ID_DAILY_OFFER,
                ShopItemType.SEED_PACKET_CHOSEN,
                ShopCategory.DAILY,
                DAILY_OFFER_BASE_PRICE,
                CurrencyType.COIN,
                Integer.MAX_VALUE,
                plant,
                DAILY_OFFER_PACKET_AMOUNT + " seed packets for " + plant + " (20% off)"
        );
        dailyOffer = new DailyOffer(offerItem, DAILY_OFFER_BASE_PRICE, today);
        lastRefreshDate = today;

        // Persist the offer's identity (plant + date).
        customer.setDailyOfferPlant(plant);
        customer.setDailyOfferDate(today.toString());

        // Restore the purchased flag from persistence.
        if (customer.getPurchasedDailyDeals() != null
                && Boolean.TRUE.equals(customer.getPurchasedDailyDeals().get(today.toString()))) {
            dailyOffer.setPurchased(true);
        }
    }

    /** Handles {@code shop buy -i <item_id> -n <count> [-t <plant_type>]}. */
    public PurchaseResult buy(int itemId, int count, String plantType) {
        if (customer == null) {
            return PurchaseResult.INVALID_ITEM;
        }
        if (count <= 0) {
            return PurchaseResult.INVALID_COUNT;
        }

        if (itemId == ITEM_ID_DAILY_OFFER) {
            // Daily offer is a single bundle — only count 1 makes sense.
            if (count != 1) {
                return PurchaseResult.INVALID_COUNT;
            }
            return buyDailyOffer();
        }

        ShopItem item = findItemById(itemId);
        if (item == null) {
            return PurchaseResult.INVALID_ITEM;
        }

        if (item.getItemType() == ShopItemType.SEED_PACKET_CHOSEN) {
            if (plantType == null || plantType.isEmpty()) {
                return PurchaseResult.PLANT_TYPE_REQUIRED;
            }
            // accept the plant name case-insensitively
            String canonical = resolveUnlockedPlantName(plantType);
            if (canonical == null) {
                return PurchaseResult.PLANT_NOT_UNLOCKED;
            }
            plantType = canonical;
        }

        if (!canAfford(item, count)) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }
        if (!hasCapacity(item.getItemType(), count)) {
            return PurchaseResult.CAPACITY_REACHED;
        }

        deductCurrency(item, count);
        applyItemEffect(item, count, plantType);
        return PurchaseResult.SUCCESS;
    }

    /** Buys today's daily offer at the discounted price (once per day). */
    public PurchaseResult buyDailyOffer() {
        if (customer == null) {
            return PurchaseResult.INVALID_ITEM;
        }
        if (needsRefresh()) {
            refreshDailyOffer();
        }
        if (dailyOffer == null) {
            return PurchaseResult.NO_DAILY_OFFER;
        }
        if (dailyOffer.isPurchased()) {
            return PurchaseResult.ALREADY_PURCHASED;
        }

        int cost = dailyOffer.getDiscountedPrice();
        if (customer.getCoins() < cost) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        customer.setCoins(customer.getCoins() - cost);
        applySeedPacketPurchase(dailyOffer.getItem().getTargetPlantType(), DAILY_OFFER_PACKET_AMOUNT);

        dailyOffer.setPurchased(true);
        if (customer.getPurchasedDailyDeals() == null) {
            customer.setPurchasedDailyDeals(new HashMap<>());
        }
        customer.getPurchasedDailyDeals().put(dailyOffer.getOfferDate().toString(), true);
        return PurchaseResult.SUCCESS;
    }

    /** Overflow-safe affordability check (price × count computed as long). */
    public boolean canAfford(ShopItem item, int count) {
        if (item == null || count <= 0 || customer == null) {
            return false;
        }
        long totalCost = (long) item.getPrice() * count;
        if (item.getCurrency() == CurrencyType.COIN) {
            return customer.getCoins() >= totalCost;
        } else if (item.getCurrency() == CurrencyType.GEM) {
            return customer.getGems() >= totalCost;
        }
        return false;
    }

    /** Capacity check; pots use the greenhouse as the single source of truth. */
    public boolean hasCapacity(ShopItemType itemType, int count) {
        if (count <= 0 || customer == null) {
            return false;
        }
        switch (itemType) {
            case POT:
                int currentPots = Greenhouse.getInstance(customer).getUnlockedPotCount();
                return (long) currentPots + count <= MAX_POTS;
            case PLANT_FOOD:
                int currentFood = customer.getPlantFoodCount();
                return (long) currentFood + count <= MAX_PLANT_FOOD;
            default:
                // Seed packets and currency conversion have no cap.
                return true;
        }
    }

    private void deductCurrency(ShopItem item, int count) {
        // Safe cast: canAfford already validated the total with long math.
        int totalCost = (int) ((long) item.getPrice() * count);
        if (item.getCurrency() == CurrencyType.COIN) {
            customer.setCoins(customer.getCoins() - totalCost);
        } else if (item.getCurrency() == CurrencyType.GEM) {
            customer.setGems(customer.getGems() - totalCost);
        }
    }

    // last plant picked by a random seed-packet purchase (for UI messages)
    private String lastRandomSeedPlant;

    public String getLastRandomSeedPlant() {
        return lastRandomSeedPlant;
    }

    private void applyItemEffect(ShopItem item, int count, String plantType) {
        switch (item.getItemType()) {
            case POT:
                applyPotPurchase(count);
                break;
            case PLANT_FOOD:
                applyPlantFoodPurchase(count);
                break;
            case SEED_PACKET_RANDOM:
                String randomPlant = pickRandomUnlockedPlant();
                lastRandomSeedPlant = randomPlant;
                if (randomPlant != null) {
                    applySeedPacketPurchase(randomPlant, RANDOM_SEED_PACKET_AMOUNT * count);
                }
                break;
            case SEED_PACKET_CHOSEN:
                applySeedPacketPurchase(plantType, CHOSEN_SEED_PACKET_AMOUNT * count);
                break;
            case CURRENCY_CONVERSION:
                applyCurrencyConversion(count);
                break;
        }
    }

    private void applyPotPurchase(int count) {
        // Capacity was validated against the greenhouse, so unlocks cannot fail here.
        Greenhouse greenhouse = Greenhouse.getInstance(customer);
        for (int i = 0; i < count; i++) {
            if (greenhouse.unlockNextPot() == null) {
                break;
            }
        }
        greenhouse.save();
    }

    private void applyPlantFoodPurchase(int count) {
        customer.setPlantFoodCount(customer.getPlantFoodCount() + count);
    }

    private void applySeedPacketPurchase(String plantName, int packetCount) {
        if (plantName == null) {
            return;
        }
        if (customer.getSeedPackets() == null) {
            customer.setSeedPackets(new HashMap<>());
        }
        Integer current = customer.getSeedPackets().get(plantName);
        int newCount = (current == null ? 0 : current) + packetCount;
        customer.getSeedPackets().put(plantName, newCount);
    }

    private void applyCurrencyConversion(int count) {
        customer.setCoins(customer.getCoins() + CURRENCY_CONVERSION_COINS * count);
    }

    /** Finds a permanent ShopItem by ID, or null if not found. */
    public ShopItem findItemById(int itemId) {
        for (ShopItem item : permanentItems) {
            if (item.getId() == itemId) {
                return item;
            }
        }
        return null;
    }

    /** Finds the canonical unlocked plant name, ignoring case. */
    private String resolveUnlockedPlantName(String plantName) {
        Set<String> unlocked = customer != null ? customer.getUnlockedPlants() : null;
        if (plantName == null || unlocked == null) {
            return null;
        }
        for (String name : unlocked) {
            if (name != null && name.equalsIgnoreCase(plantName.trim())) {
                return name;
            }
        }
        return null;
    }

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

    /** Returns all purchasable items (permanent + today's unpurchased daily offer). */
    public List<ShopItem> getAllPurchasableItems() {
        List<ShopItem> all = new ArrayList<>(permanentItems);
        if (needsRefresh()) {
            refreshDailyOffer();
        }
        if (dailyOffer != null && !dailyOffer.isPurchased() && !dailyOffer.isExpired()) {
            all.add(dailyOffer.getItem());
        }
        return all;
    }

    public boolean isDailyOfferPurchased() {
        return dailyOffer != null && dailyOffer.isPurchased();
    }

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

    /** Switching to a different user resets the in-memory daily-offer state. */
    public void setCustomer(User customer) {
        boolean sameUser = this.customer != null && customer != null
                && Objects.equals(this.customer.getUsername(), customer.getUsername());
        this.customer = customer;
        if (!sameUser) {
            this.dailyOffer = null;
            this.lastRefreshDate = null;
        }
    }
}
