package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.GameState;
import model.enums.GroundType;
import model.enums.MenuType;
import model.enums.PlacableLayer;
import model.enums.PlantCategory;
import model.enums.PlantTags;
import model.enums.WaveManagerPhase;
import model.game.core.GameModel;
import model.game.core.PvZGameLoop;
import model.enums.BowlingWalnutType;
import model.game.level.special.ConveyorBeltLevel;
import model.game.level.special.ScoreLevel;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import model.game.level.minigame.vasebreaker.PendingSeedPacket;
import model.game.level.minigame.vasebreaker.Vase;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.game.map.Cell;
import model.game.map.GameMap;
import model.game.map.Point;
import model.game.map.WaterBand;
import model.game.map.terrain.IceTerrainStrategy;
import model.game.wave.WaveManager;
import model.item.Grave;
import model.item.Sun;
import model.item.placeable.Placeable;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.plant.instance.PlantInstance;
import model.user.User;
import model.zombie.ZombieFactory;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.game.level.minigame.izombie.IZombieLevel;
import model.game.level.minigame.beghouled.BeghouledLevel;

import static controller.GameplayGuards.*;

/**
 * Controller for the in-game (gameplay) menu.
 *
 * <p>All gameplay commands — placing plants, collecting sun, advancing
 * the simulation, cheats, and inspection commands — flow through here.
 * The controller is the only component that mutates the {@link GameModel}
 * / {@link PvZGameLoop} held by {@link App}.
 *
 * <p>Every method returns a {@link CommandResult} so the view layer can
 * uniformly render success/error messages. Methods that need to return
 * structured data (e.g. lists of zombies, sun amount) use the
 * {@code successWithData(...)} factory.
 */
public class GameplayMenuController extends AppMenuController {
    private static GameplayMenuController instance = null;

    /** One in-game tick is treated as this many seconds of simulated time. */
    private static final float SECONDS_PER_TICK = 0.1f;

    private static final String WIN_MESSAGE =
            "Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz.";
    private static final String LOSE_MESSAGE =
            "The zombie ate your brain; LOSER!!!";

    private GameplayMenuController() {}

    public static GameplayMenuController getInstance() {
        if (instance == null) instance = new GameplayMenuController();
        return instance;
    }

    // ──────────────────────────────────────────────
    // Menu navigation
    // ──────────────────────────────────────────────

    @Override
    public CommandResult<Void> menuEnter(String menuName) {
        return CommandResult.error("Cannot enter other menus during gameplay. Use 'menu exit' to abort the level.");
    }

    @Override
    public CommandResult<Void> menuExit() {
        App app = App.getInstance();
        app.setCurrentMenu(MenuType.GAME);
        app.setCurrentGameModel(null);
        app.setCurrentGameLoop(null);
        return CommandResult.success("Returned to game menu.");
    }

    // ------------------------------------------------------------------
    // Composed services: shared guards live in GameplayGuards; planting
    // and board-inspection commands are delegated to focused services so
    // this controller stays a thin facade over the gameplay commands.
    // ------------------------------------------------------------------

    private final PlantingService planting = new PlantingService();
    private final BoardInfoService boardInfo = new BoardInfoService();

    /** Places a plant of the given type; see {@link PlantingService#plant}. */
    public CommandResult<Void> plant(String type, int x, int y) { return planting.plant(type, x, y); }

    /** Vase Breaker: breaks the vase at (x, y) and reveals its contents. */
    public CommandResult<Void> breakVase(int x, int y) { return planting.breakVase(x, y); }

    /** Cheat: clears the recharge (cooldown) of all currently placed plants. */
    public CommandResult<Void> cheatRemoveCooldown() { return planting.cheatRemoveCooldown(); }

    /** Removes (plucks) the topmost plant at the given grid position. */
    public CommandResult<Void> pluck(int x, int y) { return planting.pluck(x, y); }

    /** Feeds plant food to the topmost plant at the given grid position. */
    public CommandResult<Void> feed(int x, int y) { return planting.feed(x, y); }

