package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.enums.PurchaseResult;
import model.enums.ShopItemType;
import model.greenhouse.Greenhouse;
import model.shop.Shop;
import model.shop.ShopItem;
import model.user.User;
import model.user.persistance.RemoteUserRepository;
import model.user.persistance.UserSync;

import java.util.List;

public class ShopMenuController extends AppMenuController {
    private static ShopMenuController instance = null;

    private ShopMenuController() {}

    public static ShopMenuController getInstance() {
        if (instance == null) instance = new ShopMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return CommandResult.error("No menus reachable from shop.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.GREENHOUSE);
        return CommandResult.success("Returned to greenhouse.");
    }

    public CommandResult<String> shopList() {
        Shop shop = buildShop();
        List<ShopItem> items = shop.getPermanentItems();
        StringBuilder sb = new StringBuilder("── Shop ──\n");
        for (ShopItem item : items) {
            String cost = item.getPrice() + " " + item.getCurrency() + "(s)";
            sb.append("  ID ").append(item.getId()).append(": ")
                .append(item.getDescription()).append(" — ").append(cost).append("\n");
        }
        return CommandResult.successWithData(sb.toString(), sb.toString());
    }

    public CommandResult<String> shopDaily() {
        Shop shop = buildShop();
        ensureDailyOffer(shop);
        if (shop.getDailyOffer() == null) {
            String msg = "No daily offer today.";
            return CommandResult.successWithData(msg, msg);
        }
        String msg = "── Daily Offer ──\n"
            + "  " + shop.getDailyOffer().getItem().getDescription() + "\n"
            + "  Price: " + shop.getDailyOffer().getDiscountedPrice() + " coins\n"
            + (shop.getDailyOffer().isPurchased() ? "  [Already purchased today]" : "  Use 'shop buy -i 6 -n 1'");
        return CommandResult.successWithData(msg, msg);
    }

    /**
     * Rebuilds today's daily offer from the user save (or rolls a new one once),
     * then flushes so the plant identity survives restarts.
     */
    public Shop ensureDailyOffer() {
        Shop shop = buildShop();
        ensureDailyOffer(shop);
        return shop;
    }

    private void ensureDailyOffer(Shop shop) {
        if (App.getInstance().getUserRepository() instanceof RemoteUserRepository) {
            UserSync.syncDailyOfferFromServer();
            User user = App.getInstance().getCurrentUser();
            shop.refreshDailyOffer(
                    user != null ? user.getDailyOfferPlant() : null,
                    user != null ? user.getDailyOfferDate() : null);
        } else {
            shop.refreshDailyOffer();
            UserSync.flushIfLocal();
        }
    }

    public CommandResult<Void> shopBuy(int itemId, int count, String plantType) {
        if (App.getInstance().getUserRepository() instanceof RemoteUserRepository) {
            if (!UserSync.purchaseShopItem(itemId, count, plantType)) {
                return CommandResult.error("Purchase failed (check funds / capacity / server).");
            }
            Shop shop = buildShop();
            if (itemId == Shop.ITEM_ID_DAILY_OFFER && shop.getDailyOffer() != null) {
                shop.getDailyOffer().setPurchased(true);
            }
            ShopItem item = shop.findItemById(itemId);
            String message = "Purchase successful!";
            if (item != null && item.getItemType() == ShopItemType.POT) {
                Greenhouse greenhouse = Greenhouse.getInstance(App.getInstance().getCurrentUser());
                int unlocked = greenhouse.getUnlockedPotCount();
                message = "Bought pot. You now have " + unlocked + "/" + Greenhouse.TOTAL_POTS + " pots.";
            }
            return CommandResult.success(message);
        }

        Shop shop = buildShop();
        PurchaseResult result = shop.buy(itemId, count, plantType);
        if (result != PurchaseResult.SUCCESS) {
            return CommandResult.error(errorMessage(result));
        }
        UserSync.flushIfLocal();

        // For pots, enrich the message with unlock progress.
        ShopItem item = shop.findItemById(itemId);
        String message = "Purchase successful!";
        if (item != null && item.getItemType() == ShopItemType.SEED_PACKET_RANDOM
            && shop.getLastRandomSeedPlant() != null) {
            message = "Purchased " + (Shop.RANDOM_SEED_PACKET_AMOUNT * count)
                + " seed packet(s) for '" + shop.getLastRandomSeedPlant() + "'.";
        }
        if (item != null && item.getItemType() == ShopItemType.POT) {
            Greenhouse greenhouse = Greenhouse.getInstance(App.getInstance().getCurrentUser());
            int[] next = greenhouse.nextPotToUnlock();
            int unlocked = greenhouse.getUnlockedPotCount();
            int spent = item.getPrice() * count;
            if (next == null) {
                message = "Bought pot for " + spent + " coins. All "
                    + Greenhouse.TOTAL_POTS + " pots are now unlocked.";
            } else {
                message = "Bought pot for " + spent + " coins. You now have " + unlocked
                    + "/" + Greenhouse.TOTAL_POTS + " pots. Next to unlock: ("
                    + next[0] + "," + next[1] + ").";
            }
        }
        return CommandResult.success(message);
    }

    /** Maps a purchase result to a user-facing error message. */
    private String errorMessage(PurchaseResult result) {
        switch (result) {
            case INVALID_ITEM:
                return "No shop item with this ID exists.";
            case INVALID_COUNT:
                return "Invalid count for this purchase.";
            case PLANT_TYPE_REQUIRED:
                return "-t <plant_type> is required for chosen seed packets.";
            case PLANT_NOT_UNLOCKED:
                return "This plant is not unlocked yet.";
            case INSUFFICIENT_FUNDS:
                return "Not enough coins/gems for this purchase.";
            case CAPACITY_REACHED:
                return "You have reached the capacity limit for this item.";
            case NO_DAILY_OFFER:
                return "No daily offer is available today.";
            case ALREADY_PURCHASED:
                return "You have already purchased today's daily offer.";
            default:
                return "Purchase failed.";
        }
    }

    private Shop buildShop() {
        return Shop.getInstance(App.getInstance().getCurrentUser());
    }
}
