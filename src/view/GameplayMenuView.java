package view;

import controller.GameplayMenuController;
import controller.result.CommandResult;
import model.app.App;
import model.command.GameplayMenuCommand;
import java.util.Map;
import java.util.LinkedHashMap;
import static model.command.GameplayMenuCommand.*;
import model.enums.GameState;
import model.game.core.GameModel;
import view.tui.TuiShell;

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
        for (Map.Entry<GameplayMenuCommand, Runnable> entry : handlers.entrySet()) {
            if (entry.getKey().matches(input)) {
                entry.getValue().run();
                return;
            }
        }
        printUnknownCommandHelp();
    }

    /**
     * Command dispatch table (Command pattern)
     */
    private final Map<GameplayMenuCommand, Runnable> handlers = buildHandlers();

    /** Parses a named integer parameter from the command that just matched. */
    private static int intArg(GameplayMenuCommand cmd, String name) {
        return Integer.parseInt(cmd.getParameter(name));
    }

    /** Registers one handler per gameplay command. */
    private Map<GameplayMenuCommand, Runnable> buildHandlers() {
        Map<GameplayMenuCommand, Runnable> map = new LinkedHashMap<>();
        map.put(ADVANCE_TIME, () -> advanceTime(intArg(ADVANCE_TIME, "count")));
        map.put(COLLECT_SUN, () -> collectSun(intArg(COLLECT_SUN, "x"), intArg(COLLECT_SUN, "y")));
        map.put(SHOW_SUN_AMOUNT, this::showSunAmount);
        map.put(CHEAT_ADD_SUNS, () -> cheatAddSuns(intArg(CHEAT_ADD_SUNS, "count")));
        map.put(PLANT, () -> plant(PLANT.getParameter("type"), intArg(PLANT, "x"), intArg(PLANT, "y")));
        map.put(BREAK_VASE, () -> breakVase(intArg(BREAK_VASE, "x"), intArg(BREAK_VASE, "y")));
        map.put(CHEAT_REMOVE_COOLDOWN, this::cheatRemoveCooldown);
        map.put(PLUCK, () -> pluck(intArg(PLUCK, "x"), intArg(PLUCK, "y")));
        map.put(FEED, () -> feed(intArg(FEED, "x"), intArg(FEED, "y")));
        map.put(CHEAT_ADD_PLANT_FOOD, this::cheatAddPlantFood);
        map.put(SHOW_MAP, this::showMap);
        map.put(SHOW_SCORE, this::showScore);
        map.put(SHOW_PLANTS_STATUS, this::showPlantsStatus);
        map.put(SHOW_KILL_STATS, this::showKillStats);
        map.put(SHOW_TILE_STATUS, () -> showTileStatus(intArg(SHOW_TILE_STATUS, "x"), intArg(SHOW_TILE_STATUS, "y")));
        map.put(RELEASE_NUKE, this::releaseNuke);
        map.put(ZOMBIES_INFO, this::zombiesInfo);
        map.put(START_ZOMBIE_WAVES, this::startZombieWaves);
        map.put(CHEAT_SPAWN_ZOMBIE, () -> cheatSpawnZombie(CHEAT_SPAWN_ZOMBIE.getParameter("zombieType"),
                intArg(CHEAT_SPAWN_ZOMBIE, "x"), intArg(CHEAT_SPAWN_ZOMBIE, "y")));
        map.put(PLACE_ZOMBIE, () -> placeZombie(PLACE_ZOMBIE.getParameter("type"),
                intArg(PLACE_ZOMBIE, "x"), intArg(PLACE_ZOMBIE, "y")));
        map.put(SWAP_PLANT, () -> swapPlant(intArg(SWAP_PLANT, "x"), intArg(SWAP_PLANT, "y"),
                SWAP_PLANT.getParameter("dir")));
        map.put(UPGRADE_PLANT, () -> upgradePlant(UPGRADE_PLANT.getParameter("type")));
        map.put(SHOW_BEGHOULED_STATUS, this::showBeghouledStatus);
        return map;
    }

    /** Prints the command reference shown for unrecognized input. */
    private void printUnknownCommandHelp() {
        displayError("Available commands:");
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
        displayError("  show kill stats");
        displayError("  menu exit");
    }

    /** Delay between rendered ticks in TUI mode - faster than the 0.1s sim tick. */
    private static final long TICK_RENDER_MS = 30;

    public void advanceTime(int count) {
        TuiShell shell = TuiShell.getActive();
        if (shell == null) {
            // Plain CLI mode: advance silently and print the final result.
            CommandResult<Void> result = controller.advanceTime(count);
            displayCommandResult(result);
            return;
        }

        // TUI mode: advance one tick at a time and repaint the live map
        // after each tick, so the simulation is visibly animated.
        for (int i = 0; i < count; i++) {
            CommandResult<Void> result = controller.advanceTime(1);
            if (!result.isSuccess()) {
                displayCommandResult(result);
                return;
            }
            shell.renderFrame();

            GameModel model = App.getInstance().getCurrentGameModel();
            if (model == null || model.getState() != GameState.RUNNING) {
                // Win/lose verdict message produced by the controller.
                displayCommandResult(result);
                return;
            }
            try {
                Thread.sleep(TICK_RENDER_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        displayMessage("Advanced " + count + " ticks.");
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

    public void showKillStats() {
        CommandResult<String> result = controller.showKillStats();
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