    /** Shows the current Myopoint score and its per-pattern breakdown. */
    public CommandResult<Void> showScore() { return boardInfo.showScore(); }

    /** Returns a textual snapshot of the entire game map. */
    public CommandResult<String> showMap() { return boardInfo.showMap(); }

    /** Returns a status report for every plant currently on the field. */
    public CommandResult<String> showPlantsStatus() { return boardInfo.showPlantsStatus(); }

    /** Diagnostic: kill-attribution stats used by exclusive-kill quests. */
    public CommandResult<String> showKillStats() { return boardInfo.showKillStats(); }

    /** Returns detailed information about the cell at (x, y). */
    public CommandResult<String> showTileStatus(int x, int y) { return boardInfo.showTileStatus(x, y); }

    /** Returns detailed information about every zombie on the field. */
    public CommandResult<String> zombiesInfo() { return boardInfo.zombiesInfo(); }

    /** Returns the plant names selected for the current level. */
    public CommandResult<List<String>> selectedPlants() { return boardInfo.selectedPlants(); }

    // ──────────────────────────────────────────────
    // Time / simulation
    // ──────────────────────────────────────────────

    /**
     * Advances the game simulation by {@code count} ticks. Each tick
     * corresponds to {@link #SECONDS_PER_TICK} seconds of simulated time.
     */
    public CommandResult<Void> advanceTime(int count) {
        if (count <= 0) {
            return CommandResult.error("Tick count must be a positive integer.");
        }
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        PvZGameLoop loop = requireLoop();
        if (loop == null) {
            return CommandResult.error("Game loop is not initialized.");
        }
        GameModel model = requireGame();
        for (int i = 0; i < count; i++) {
            loop.update(SECONDS_PER_TICK);
            if (model.getState() == GameState.WON) {
                return CommandResult.success(WIN_MESSAGE);
            }
            if (model.getState() == GameState.LOST) {
                return CommandResult.success(LOSE_MESSAGE);
            }
        }
        return CommandResult.success("Advanced " + count + " ticks.");
    }

    /**
     * Manually starts the zombie waves (otherwise they auto-start after
     * the level's start delay).
     */
    public CommandResult<Void> startZombieWaves() {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        WaveManager wm = model.getWaveManager();
        if (wm == null) {
            return CommandResult.error("This level has no waves to start.");
        }
        if (wm.getPhase() != WaveManagerPhase.WAITING_FOR_NEXT_WAVE) {
            return CommandResult.error("Waves are already in progress (phase: " + wm.getPhase() + ").");
        }
        wm.startNextWave();
        return CommandResult.success("Zombie waves have started!");
    }

    // ──────────────────────────────────────────────
    // Sun
    // ──────────────────────────────────────────────

    /**
     * Collects a sun token at the given grid position. The Sun must
     * currently exist on the board at that position.
     */
    public CommandResult<Void> collectSun(int x, int y) {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        Sun picked = null;
        for (Sun s : model.getActiveSuns()) {
            if (s.getX() == x && s.getY() == y) {
                picked = s;
                break;
            }
        }
        if (picked == null) {
            return CommandResult.error("No sun at (" + x + ", " + y + ").");
        }
        model.collectSun(picked);
        return CommandResult.success("Collected " + picked.getValue() + " sun."
                + " Total: " + model.getSunAmount() + ".");
    }

    /**
     * Returns the player's current sun balance.
     */
    public CommandResult<Integer> showSunAmount() {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return retypeError(guard);

        GameModel model = requireGame();
        return CommandResult.successWithData("Sun: " + model.getSunAmount(),
                model.getSunAmount());
    }

    /**
     * Cheat: adds {@code count} sun to the player's balance.
     */
    public CommandResult<Void> cheatAddSuns(int count) {
        if (count <= 0) {
            return CommandResult.error("Count must be a positive integer.");
        }
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        model.addSun(count);
        return CommandResult.success("Added " + count + " sun. Total: "
                + model.getSunAmount() + ".");
    }

