package view;

import controller.ShopMenuController;
import controller.result.CommandResult;
import model.command.ShopMenuCommand;

public class ShopMenuView extends AppMenuView {
    private static ShopMenuView instance = null;

    public static ShopMenuView getInstance() {
        if (instance == null) instance = new ShopMenuView();
        return instance;
    }

    private final ShopMenuController controller = ShopMenuController.getInstance();

    @Override
    public void processInput(String input) {
        if (ShopMenuCommand.SHOP_LIST.matches(input)) {
            shopList();
        } else if (ShopMenuCommand.SHOP_DAILY.matches(input)) {
            shopDaily();
        } else if (ShopMenuCommand.SHOP_BUY.matches(input)) {
            try {
                int itemId = Integer.parseInt(ShopMenuCommand.SHOP_BUY.getParameter("itemId"));
                int count = Integer.parseInt(ShopMenuCommand.SHOP_BUY.getParameter("count"));
                String plantType = ShopMenuCommand.SHOP_BUY.getParameter("plantType");
                shopBuy(itemId, count, plantType);
            } catch (NumberFormatException e) {
                // Guards against out-of-range numbers crashing the app.
                displayError("Invalid number in command.");
            }
        } else {
            displayError("Usage:");
            displayError("  shop list");
            displayError("  shop daily");
            displayError("  shop buy -i <item_id> -n <count> [-t <plant_type>]");
        }
    }

    public void shopList() {
        CommandResult<String> result = controller.shopList();
        if (result.isSuccess()) {
            displayMessage(result.getMessage());
        } else {
            displayError(result.getMessage());
        }
    }

    public void shopDaily() {
        CommandResult<String> result = controller.shopDaily();
        if (result.isSuccess()) {
            displayMessage(result.getMessage());
        } else {
            displayError(result.getMessage());
        }
    }

    public void shopBuy(int itemId, int count, String plantType) {
        CommandResult<Void> result = controller.shopBuy(itemId, count, plantType);
        displayCommandResult(result);
    }
}
