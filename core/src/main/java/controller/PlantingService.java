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
 * Handles every plant-related gameplay command
 */
final class PlantingService {

    /**
     * Places a plant of the given type at the given grid position.
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

        return plantSelected(model, conveyor, selected, type, x, y);
    }

    /** Validates the target cell and definition, then stacks or places the plant. */
    private CommandResult<Void> plantSelected(GameModel model, boolean conveyor,
                                              List<String> selected, String type, int x, int y) {
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

        CommandResult<Void> collision = checkLayerCollision(cell, targetLayer, type, x, y);
        if (collision != null) return collision;

        return placeNewPlant(model, conveyor, selected, definition, targetLayer, type, x, y);
    }

    /** @return an error result if the target layer is already occupied, else {@code null}. */
    private CommandResult<Void> checkLayerCollision(Cell cell, PlacableLayer targetLayer,
                                                    String type, int x, int y) {
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

        return null;
    }

    /** Builds the leveled instance, pays the sun cost and places it on the field. */
    private CommandResult<Void> placeNewPlant(GameModel model, boolean conveyor, List<String> selected,
                                              Plant definition, PlacableLayer targetLayer,
                                              String type, int x, int y) {
        // --- Build the instance first so leveled stats (incl. cost) apply ---
        PlantInstance instance = new PlantInstance(definition);
        int level = plantLevelFor(definition.getName());
        if (level > 1) {
            instance.applyLevelUpgrade(level);
        }

        // --- Seed packet recharge (skipped for conveyor — seeds come from the belt) ---
        if (!conveyor && !model.isSeedReady(definition.getName())) {
            return CommandResult.error("'" + type + "' is recharging. "
                    + String.format("%.1f", model.getSeedCooldown(definition.getName())) + "s left.");
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
        } else {
            model.startSeedRecharge(definition.getName(), instance.getDefinition().getRechargeTime());
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
     * Adds one head to an existing same-type stacker (Pea Pod).
     */
    private CommandResult<Void> growStack(GameModel model, PlantInstance existing,
                                          int x, int y, boolean conveyor,
                                          List<String> selected, String type) {
        if (!existing.canStackMore()) {
            int limit = existing.getStackLimit();
            return CommandResult.error("'" + type + "' at (" + x + ", " + y
                    + ") is already at its max stack of " + limit + ".");
        }
        if (!conveyor && !model.isSeedReady(existing.getDefinition().getName())) {
            return CommandResult.error("'" + type + "' is recharging. "
                    + String.format("%.1f", model.getSeedCooldown(existing.getDefinition().getName())) + "s left.");
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
        } else {
            model.startSeedRecharge(existing.getDefinition().getName(), existing.getDefinition().getRechargeTime());
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
     * Cheat: disables all seed-packet cooldowns for the rest of the level,
     * so the player can plant any seed at any time without waiting for recharge.
     */
    public CommandResult<Void> cheatRemoveCooldown() {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return guard;

        GameModel model = requireGame();
        model.disableSeedCooldowns();
        return CommandResult.success("Seed cooldowns disabled for the rest of the level.");
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
}