    // ──────────────────────────────────────────────
    // Plants
    // ──────────────────────────────────────────────

    /**
     * Cheat: grants one unit of plant food.
     */
    public CommandResult<Void> cheatAddPlantFood() {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        model.addPlantFood();
        return CommandResult.success("Added 1 plant food. Total: "
                + model.getPlantFoodCount() + ".");
    }

    // ──────────────────────────────────────────────
    // Inspection
    // ──────────────────────────────────────────────

    /**
     * Releases "the nuke" — an in-game cheat that wipes all zombies
     * currently on the field. Per the spec, this is the only way to
     * forcibly clear the board.
     */
    public CommandResult<Void> releaseNuke() {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        int killed = model.getZombies().size();
        if (killed == 0) {
            return CommandResult.success(
                "Dear humanz, zis is not done yet; we will come back to eat your brainz, humanz. (No zombies to nuke.)"
            );
        }
        // Snapshot to avoid ConcurrentModificationException while removing.
        // Fire on-death hooks first (Wizard sheep revert, barrel leftover, …)
        // then strip the board, including anything those hooks spawned.
        List<ZombieInstance> snapshot = new ArrayList<>(model.getZombies());
        for (ZombieInstance z : snapshot) {
            z.fireOnDeathBehaviors(model);
        }
        for (ZombieInstance z : new ArrayList<>(model.getZombies())) {
            model.removeZombie(z);
        }
        return CommandResult.success("Nuke released! " + killed + " zombie(s) vaporized.");
    }

    /**
     * Cheat: spawns a zombie of the given type at the given grid
     * position. Used for testing defense layouts.
     */
    public CommandResult<Void> cheatSpawnZombie(String zombieType, int x, int y) {
        if (zombieType == null || zombieType.isBlank()) {
            return CommandResult.error("Zombie type cannot be empty.");
        }
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        GameMap map = model.getMap();
        if (!inBounds(map, x, y)) {
            return CommandResult.error("Position (" + x + ", " + y + ") is out of bounds.");
        }

        // Try the ZombieFactory registry first.
        ZombieInstance instance = null;
        try {
            instance = ZombieFactory.createInstance(zombieType);
        } catch (Throwable t) {
            // ZombieFactory.init() may not have been called yet; fall back
            // to a graceful error.
            instance = null;
        }
        if (instance == null) {
            return CommandResult.error("Unknown zombie type: '" + zombieType
                    + "'. Make sure ZombieFactory has been initialized.");
        }
        instance.setGridPosition(new Point(x, y));
        instance.setContinuousPosition(new model.game.map.FloatPoint(x, y));
        model.getZombies().add(instance);
        map.addZombie(instance, x, y);
        return CommandResult.success("Spawned '" + zombieType + "' at (" + x + ", " + y + ").");
    }

    /**
     * Cheat: plants a frozen zombie inside a Frostbite ice block at the cell.
     * The zombie is not on the walking list until the ice melts.
     */
    public CommandResult<Void> cheatSpawnIcedZombie(String zombieType, int x, int y) {
        if (zombieType == null || zombieType.isBlank()) {
            return CommandResult.error("Zombie type cannot be empty.");
        }
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        GameMap map = model.getMap();
        if (!inBounds(map, x, y)) {
            return CommandResult.error("Position (" + x + ", " + y + ") is out of bounds.");
        }
        Cell cell = model.getCellAt(y, x);
        if (cell == null) {
            return CommandResult.error("No cell at (" + x + ", " + y + ").");
        }

        ZombieInstance instance;
        try {
            instance = ZombieFactory.createInstance(zombieType);
        } catch (Throwable t) {
            instance = null;
        }
        if (instance == null) {
            return CommandResult.error("Unknown zombie type: '" + zombieType
                    + "'. Make sure ZombieFactory has been initialized.");
        }
        instance.setGridPosition(new Point(x, y));
        instance.setContinuousPosition(new model.game.map.FloatPoint(x, y));
        cell.setGroundType(GroundType.ICE);
        cell.setTerrainStrategy(new IceTerrainStrategy(instance));
        return CommandResult.success("Iced '" + zombieType + "' at (" + x + ", " + y + ").");
    }

