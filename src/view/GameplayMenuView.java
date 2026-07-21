package view;

import controller.GameplayMenuController;
import controller.result.CommandResult;
import model.command.GameplayMenuCommand;

public class GameplayMenuView extends AppMenuView {
    private static GameplayMenuView instance = null;

    public static GameplayMenuView getInstance() {
        if (instance == null) instance = new GameplayMenuView();
        return instance;
    }

    private final GameplayMenuController controller = GameplayMenuController.getInstance();

    @Override
    public void processInput(String input) {
        if (input == null || input.isBlank()) {
            displayError("Empty command.");
            return;
        }

        if (GameplayMenuCommand.ADVANCE_TIME.matches(input)) {
            int count = Integer.parseInt(GameplayMenuCommand.ADVANCE_TIME.getParameter("count"));
            advanceTime(count);
            return;
        }
        if (GameplayMenuCommand.COLLECT_SUN.matches(input)) {
            int x = Integer.parseInt(GameplayMenuCommand.COLLECT_SUN.getParameter("x"));
            int y = Integer.parseInt(GameplayMenuCommand.COLLECT_SUN.getParameter("y"));
            collectSun(x, y);
            return;
        }
        if (GameplayMenuCommand.SHOW_SUN_AMOUNT.matches(input)) {
            showSunAmount();
            return;
        }
        if (GameplayMenuCommand.CHEAT_ADD_SUNS.matches(input)) {
            int count = Integer.parseInt(GameplayMenuCommand.CHEAT_ADD_SUNS.getParameter("count"));
            cheatAddSuns(count);
            return;
        }
        if (GameplayMenuCommand.PLANT.matches(input)) {
            String type = GameplayMenuCommand.PLANT.getParameter("type");
            int x = Integer.parseInt(GameplayMenuCommand.PLANT.getParameter("x"));
            int y = Integer.parseInt(GameplayMenuCommand.PLANT.getParameter("y"));
            plant(type, x, y);
            return;
        }
        if (GameplayMenuCommand.BREAK_VASE.matches(input)) {
            int x = Integer.parseInt(GameplayMenuCommand.BREAK_VASE.getParameter("x"));
            int y = Integer.parseInt(GameplayMenuCommand.BREAK_VASE.getParameter("y"));
            breakVase(x, y);
            return;
        }
        if (GameplayMenuCommand.CHEAT_REMOVE_COOLDOWN.matches(input)) {
            cheatRemoveCooldown();
            return;
        }
        if (GameplayMenuCommand.PLUCK.matches(input)) {
            int x = Integer.parseInt(GameplayMenuCommand.PLUCK.getParameter("x"));
            int y = Integer.parseInt(GameplayMenuCommand.PLUCK.getParameter("y"));
            pluck(x, y);
            return;
        }
        if (GameplayMenuCommand.FEED.matches(input)) {
            int x = Integer.parseInt(GameplayMenuCommand.FEED.getParameter("x"));
            int y = Integer.parseInt(GameplayMenuCommand.FEED.getParameter("y"));
            feed(x, y);
            return;
        }
        if (GameplayMenuCommand.CHEAT_ADD_PLANT_FOOD.matches(input)) {
            cheatAddPlantFood();
            return;
        }
        if (GameplayMenuCommand.SHOW_MAP.matches(input)) {
            showMap();
            return;
        }
        if (GameplayMenuCommand.SHOW_SCORE.matches(input)) {
            showScore();
            return;
        }
        if (GameplayMenuCommand.SHOW_PLANTS_STATUS.matches(input)) {
            showPlantsStatus();
            return;
        }
        if (GameplayMenuCommand.SHOW_TILE_STATUS.matches(input)) {
            int x = Integer.parseInt(GameplayMenuCommand.SHOW_TILE_STATUS.getParameter("x"));
            int y = Integer.parseInt(GameplayMenuCommand.SHOW_TILE_STATUS.getParameter("y"));
            showTileStatus(x, y);
            return;
        }
        if (GameplayMenuCommand.RELEASE_NUKE.matches(input)) {
            releaseNuke();
            return;
        }
        if (GameplayMenuCommand.ZOMBIES_INFO.matches(input)) {
            zombiesInfo();
            return;
        }
        if (GameplayMenuCommand.START_ZOMBIE_WAVES.matches(input)) {
            startZombieWaves();
            return;
        }
        if (GameplayMenuCommand.CHEAT_SPAWN_ZOMBIE.matches(input)) {
            String type = GameplayMenuCommand.CHEAT_SPAWN_ZOMBIE.getParameter("zombieType");
            int x = Integer.parseInt(GameplayMenuCommand.CHEAT_SPAWN_ZOMBIE.getParameter("x"));
            int y = Integer.parseInt(GameplayMenuCommand.CHEAT_SPAWN_ZOMBIE.getParameter("y"));
            cheatSpawnZombie(type, x, y);
            return;
        }
        if (GameplayMenuCommand.PLACE_ZOMBIE.matches(input)) {
            String type = GameplayMenuCommand.PLACE_ZOMBIE.getParameter("type");
            int x = Integer.parseInt(GameplayMenuCommand.PLACE_ZOMBIE.getParameter("x"));
            int y = Integer.parseInt(GameplayMenuCommand.PLACE_ZOMBIE.getParameter("y"));
            placeZombie(type, x, y);
            return;
        }
        if (GameplayMenuCommand.SWAP_PLANT.matches(input)) {
            int x = Integer.parseInt(GameplayMenuCommand.SWAP_PLANT.getParameter("x"));
            int y = Integer.parseInt(GameplayMenuCommand.SWAP_PLANT.getParameter("y"));
            String direction = GameplayMenuCommand.SWAP_PLANT.getParameter("dir");
            swapPlant(x, y, direction);
            return;
        }
        if (GameplayMenuCommand.UPGRADE_PLANT.matches(input)) {
            String type = GameplayMenuCommand.UPGRADE_PLANT.getParameter("type");
            upgradePlant(type);
            return;
        }
        if (GameplayMenuCommand.SHOW_BEGHOULED_STATUS.matches(input)) {
            showBeghouledStatus();
            return;
        }

        displayError("Unknown gameplay command. Available commands:");
        displayError("  advance time -t <count> ticks");
        displayError("  start zombie waves");
        displayError("  collect sun -l (<x>, <y>)");
        displayError("  show sun amount");
        displayError("  cheat add -n <count> suns");
        displayError("  plant plant -t <type> -l (<x>, <y>)");
        displayError("  break vase -l (<x>, <y>)");
        displayError("  cheat remove-cooldown");
        displayError("  pluck plant -l (<x>, <y>)");
        displayError("  feed plant -l (<x>, <y>)");
        displayError("  cheat add-plant-food");
        displayError("  show map");
        displayError("  show plants status");
        displayError("  show tile status -l (<x>, <y>)");
        displayError("  release the nuke");
        displayError("  zombies info");
        displayError("  cheat spawn-zombie -t <type> -l <x>, <y>");
        displayError("  place zombie -t <type> -l (<x>, <y>)");
        displayError("  swap plant -l (<x>, <y>) -d <up|down|left|right>");
        displayError("  upgrade plant -t <type>");
        displayError("  show beghouled status");
        displayError("  menu exit");
    }

