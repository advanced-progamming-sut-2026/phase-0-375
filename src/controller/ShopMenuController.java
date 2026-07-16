package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.enums.ShopItemType;
import model.greenhouse.Greenhouse;
import model.shop.Shop;
import model.shop.ShopItem;

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
        shop.refreshDailyOffer();
        if (shop.getDailyOffer() == null) {
            return CommandResult.successWithData("No daily offer today.", "None");
        }
        String msg = "── Daily Offer ──\n"
                + "  " + shop.getDailyOffer().getItem().getDescription() + "\n"
                + "  Price: " + shop.getDailyOffer().getDiscountedPrice() + " coins\n"
                + (shop.getDailyOffer().isPurchased() ? "  [Already purchased today]" : "  Use 'shop buy -i 6 -n 1'");
        return CommandResult.successWithData(msg, msg);
    }

    /**
     * TODO: Once Shop.buy() is implemented, this will process purchases.
     */
    public CommandResult<Void> shopBuy(int itemId, int count, String plantType) {
        Shop shop = buildShop();
        boolean success = shop.buy(itemId, count, plantType);
        if (!success) {
            return CommandResult.error("Purchase failed.");
        }
        App.getInstance().getUserRepository().flush();

        // If the purchased item was a Pot, enrich the success message with
        // the coordinates of the pot(s) that were just unlocked.
        ShopItem item = shop.findItemById(itemId);
        String message = "Purchase successful!";
        if (item != null && item.getItemType() == ShopItemType.POT) {
            Greenhouse greenhouse = Greenhouse.getInstance(App.getInstance().getCurrentUser());
            int[] next = greenhouse.nextPotToUnlock();
            int unlocked = greenhouse.getUnlockedPotCount();
            if (next == null) {
                message = "All " + Greenhouse.TOTAL_POTS + " pots are now unlocked.";
            } else {
                message = "Unlocked " + count + " pot(s). You now have " + unlocked
                        + "/" + Greenhouse.TOTAL_POTS + " pots. Next to unlock: ("
                        + next[0] + "," + next[1] + ").";
            }
        }
        return CommandResult.success(message);
    }

    private Shop buildShop() {
        return Shop.getInstance(App.getInstance().getCurrentUser());
    }
}
