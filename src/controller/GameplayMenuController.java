package controller;

import controller.result.CommandResult;
import model.app.App;
import model.enums.GameState;
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
import model.game.wave.WaveManager;
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

    // ──────────────────────────────────────────────
    // Myopoint score game
    // ──────────────────────────────────────────────

    /** Shows the current Myopoint score and its per-pattern breakdown. */
    public CommandResult<Void> showScore() {
        GameModel model = requireGame();
        if (model == null) {
            return CommandResult.error("No active game. Start a level from the game menu first.");
        }
        if (!(model.getCurrentLevel() instanceof ScoreLevel scoreLevel)) {
            return CommandResult.error("The current level is not a Myopoint score game.");
        }
        StringBuilder text = new StringBuilder();
        for (String line : scoreLevel.getTracker().getSummaryLines()) {
            if (text.length() > 0) text.append(System.lineSeparator());
            text.append(line);
        }
        return CommandResult.success(text.toString());
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private GameModel requireGame() {
        return App.getInstance().getCurrentGameModel();
    }

    private PvZGameLoop requireLoop() {
        return App.getInstance().getCurrentGameLoop();
    }

    private CommandResult<Void> guardGameRunning() {
        GameModel model = requireGame();
        if (model == null) {
            return CommandResult.error("No active game. Start a level from the game menu first.");
        }
        if (model.getState() == GameState.WON) {
            return CommandResult.error("Level already won. Use 'menu exit' to return.");
        }
        if (model.getState() == GameState.LOST) {
            return CommandResult.error("Level already lost. Use 'menu exit' to return.");
        }
        return null;
    }

    private boolean inBounds(GameMap map, int x, int y) {
        return x >= 0 && y >= 0
                && x < map.getCols()
                && y < map.getRows();
    }

    /**
     * Returns the {@link PlantInstance} placed on the MAIN layer of the
     * given cell, or {@code null} if the cell is empty.
     *
     * <p>This bypasses {@link Cell#getMainPlant()}, which has a
     * pre-existing bug: it casts the stored {@code Placeable} to
     * {@code Plant} (the definition class), but the actual stored
     * object is a {@code PlantInstance} (which {@code implements
     * Placeable}). Using {@link Cell#getPlaceable(PlacableLayer)}
     * + a safe cast avoids the {@link ClassCastException}.
     */
    private PlantInstance plantAt(Cell cell) {
        if (cell == null) return null;
        var p = cell.getPlaceable(PlacableLayer.MAIN);
        return (p instanceof PlantInstance) ? (PlantInstance) p : null;
    }

    /**
     * Adapts a {@code CommandResult<Void>} error to whatever payload type the
     * caller needs. Used so {@code guardGameRunning()} can be reused inside
     * methods that return {@code CommandResult<T>} for various {@code T}.
     */
    @SuppressWarnings("unchecked")
    private static <T> CommandResult<T> errorTyped(String message) {
        return (CommandResult<T>) CommandResult.error(message);
    }

    /** Same as {@link #errorTyped(String)} but pulls the message from an existing failed result. */
    @SuppressWarnings("unchecked")
    private static <T> CommandResult<T> retypeError(CommandResult<Void> source) {
        return (CommandResult<T>) source;
    }

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
     * Places a plant of the given type at the given grid position.
     *
     * <p>Validation:
     * <ul>
     *   <li>The plant type must be one of the player's selected plants for this level.</li>
     *   <li>The cell must be in-bounds.</li>
     *   <li>The player must have enough sun to pay the plant's cost.</li>
     * </ul>
     */
    public CommandResult<Void> plant(String type, int x, int y) {
        if (type == null || type.isBlank()) {
            return CommandResult.error("Plant type cannot be empty.");
        }
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();

        // Wall-nut Bowling: the plant command rolls a walnut from the
        // leftmost column instead of placing a plant.
        if (model.getCurrentLevel() instanceof WallnutBowlingLevel bowling) {
            return rollWalnut(model, bowling, type, x, y);
        }

        // Vase Breaker: plants come from seed packets revealed by breaking
        // vases and are planted for free on any free tile.
        if (model.getCurrentLevel() instanceof VaseBreakerLevel vaseBreaker) {
            return plantFromVase(model, vaseBreaker, type, x, y);
        }

        boolean conveyor = model.getCurrentLevel() instanceof ConveyorBeltLevel;
        List<String> selected = model.getSelectedPlants();
        if (selected == null || !selected.contains(type)) {
            return CommandResult.error(conveyor
                    ? "Plant '" + type + "' is not on the conveyor belt right now."
                    : "Plant '" + type + "' is not in your selection."
                            + " Add it from the plant selection menu.");
        }

        GameMap map = model.getMap();
        if (!inBounds(map, x, y)) {
            return CommandResult.error("Position (" + x + ", " + y + ") is out of bounds. "
                    + "Map is " + map.getRows() + "x" + map.getCols() + ".");
        }
        Cell cell = map.getCell(x, y);
        if (cell == null) {
            return CommandResult.error("Cell (" + x + ", " + y + ") does not exist.");
        }

        Plant definition = lookupPlantDefinition(type);
        if (definition == null) {
            return CommandResult.error("Unknown plant type: '" + type + "'.");
        }

        PlacableLayer targetLayer = computeLayer(definition);

        // --- Same-type stacker (Pea Pod) ---
        if (definition.hasTag(PlantTags.STACK) && targetLayer == PlacableLayer.MAIN) {
            Placeable existingMain = cell.getPlaceable(PlacableLayer.MAIN);
            if (existingMain instanceof PlantInstance existingPlant
                    && existingPlant.getDefinition() != null
                    && existingPlant.getDefinition().getName().equalsIgnoreCase(definition.getName())) {
                return growStack(model, existingPlant, x, y, conveyor, selected, type);
            }
        }

        // --- Layer collision check ---
        if (cell.getPlaceable(targetLayer) != null) {
            String layerName = targetLayer.name().toLowerCase();
            String hint = switch (targetLayer) {
                case OVERLAY -> " A cover plant is already on this cell.";
                case GROUND -> " A foundation is already on this cell.";
                default -> "";
            };
            return CommandResult.error("Cannot place '" + type + "' on the " + layerName
                    + " layer at (" + x + ", " + y + ") — already occupied." + hint);
        }

        // For MAIN-layer plants, also reject if the cell has a
        // non-plant placeable on the GROUND layer.
        if (targetLayer == PlacableLayer.MAIN && cell.getPlaceable(PlacableLayer.GROUND) != null) {
            Placeable ground = cell.getPlaceable(PlacableLayer.GROUND);
            if (!(ground instanceof PlantInstance)) {
                return CommandResult.error("An item is already placed at (" + x + ", " + y + ").");
            }
        }

        // --- Build the instance first so leveled stats (incl. cost) apply ---
        PlantInstance instance = new PlantInstance(definition);
        int level = plantLevelFor(definition.getName());
        if (level > 1) {
            instance.applyLevelUpgrade(level);
        }

        // --- Sun cost (leveled definition may discount it) ---
        int cost = conveyor ? 0 : instance.getDefinition().getCost();
        if (!model.spendSun(cost)) {
            return CommandResult.error("Not enough sun. Need " + cost
                    + ", have " + model.getSunAmount() + ".");
        }

        instance.setPosition(new Point(x, y));

        // Imitater: default the imitate target to the first non-Imitater
        // plant in the player's selection.
        wireImitateTargetIfNeeded(instance, definition, model.getSelectedPlants());

        boolean placed = model.placePlant(instance, y, x);
        if (!placed) {
            model.addSun(cost);
            return CommandResult.error("Cannot plant '" + type + "' at (" + x + ", " + y
                    + "). The terrain rejected it (need a Lily Pad on water?).");
        }
        if (conveyor) {
            selected.remove(type); // consume the seed packet from the belt
        }
        String note = consumeBoostIfAny(instance) ? " Boost consumed: plant food activated!" : "";
        String stackNote = definition.hasTag(PlantTags.STACK)
                ? " (STACK: layer=" + targetLayer + ")"
                : "";
        return CommandResult.success("Planted " + type + " at (" + x + ", " + y
                + ") for " + cost + " sun. Remaining sun: " + model.getSunAmount()
                + "." + stackNote + note);
    }

    /**
     * @return the cell layer this plant should occupy.
     */
    private PlacableLayer computeLayer(Plant definition) {
        if (definition == null || !definition.hasTag(PlantTags.STACK)) {
            return PlacableLayer.MAIN;
        }
        if (definition.hasTag(PlantTags.WATER)) {
            return PlacableLayer.GROUND;
        }
        if (definition.getCategory() == PlantCategory.WALL_NUT) {
            return PlacableLayer.OVERLAY;
        }
        return PlacableLayer.MAIN;
    }

    /**
     * Adds one head to an existing same-type stacker (Pea Pod). Pays
     * the cost, increments {@link PlantInstance#incrementStackCount()},
     * and reports the new stack size. If the instance is already at its
     * stack limit, the cost is not charged.
     */
    private CommandResult<Void> growStack(GameModel model, PlantInstance existing,
                                          int x, int y, boolean conveyor,
                                          List<String> selected, String type) {
        if (!existing.canStackMore()) {
            int limit = existing.getStackLimit();
            return CommandResult.error("'" + type + "' at (" + x + ", " + y
                    + ") is already at its max stack of " + limit + ".");
        }
        int cost = conveyor ? 0 : existing.getDefinition().getCost();
        if (!model.spendSun(cost)) {
            return CommandResult.error("Not enough sun. Need " + cost
                    + ", have " + model.getSunAmount() + ".");
        }
        boolean grew = existing.incrementStackCount();
        if (!grew) {
            model.addSun(cost);
            return CommandResult.error("'" + type + "' at (" + x + ", " + y
                    + ") is already at its max stack.");
        }
        if (conveyor) {
            selected.remove(type);
        }
        return CommandResult.success("Stacked another '" + type + "' at (" + x + ", " + y
                + ") for " + cost + " sun. Stack: " + existing.getStackCount()
                + "/" + existing.getStackLimit()
                + ". Remaining sun: " + model.getSunAmount() + ".");
    }

    /**
     * Consumes a stored one-shot boost (greenhouse harvest or 'boost plant')
     * for this plant type; the plant starts in its plant-food phase.
     */
    private boolean consumeBoostIfAny(PlantInstance instance) {
        User user = App.getInstance().getCurrentUser();
        if (user == null || user.getPlantBoosts() == null) {
            return false;
        }
        String name = instance.getDefinition().getName();
        for (Map.Entry<String, Boolean> e : user.getPlantBoosts().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue()) && e.getKey().equalsIgnoreCase(name)) {
                e.setValue(false); // one-shot
                App.getInstance().getUserRepository().flush();
                instance.activatePlantFood();
                return true;
            }
        }
        return false;
    }

    /**
     * Wall-nut Bowling: consumes a walnut from the conveyor belt and rolls
     * it from the leftmost column down the given lane.
     */
    private CommandResult<Void> rollWalnut(GameModel model, WallnutBowlingLevel bowling,
                                           String type, int x, int y) {
        BowlingWalnutType walnutType = WallnutBowlingLevel.parseWalnutType(type);
        if (walnutType == null) {
            return CommandResult.error("Unknown walnut type: '" + type + "'.");
        }
        List<String> belt = model.getSelectedPlants();
        String beltEntry = findBeltEntry(belt, walnutType);
        if (beltEntry == null) {
            return CommandResult.error("Walnut '" + type + "' is not on the conveyor belt right now.");
        }
        GameMap map = model.getMap();
        if (!inBounds(map, x, y)) {
            return CommandResult.error("Position (" + x + ", " + y + ") is out of bounds. "
                    + "Map is " + map.getRows() + "x" + map.getCols() + ".");
        }
        if (x != 0) {
            return CommandResult.error("Bowling walnuts must be launched from the leftmost column:"
                    + " use -l (0," + y + ").");
        }
        bowling.launchWalnut(walnutType, y);
        belt.remove(beltEntry);
        return CommandResult.success("Rolled " + beltEntry + " down lane " + y + ".");
    }

    /** First belt entry naming the same walnut type (aliases accepted). */
    private String findBeltEntry(List<String> belt, BowlingWalnutType type) {
        if (belt == null) return null;
        for (String entry : belt) {
            if (WallnutBowlingLevel.parseWalnutType(entry) == type) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Vase Breaker: plants are free but must come from a seed packet
     * revealed by breaking a vase; it can then be planted on any free
     * tile before it expires.
     */
    private CommandResult<Void> plantFromVase(GameModel model, VaseBreakerLevel level,
                                              String type, int x, int y) {
        GameMap map = model.getMap();
        if (!inBounds(map, x, y)) {
            return CommandResult.error("Position (" + x + ", " + y + ") is out of bounds. "
                    + "Map is " + map.getRows() + "x" + map.getCols() + ".");
        }
        if (level.vaseAt(x, y) != null) {
            return CommandResult.error("There is an unbroken vase at (" + x + ", " + y + ")."
                    + " Break it first: break vase -l (" + x + "," + y + ")");
        }
        Cell cell = map.getCell(x, y);
        if (cell == null) {
            return CommandResult.error("Cell (" + x + ", " + y + ") does not exist.");
        }
        if (plantAt(cell) != null) {
            return CommandResult.error("A plant is already placed at (" + x + ", " + y + ").");
        }
        PendingSeedPacket packet = level.claimSeedPacket(type);
        if (packet == null) {
            return CommandResult.error("No '" + type + "' seed packet is available."
                    + " Break a vase containing one and plant it before it expires.");
        }
        PlantInstance instance = new PlantInstance(packet.getPlant());
        instance.setPosition(new Point(x, y));
        cell.addPlaceable(instance);
        return CommandResult.success("Planted " + packet.getPlant().getName()
                + " at (" + x + ", " + y + ") for free.");
    }

    /** Vase Breaker: breaks the vase at (x, y) and reveals its contents. */
    public CommandResult<Void> breakVase(int x, int y) {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        if (!(model.getCurrentLevel() instanceof VaseBreakerLevel level)) {
            return CommandResult.error("Breaking vases is only available in the Vase Breaker mini-game.");
        }
        GameMap map = model.getMap();
        if (!inBounds(map, x, y)) {
            return CommandResult.error("Position (" + x + ", " + y + ") is out of bounds. "
                    + "Map is " + map.getRows() + "x" + map.getCols() + ".");
        }
        Vase vase = level.vaseAt(x, y);
        if (vase == null) {
            return CommandResult.error("There is no unbroken vase at (" + x + ", " + y + ").");
        }
        return CommandResult.success(level.breakVase(model, vase));
    }

    /**
     * If the placed plant is an Imitater, sets its imitate target to the
     * first non-Imitater plant in the player's selection.
     */
    private void wireImitateTargetIfNeeded(PlantInstance instance, Plant definition,
                                           List<String> selectedPlants) {
        if (instance == null || definition == null) return;
        String name = definition.getName();
        if (name == null) return;
        if (!name.toLowerCase().contains("imitat")) return;
        if (selectedPlants == null || selectedPlants.isEmpty()) return;

        for (String candidate : selectedPlants) {
            if (candidate == null) continue;
            if (candidate.equalsIgnoreCase(name)) continue; // skip self
            // Skip other Imitaters.
            if (candidate.toLowerCase().contains("imitat")) continue;
            // Skip mints (they're one-shot boosts, not useful copy targets).
            if (candidate.toLowerCase().contains("-mint")) continue;
            // Make sure the candidate actually exists in the factory.
            if (!PlantFactory.hasDefinition(candidate)) continue;
            instance.setImitateTarget(candidate);
            return;
        }
    }

    /**
     * Cheat: clears the recharge (cooldown) of all currently placed
     * plants so they can immediately act again.
     */
    public CommandResult<Void> cheatRemoveCooldown() {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        GameMap map = model.getMap();
        int cleared = 0;
        for (int r = 0; r < map.getRows(); r++) {
            for (int c = 0; c < map.getCols(); c++) {
                Cell cell = map.getCell(c, r);
                if (cell == null) continue;
                PlantInstance pi = plantAt(cell);
                if (pi == null) continue;
                // Reset this instance's recharge so the next ability tick fires
                // immediately. PlantInstance exposes a setter for currentRecharge.
                pi.setCurrentRecharge(0f);
                cleared++;
            }
        }
        if (cleared == 0) {
            return CommandResult.error("No plants on the field to recharge.");
        }
        return CommandResult.success("Removed cooldown for " + cleared + " plant(s).");
    }

    /**
     * Removes (plucks) the topmost plant at the given grid position.
     * Sun spent on the plant is not refunded.
     */
    public CommandResult<Void> pluck(int x, int y) {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        GameMap map = model.getMap();
        if (!inBounds(map, x, y)) {
            return CommandResult.error("Position (" + x + ", " + y + ") is out of bounds.");
        }
        Cell cell = map.getCell(x, y);
        if (cell == null) {
            return CommandResult.error("No plant at (" + x + ", " + y + ").");
        }
        PlantInstance instance = cell.getTopmostPlant();
        if (instance == null) {
            Placeable ground = cell.getPlaceable(PlacableLayer.GROUND);
            if (ground instanceof PlantInstance groundPlant) {
                instance = groundPlant;
            }
        }
        if (instance == null) {
            return CommandResult.error("No plant at (" + x + ", " + y + ").");
        }
        String plantName = instance.getDefinition().getName();
        PlacableLayer layer = instance.getLayer();
        if (instance.getStackCount() > 1) {
            instance.setStackCount(instance.getStackCount() - 1);
            instance.setCurrentHP(Math.max(1,
                    instance.getCurrentHP() - instance.getDefinition().getBaseHP()));
            return CommandResult.success("Plucked one head of '" + plantName
                    + "' at (" + x + ", " + y + "). Stack remaining: "
                    + instance.getStackCount() + "/" + instance.getStackLimit() + ".");
        }
        // PlantInstance implements Placeable; remove via the cell API.
        cell.removePlaceable(instance);
        return CommandResult.success("Plucked plant '" + plantName
                + "' (layer=" + layer + ") from (" + x + ", " + y + ").");
    }

    /**
     * Feeds plant food to the topmost plant at the given grid position.
     * Consumes one unit of stored plant food. For stacked tiles, the
     * OVERLAY plant is fed first; if absent, the MAIN plant
     * (e.g. Pea Pod, Sunflower) is fed.
     */
    public CommandResult<Void> feed(int x, int y) {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        GameMap map = model.getMap();
        if (!inBounds(map, x, y)) {
            return CommandResult.error("Position (" + x + ", " + y + ") is out of bounds.");
        }
        Cell cell = map.getCell(x, y);
        if (cell == null) {
            return CommandResult.error("No plant at (" + x + ", " + y + ") to feed.");
        }
        PlantInstance instance = cell.getTopmostPlant();
        if (instance == null) {
            return CommandResult.error("No plant at (" + x + ", " + y + ") to feed.");
        }
        if (!model.usePlantFood()) {
            return CommandResult.error("No plant food available. Cheat with 'cheat add-plant-food'.");
        }
        // Activate the plant food effect on this instance. PlantInstance's
        // activatePlantFood() schedules the per-plant plant-food effect,
        // which fires on the next tick via the plant's ability strategy.
        instance.activatePlantFood();
        return CommandResult.success("Fed plant food to '" + instance.getDefinition().getName()
                + "' (layer=" + instance.getLayer() + ") at (" + x + ", " + y
                + "). Plant food remaining: " + model.getPlantFoodCount() + ".");
    }

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
     * Returns a textual snapshot of the entire game map — which cells
     * have plants, which cells have zombies, sun tokens on the ground.
     */
    public CommandResult<String> showMap() {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return retypeError(guard);

        GameModel model = requireGame();
        GameMap map = model.getMap();
        StringBuilder sb = new StringBuilder();
        sb.append("── Map (").append(map.getRows()).append("x").append(map.getCols()).append(") ──\n");
        sb.append("Sun: ").append(model.getSunAmount())
                .append(" | Plant food: ").append(model.getPlantFoodCount())
                .append(" | Tick: ").append(model.getTick()).append("\n");

        for (int r = 0; r < map.getRows(); r++) {
            StringBuilder row = new StringBuilder("  Row " + r + ": ");
            for (int c = 0; c < map.getCols(); c++) {
                Cell cell = map.getCell(c, r);
                char ch = '.';
                if (cell != null) {
                    // Render every plant layer so stacked tiles are visible: GROUND (Lily Pad) -> 'G', (regular plant)
                    // -> 'P', OVERLAY (Pumpkin) -> 'O'. A cell with both MAIN and OVERLAY becomes 'B' (both).
                    boolean hasGround = cell.getPlaceable(PlacableLayer.GROUND) instanceof PlantInstance;
                    boolean hasMain = cell.getPlaceable(PlacableLayer.MAIN) instanceof PlantInstance;
                    boolean hasOverlay = cell.getPlaceable(PlacableLayer.OVERLAY) instanceof PlantInstance;
                    if (hasMain && hasOverlay) ch = 'B';
                    else if (hasMain) ch = 'P';
                    else if (hasOverlay) ch = 'O';
                    else if (hasGround) ch = 'G';

                    for (ZombieInstance z : model.getZombies()) {
                        var gp = z.getGridPosition();
                        if (gp != null && gp.getX() == c && gp.getY() == r) {
                            ch = (ch == '.' || ch == 'G') ? 'Z' : 'X';
                            break;
                        }
                    }
                }
                row.append(ch).append(' ');
            }
            sb.append(row).append('\n');
        }
        // Sun tokens on the ground
        if (!model.getActiveSuns().isEmpty()) {
            sb.append("Sun tokens on the ground:\n");
            for (Sun s : model.getActiveSuns()) {
                sb.append("  (").append(s.getX()).append(", ").append(s.getY())
                        .append(") value=").append(s.getValue()).append('\n');
            }
        }
        return CommandResult.successWithData(sb.toString(), sb.toString());
    }

    /**
     * Returns a textual status report for every plant currently on the
     * field, across all layers (GROUND, MAIN, OVERLAY). Stacked plants
     * on the same cell are listed separately with their layer tag.
     */
    public CommandResult<String> showPlantsStatus() {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return retypeError(guard);

        GameModel model = requireGame();
        GameMap map = model.getMap();
        StringBuilder sb = new StringBuilder("── Plants on field ──\n");
        int count = 0;
        for (int r = 0; r < map.getRows(); r++) {
            for (int c = 0; c < map.getCols(); c++) {
                Cell cell = map.getCell(c, r);
                if (cell == null) continue;
                for (PlantInstance plant : cell.getAllPlants()) {
                    Plant def = plant.getDefinition();
                    count++;
                    sb.append("  [").append(plant.getLayer()).append("] ")
                            .append(def.getName())
                            .append(" @ (").append(c).append(", ").append(r).append(")")
                            .append(" cost=").append(def.getCost())
                            .append(" hp=").append(plant.getCurrentHP()).append("/").append(def.getBaseHP())
                            .append(" level=").append(plant.getLevel())
                            .append(" state=").append(plant.getState());
                    if (plant.getStackCount() > 1) {
                        sb.append(" stack=").append(plant.getStackCount())
                                .append("/").append(plant.getStackLimit());
                    }
                    sb.append('\n');
                }
            }
        }
        if (count == 0) {
            sb.append("  (no plants placed yet)\n");
        }
        sb.append("Total: ").append(count).append(" plant(s).");
        return CommandResult.successWithData(sb.toString(), sb.toString());
    }

    /** Diagnostic: kill-attribution stats used by exclusive-kill quests. */
    public CommandResult<String> showKillStats() {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return retypeError(guard);

        GameModel model = requireGame();
        StringBuilder sb = new StringBuilder("── Kill attribution ──\n");
        sb.append("Total zombies killed: ").append(model.getZombiesKilled()).append('\n');
        sb.append("Mower kills (this level): ").append(model.getMowerKills()).append('\n');
        int exclusiveTotal = 0;
        sb.append("Exclusive kills per plant:\n");
        for (java.util.Map.Entry<String, Integer> e : model.getExclusivePlantKillsMap().entrySet()) {
            sb.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append('\n');
            exclusiveTotal += e.getValue();
        }
        sb.append("Exclusive kills per family:\n");
        for (java.util.Map.Entry<PlantCategory, Integer> e : model.getExclusiveFamilyKillsMap().entrySet()) {
            sb.append("  ").append(e.getKey().name()).append(" = ").append(e.getValue()).append('\n');
        }
        sb.append("Non-exclusive kills (mixed families or non-plant damage like mowers): ")
                .append(model.getZombiesKilled() - exclusiveTotal);
        String out = sb.toString();
        return CommandResult.successWithData(out, out);
    }

    /**
     * Returns detailed information about the cell at (x, y): terrain,
     * every stacked plant (GROUND/MAIN/OVERLAY), zombies, projectiles.
     */
    public CommandResult<String> showTileStatus(int x, int y) {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return retypeError(guard);

        GameModel model = requireGame();
        GameMap map = model.getMap();
        if (!inBounds(map, x, y)) { return errorTyped("Position (" + x + ", " + y + ") is out of bounds."); }
        Cell cell = map.getCell(x, y);
        StringBuilder sb = new StringBuilder();
        sb.append("── Tile (").append(x).append(", ").append(y).append(") ──\n");
        if (cell == null) {
            sb.append("  (cell not initialized)");
        } else {
            List<PlantInstance> plants = cell.getAllPlants();
            if (plants.isEmpty()) {
                sb.append("  Plants: (none)\n");
            } else {
                sb.append("  Plants (").append(plants.size()).append("):\n");
                for (PlantInstance plant : plants) {
                    Plant def = plant.getDefinition();
                    sb.append("    [").append(plant.getLayer()).append("] ")
                            .append(def.getName())
                            .append(" hp=").append(plant.getCurrentHP()).append("/").append(def.getBaseHP())
                            .append(" level=").append(plant.getLevel())
                            .append(" state=").append(plant.getState());
                    if (plant.getStackCount() > 1) {
                        sb.append(" stack=").append(plant.getStackCount())
                                .append("/").append(plant.getStackLimit());
                    }
                    sb.append('\n');
                }
            }
            // Zombies on this tile
            List<ZombieInstance> here = new ArrayList<>();
            for (ZombieInstance z : model.getZombies()) {
                var gp = z.getGridPosition();
                if (gp != null && gp.getX() == x && gp.getY() == y) {
                    here.add(z);
                }
            }
            sb.append("  Zombies: ").append(here.size()).append('\n');
            for (ZombieInstance z : here) {
                sb.append("    - ").append(z.getDefinition().getName())
                        .append(" hp=").append(z.getCurrentHP())
                        .append(" state=").append(z.getState())
                        .append('\n');
            }
        }
        return CommandResult.successWithData(sb.toString(), sb.toString());
    }

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
        // Snapshot to avoid ConcurrentModificationException while removing
        List<ZombieInstance> snapshot = new ArrayList<>(model.getZombies());
        for (ZombieInstance z : snapshot) {
            model.removeZombie(z);
        }
        return CommandResult.success("Nuke released! " + killed + " zombie(s) vaporized.");
    }

    /**
     * Returns detailed information about every zombie currently on the
     * field — position, HP, armor, status effects. Output format matches
     * the example in the project spec.
     */
    public CommandResult<String> zombiesInfo() {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return retypeError(guard);

        GameModel model = requireGame();
        List<ZombieInstance> zombies = model.getZombies();
        StringBuilder sb = new StringBuilder();
        if (zombies.isEmpty()) {
            sb.append("No zombies on the field.");
        } else {
            for (ZombieInstance z : zombies) {
                sb.append(z.getDefinition().getName()).append(":\n");
                var gp = z.getGridPosition();
                sb.append("  position: ")
                        .append(gp == null ? "?" : gp.getX() + ", " + gp.getY())
                        .append('\n');
                sb.append("  health: ").append(z.getCurrentHP()).append('\n');
                // Armor summary
                if (z.getArmors() == null || z.getArmors().isEmpty()) {
                    sb.append("  armor: (none)\n");
                } else {
                    sb.append("  armor:\n");
                    for (var a : z.getArmors()) {
                        sb.append("    ").append(a).append('\n');
                    }
                }
                // Effects
                sb.append("  effects:\n");
                if (z.isFrozen()) {
                    sb.append("    frozen\n");
                } else if (z.isChilled()) {
                    sb.append("    chilled\n");
                }
                if (z.isEating()) {
                    sb.append("    eating\n");
                }
            }
        }
        return CommandResult.successWithData(sb.toString(), sb.toString());
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

    // ──────────────────────────────────────────────
    // Plant definition lookup
    // ──────────────────────────────────────────────

    /**
     * Minimal inline plant catalogue so the gameplay loop is testable
     * before PlantRegistry is merged. Each entry maps a name to a
     * fully-constructed {@link Plant} with sensible starter stats.
     *
     * @param name plant name (e.g. {@code "Sunflower"})
     * @return the matching definition, or {@code null} if no plant with
     *         that name is registered or the factory has not been initialized
     */
    /** User's saved level for this plant (defaults to 1). */
    private int plantLevelFor(String plantName) {
        User user = App.getInstance().getCurrentUser();
        Map<String, Integer> levels = user != null ? user.getPlantLevels() : null;
        return levels != null ? levels.getOrDefault(plantName, 1) : 1;
    }

    private Plant lookupPlantDefinition(String name) {
        if (name == null || name.isBlank()) return null;
        // Exact match first (fast path).
        try {
            Plant plant = PlantFactory.getDefinition(name);
            if (plant != null) return plant;
        } catch (IllegalStateException ignored) {
            // Factory not initialized yet - fall through to return null.
            return null;
        }
        // Case-insensitive fallback across every registered definition.
        for (Plant plant : PlantFactory.getAllDefinitions()) {
            if (plant.getName().equalsIgnoreCase(name)) return plant;
        }
        return null;
    }



    /**
     * Returns the list of plant names this user has selected for the
     * current level. Used by the view for tab-completion / hints.
     */
    public CommandResult<List<String>> selectedPlants() {
        GameModel model = requireGame();
        if (model == null) {
            return errorTyped("No active game.");
        }
        List<String> selected = model.getSelectedPlants();
        if (selected == null) selected = java.util.Collections.emptyList();
        return CommandResult.successWithData(
                "Selected plants (" + selected.size() + "):",
                new ArrayList<>(selected));
    }
}
