package model.shop;

import model.enums.CurrencyType;
import model.enums.ShopCategory;
import model.enums.ShopItemType;

/**
 * Represents an item available for purchase in the shop.
 * Each item has a type, category (permanent or daily),
 * price, currency, and quantity limits.
 */
public class ShopItem {
    private int id;
    private ShopItemType itemType;
    private ShopCategory category;
    private int price;
    private CurrencyType currency;       // "coin" or "gem"
    private int maxQuantity;         // 20 for pots, 3 for plant foods
    private String targetPlantType;
    private String description;

    public ShopItem(int id, ShopItemType itemType, ShopCategory category,
                    int price, CurrencyType currency, int maxQuantity,
                    String targetPlantType, String description) {
        this.id = id;
        this.itemType = itemType;
        this.category = category;
        this.price = price;
        this.currency = currency;
        this.maxQuantity = maxQuantity;
        this.targetPlantType = targetPlantType;
        this.description = description;
    }

    // --- Getters ---

    public int getId() {
        return id;
    }

    public ShopItemType getItemType() {
        return itemType;
    }

    public ShopCategory getCategory() {
        return category;
    }

    public int getPrice() {
        return price;
    }

    public CurrencyType getCurrency() {
        return currency;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    public String getTargetPlantType() {
        return targetPlantType;
    }

    public String getDescription() {
        return description;
    }

    // --- Setters ---

    public void setId(int id) {
        this.id = id;
    }

    public void setItemType(ShopItemType itemType) {
        this.itemType = itemType;
    }

    public void setCategory(ShopCategory category) {
        this.category = category;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setCurrency(CurrencyType currency) {
        this.currency = currency;
    }

    public void setMaxQuantity(int maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public void setTargetPlantType(String targetPlantType) {
        this.targetPlantType = targetPlantType;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
