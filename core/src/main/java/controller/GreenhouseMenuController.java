package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.enums.PotState;
import model.greenhouse.Greenhouse;
import model.greenhouse.GreenhouseProduce;
import model.greenhouse.Pot;
import model.shop.Shop;
import model.user.User;
import model.user.persistance.UserRepository;

import java.util.HashMap;
import java.util.Map;

public class GreenhouseMenuController extends AppMenuController {
    private static GreenhouseMenuController instance = null;

    private GreenhouseMenuController() {}

    public static GreenhouseMenuController getInstance() {
        if (instance == null) instance = new GreenhouseMenuController();
        return instance;
    }

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        if (menuName.equalsIgnoreCase("shop")) {
            saveGreenhouse();
            App.getInstance().setCurrentMenu(MenuType.SHOP);
            return CommandResult.success("Entered shop.");
        }
        return CommandResult.error("Cannot go to '" + menuName + "' from greenhouse.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        saveGreenhouse();
        App.getInstance().setCurrentMenu(MenuType.GAME);
        return CommandResult.success("Returned to game menu.");
    }

    public CommandResult<String> showGreenhouse() {
        Greenhouse greenhouse = currentGreenhouse();
        String grid = greenhouse.renderGrid();
        int growing = greenhouse.getProducingCount();
        int ready = greenhouse.getReadyCount();
        int unlocked = greenhouse.getUnlockedPotCount();
        int[] next = greenhouse.nextPotToUnlock();
        String nextHint = next == null
            ? "All pots are unlocked."
            : "Next pot to unlock: (" + next[0] + "," + next[1]
            + ") — buy a 'Pot' from the shop.";
        String summary = "\nUnlocked: " + unlocked + "/" + Greenhouse.TOTAL_POTS
            + "  |  Growing: " + growing + "  |  Ready: " + ready
            + "  |  " + nextHint;
        return CommandResult.successWithData(grid + summary, grid + summary);
    }

    public CommandResult<Void> plantPot(int x, int y) {
        Greenhouse greenhouse = currentGreenhouse();
        Pot pot = greenhouse.getPot(x, y);
        if (pot == null) {
            return CommandResult.error("Invalid position: (" + x + "," + y
                + "). " + boundsHint());
        }
        // distinct errors per spec
        if (pot.getState() == PotState.LOCKED) {
            return CommandResult.error("Pot (" + x + "," + y
                + ") is locked. Buy a 'Pot' from the shop to unlock it.");
        }
        pot.isReady(); // refresh lazy GROWING -> READY
        if (pot.getState() != PotState.EMPTY) {
            return CommandResult.error("Pot (" + x + "," + y + ") is already occupied.");
        }
        String planted = greenhouse.plantPot(x, y);
        if (planted == null) {
            return CommandResult.error("Cannot plant here.");
        }
        saveGreenhouse();
        return CommandResult.success("Planted " + planted + " in pot (" + x + "," + y + ").");
    }

    public CommandResult<Void> collect(int x, int y) {
        Greenhouse greenhouse = currentGreenhouse();
        if (greenhouse.getPot(x, y) == null) {
            return CommandResult.error("Invalid position: (" + x + "," + y
                + "). " + boundsHint());
        }
        GreenhouseProduce produce = greenhouse.collect(x, y);
        if (produce == null) {
            return CommandResult.error("Pot is not ready for harvest, or is empty/locked.");
        }
        String message = applyProduceToUser(produce);
        saveGreenhouse();
        return CommandResult.success(message);
    }

    /** Accelerates growth at (x, y) for ceil(remainingHours) gems. */
    public CommandResult<Void> grow(int x, int y) {
        Greenhouse greenhouse = currentGreenhouse();
        Pot pot = greenhouse.getPot(x, y);
        if (pot == null) {
            return CommandResult.error("Invalid position: (" + x + "," + y
                + "). " + boundsHint());
        }
        pot.isReady(); // refresh lazy state
        // distinct errors: already ready vs nothing growing
        if (pot.getState() == PotState.READY) {
            return CommandResult.error("Plant at (" + x + "," + y
                + ") is already fully grown. Use collect instead.");
        }
        if (pot.getState() != PotState.GROWING) {
            return CommandResult.error("No growing plant at (" + x + "," + y + ").");
        }
        int cost = greenhouse.growCost(x, y);
        if (cost <= 0) {
            return CommandResult.error("Plant at (" + x + "," + y
                + ") is already fully grown. Use collect instead.");
        }
        User user = App.getInstance().getCurrentUser();
        if (user.getGems() < cost) {
            return CommandResult.error("Need " + cost + " gems, have " + user.getGems() + ".");
        }
        if (!greenhouse.commitGrow(x, y)) {
            return CommandResult.error("Failed to accelerate growth.");
        }
        if (!model.user.persistance.UserSync.spendGems(cost)) {
            return CommandResult.error("Need " + cost + " gems.");
        }
        saveGreenhouse();
        return CommandResult.success("Accelerated growth for " + cost + " gem(s).");
    }

    /**
     * Buys one greenhouse pot slot using the same shop item as the TUI shop
     * ({@link Shop#ITEM_ID_POT}, 2000 coins). Unlocks the next locked pot.
     */
    public CommandResult<Void> buyPot() {
        return ShopMenuController.getInstance().shopBuy(Shop.ITEM_ID_POT, 1, null);
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private Greenhouse currentGreenhouse() {
        return Greenhouse.getInstance(App.getInstance().getCurrentUser());
    }

    /** Flushes greenhouse state to the user and persists it. */
    private void saveGreenhouse() {
        Greenhouse greenhouse = currentGreenhouse();
        greenhouse.save();
        model.user.persistance.UserSync.flushIfLocal();
        // Remote: re-push pot plantings from the current user snapshot.
        User user = App.getInstance().getCurrentUser();
        var repo = App.getInstance().getUserRepository();
        if (user != null && repo instanceof model.user.persistance.RemoteUserRepository
                && user.getGreenhousePots() != null) {
            for (var e : user.getGreenhousePots().entrySet()) {
                String[] xy = e.getKey().split(",");
                if (xy.length != 2) continue;
                int px = Integer.parseInt(xy[0].trim());
                int py = Integer.parseInt(xy[1].trim());
                long ts = user.getGreenhousePlantTimestamps() != null
                        ? user.getGreenhousePlantTimestamps().getOrDefault(e.getKey(), System.currentTimeMillis())
                        : System.currentTimeMillis();
                repo.plantInGreenhouse(user.getUsername(), px, py, e.getValue(), ts);
            }
        }
    }

    private String applyProduceToUser(GreenhouseProduce produce) {
        User user = App.getInstance().getCurrentUser();
        if (produce.isCoinReward()) {
            model.user.persistance.UserSync.addCoins(produce.getCoinAmount());
            return "Harvested marigold for " + produce.getCoinAmount() + " coins.";
        }
        if (produce.isBoost()) {
            var repo = App.getInstance().getUserRepository();
            if (user != null && repo != null) {
                repo.storePlantBoost(user.getUsername(), produce.getBoostPlantType());
            }
            return "Harvested " + produce.getBoostPlantType()
                + " — a one-shot boost has been stored for that plant.";
        }
        return "Harvested, but no extra boost was granted (a boost was already stored).";
    }

    private static String boundsHint() {
        return "x must be 1.." + Greenhouse.COLS + ", y must be 1.." + Greenhouse.ROWS + ".";
    }
}