    /**
     * Cheat / beach setup: flood the rightmost {@code columnsFromRight} columns
     * with water (0 clears the band). Same helper Big Wave Beach should use.
     */
    public CommandResult<Void> cheatSetWaterBand(int columnsFromRight) {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;
        if (columnsFromRight < 0) {
            return CommandResult.error("Water band width cannot be negative.");
        }
        GameModel model = requireGame();
        GameMap map = model.getMap();
        WaterBand.applyFromRight(map, columnsFromRight);
        int live = WaterBand.columnsFromRight(map);
        if (live == 0) {
            return CommandResult.success("Cleared the water band.");
        }
        return CommandResult.success("Water on the rightmost " + live + " column(s).");
    }

    /**
     * Moves the water line one tile inland (positive) or seaward (negative).
     */
    public CommandResult<Void> cheatNudgeWaterBand(int delta) {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;
        GameModel model = requireGame();
        GameMap map = model.getMap();
        int before = WaterBand.columnsFromRight(map);
        int live = WaterBand.nudgeFromRight(map, delta);
        if (live == before) {
            return CommandResult.success(live >= map.getCols()
                    ? "Water already reaches the leftmost column."
                    : "Water is already gone.");
        }
        String dir = live > before ? "left" : "right";
        return CommandResult.success("Water line moved " + dir + " 1 tile (" + live + " column(s)).");
    }

    /**
     * I, Zombie: places a roster zombie on the lawn, spending sun.
     * x is the column and y is the row, matching the plant command.
     */
    public CommandResult<Void> placeZombie(String type, int x, int y) {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        if (!(model.getCurrentLevel() instanceof IZombieLevel iZombie)) {
            return CommandResult.error("Placing zombies is only possible in the I, Zombie mini-game.");
        }
        String error = iZombie.placeZombie(model, type, y, x);
        if (error != null) {
            return CommandResult.error(error);
        }
        return CommandResult.success("Placed '" + type + "' at (" + x + ", " + y + ").");
    }

    /**
     * Beghouled: swaps the plant at (x, y) with its neighbour in the given
     * direction, provided the swap creates a match of 3+. x = column, y = row.
     */
    public CommandResult<Void> swapPlant(int x, int y, String direction) {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        if (!(model.getCurrentLevel() instanceof BeghouledLevel beghouled)) {
            return CommandResult.error("Swapping plants is only possible in the Beghouled mini-game.");
        }
        String error = beghouled.swapPlant(model, y, x, direction);
        if (error != null) {
            return CommandResult.error(error);
        }
        return CommandResult.success("Swapped. Matches: " + beghouled.getMatchesMade()
                + "/" + beghouled.getSettings().getMatchTarget()
                + ", sun: " + model.getSunAmount() + ".");
    }

    /** Beghouled: upgrades every plant of the given type on the board at once. */
    public CommandResult<Void> upgradePlant(String type) {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        if (!(model.getCurrentLevel() instanceof BeghouledLevel beghouled)) {
            return CommandResult.error("Upgrading plants is only possible in the Beghouled mini-game.");
        }
        String error = beghouled.upgradePlant(model, type);
        if (error != null) {
            return CommandResult.error(error);
        }
        return CommandResult.success("Upgraded every '" + type + "'. Sun left: " + model.getSunAmount() + ".");
    }

    /** Beghouled: shows match progress, craters and the upgrade price list. */
    public CommandResult<Void> beghouledStatus() {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        if (!(model.getCurrentLevel() instanceof BeghouledLevel beghouled)) {
            return CommandResult.error("This command is only available in the Beghouled mini-game.");
        }
        return CommandResult.success(beghouled.statusReport(model));
    }

}