    public void advanceTime(int count) {
        CommandResult<Void> result = controller.advanceTime(count);
        displayCommandResult(result);
    }

    public void breakVase(int x, int y) {
        CommandResult<Void> result = controller.breakVase(x, y);
        displayCommandResult(result);
    }

    public void placeZombie(String type, int x, int y) {
        CommandResult<Void> result = controller.placeZombie(type, x, y);
        displayCommandResult(result);
    }

    public void swapPlant(int x, int y, String direction) {
        CommandResult<Void> result = controller.swapPlant(x, y, direction);
        displayCommandResult(result);
    }

    public void upgradePlant(String type) {
        CommandResult<Void> result = controller.upgradePlant(type);
        displayCommandResult(result);
    }

    public void showBeghouledStatus() {
        CommandResult<Void> result = controller.beghouledStatus();
        displayCommandResult(result);
    }

    public void startZombieWaves() {
        CommandResult<Void> result = controller.startZombieWaves();
        displayCommandResult(result);
    }

    public void collectSun(int x, int y) {
        CommandResult<Void> result = controller.collectSun(x, y);
        displayCommandResult(result);
    }

    public void showScore() {
        CommandResult<Void> result = controller.showScore();
        displayCommandResult(result);
    }

    public void showSunAmount() {
        CommandResult<Integer> result = controller.showSunAmount();
        if (result.isSuccess()) {
            displayMessage(result.getMessage());
        } else {
            displayError(result.getMessage());
        }
    }

    public void cheatAddSuns(int count) {
        CommandResult<Void> result = controller.cheatAddSuns(count);
        displayCommandResult(result);
    }

    public void plant(String type, int x, int y) {
        CommandResult<Void> result = controller.plant(type, x, y);
        displayCommandResult(result);
    }

    public void cheatRemoveCooldown() {
        CommandResult<Void> result = controller.cheatRemoveCooldown();
        displayCommandResult(result);
    }

    public void pluck(int x, int y) {
        CommandResult<Void> result = controller.pluck(x, y);
        displayCommandResult(result);
    }

    public void feed(int x, int y) {
        CommandResult<Void> result = controller.feed(x, y);
        displayCommandResult(result);
    }

    public void cheatAddPlantFood() {
        CommandResult<Void> result = controller.cheatAddPlantFood();
        displayCommandResult(result);
    }

    public void showMap() {
        CommandResult<String> result = controller.showMap();
        if (result.isSuccess()) {
            displayMessage(result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void showPlantsStatus() {
        CommandResult<String> result = controller.showPlantsStatus();
        if (result.isSuccess()) {
            displayMessage(result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void showTileStatus(int x, int y) {
        CommandResult<String> result = controller.showTileStatus(x, y);
        if (result.isSuccess()) {
            displayMessage(result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void releaseNuke() {
        CommandResult<Void> result = controller.releaseNuke();
        displayCommandResult(result);
    }

    public void zombiesInfo() {
        CommandResult<String> result = controller.zombiesInfo();
        if (result.isSuccess()) {
            displayMessage(result.getData());
        } else {
            displayError(result.getMessage());
        }
    }

    public void cheatSpawnZombie(String zombieType, int x, int y) {
        CommandResult<Void> result = controller.cheatSpawnZombie(zombieType, x, y);
        displayCommandResult(result);
    }
}
