package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.greenhouse.Greenhouse;
import model.greenhouse.GreenhouseProduce;
import model.user.User;
import model.user.persistance.UserRepository;

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
            // Persist any in-flight pot mutations before leaving.
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
        if (greenhouse.getPot(x, y) == null) {
            return CommandResult.error("Invalid position: (" + x + "," + y
                    + "). x must be 1..5, y must be 1..4.");
        }
        String planted = greenhouse.plantPot(x, y);
        if (planted == null) {
            return CommandResult.error("Cannot plant here: pot is locked or already occupied.");
        }
        saveGreenhouse();
        return CommandResult.success("Planted " + planted + " in pot (" + x + "," + y + ").");
    }

    public CommandResult<Void> collect(int x, int y) {
        Greenhouse greenhouse = currentGreenhouse();
        if (greenhouse.getPot(x, y) == null) {
            return CommandResult.error("Invalid position: (" + x + "," + y
                    + "). x must be 1..5, y must be 1..4.");
        }
        GreenhouseProduce produce = greenhouse.collect(x, y);
        if (produce == null) {
            return CommandResult.error("Pot is not ready for harvest, or is empty/locked.");
        }
        String message = applyProduceToUser(produce);
        saveGreenhouse();
        return CommandResult.success(message);
    }

    /**
     * Accelerates growth of the plant at {@code (x, y)} by spending
     * {@code ceil(remainingHours)} gems.
     */
    public CommandResult<Void> grow(int x, int y) {
        Greenhouse greenhouse = currentGreenhouse();
        if (greenhouse.getPot(x, y) == null) {
            return CommandResult.error("Invalid position: (" + x + "," + y
                    + "). x must be 1..5, y must be 1..4.");
        }
        int cost = greenhouse.growCost(x, y);
        if (cost <= 0) {
            // Either nothing is growing here, or it's already ready.
            return CommandResult.error("No growing plant here, or plant is already ready.");
        }
        User user = App.getInstance().getCurrentUser();
        if (user.getGems() < cost) {
            return CommandResult.error("Need " + cost + " gems, have " + user.getGems() + ".");
        }
        if (!greenhouse.commitGrow(x, y)) {
            return CommandResult.error("Failed to accelerate growth.");
        }
        user.setGems(user.getGems() - cost);
        saveGreenhouse();
        return CommandResult.success("Accelerated growth for " + cost + " gem(s).");
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    /**
     * Returns the singleton greenhouse instance bound to the current user.
     * The greenhouse reads its initial state from the user's persisted
     * fields on construction.
     */
    private Greenhouse currentGreenhouse() {
        return Greenhouse.getInstance(App.getInstance().getCurrentUser());
    }

    /**
     * Flushes the greenhouse's in-memory state back to the user's
     * persisted fields and saves the user. Called after every mutation
     * so changes survive a session restart.
     */
    private void saveGreenhouse() {
        Greenhouse greenhouse = currentGreenhouse();
        greenhouse.save();
        UserRepository repo = App.getInstance().getUserRepository();
        repo.save(App.getInstance().getCurrentUser());
    }

    /**
     * Applies the produce of a harvest to the current user and returns a
     * human-readable description of what happened.
     *
     * @param produce the harvest produce
     * @return a message describing the reward
     */
    private String applyProduceToUser(GreenhouseProduce produce) {
        User user = App.getInstance().getCurrentUser();
        if (produce.isCoinReward()) {
            user.setCoins(user.getCoins() + produce.getCoinAmount());
            return "Harvested marigold for " + produce.getCoinAmount() + " coins.";
        }
        if (produce.isBoost()) {
            // Store the boost on the user's plantBoosts map.
            if (user.getPlantBoosts() != null) {
                user.getPlantBoosts().put(produce.getBoostPlantType(), true);
            }
            return "Harvested " + produce.getBoostPlantType()
                    + " — a one-shot boost has been stored for that plant.";
        }
        return "Harvested, but no extra boost was granted (a boost was already stored).";
    }
}
