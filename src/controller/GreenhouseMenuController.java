package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.MenuType;
import model.greenhouse.Greenhouse;
import model.greenhouse.GreenhouseProduce;
import model.user.User;

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
            App.getInstance().setCurrentMenu(MenuType.SHOP);
            return CommandResult.success("Entered shop.");
        }
        return CommandResult.error("Cannot go to '" + menuName + "' from greenhouse.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App.getInstance().setCurrentMenu(MenuType.GAME);
        return CommandResult.success("Returned to game menu.");
    }

    public CommandResult<String> showGreenhouse() {
        Greenhouse greenhouse = new Greenhouse(); // TODO: hydrate from user data
        greenhouse.showGreenhouse();
        return CommandResult.successWithData("Greenhouse displayed.", "Greenhouse displayed.");
    }

    public CommandResult<Void> plantPot(int x, int y) {
        Greenhouse greenhouse = new Greenhouse(); // TODO: hydrate from user data
        greenhouse.plantPot(x, y);
        return CommandResult.success("Planted in pot (" + x + "," + y + ").");
    }

    /**
     * TODO: Once Greenhouse.collect() is implemented, apply the produce effect to the user.
     */
    public CommandResult<Void> collect(int x, int y) {
        Greenhouse greenhouse = new Greenhouse(); // TODO: hydrate from user data
        GreenhouseProduce produce = greenhouse.collect(x, y);
        if (produce == null) {
            return CommandResult.error("Pot is not ready for harvest, or invalid position.");
        }
        return CommandResult.success("Harvested successfully!");
    }

    public CommandResult<Void> grow(int x, int y) {
        Greenhouse greenhouse = new Greenhouse(); // TODO: hydrate from user data
        int cost = greenhouse.grow(x, y);
        if (cost <= 0) {
            return CommandResult.error("No plant to accelerate, or already ready.");
        }
        User user = App.getInstance().getCurrentUser();
        if (user.getGems() < cost) {
            return CommandResult.error("Need " + cost + " gems, have " + user.getGems() + ".");
        }
        user.setGems(user.getGems() - cost);
        App.getInstance().getUserRepository().flush();
        return CommandResult.success("Accelerated growth for " + cost + " gems.");
    }
}
