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
 * Read-only board inspection commands: map snapshot, plant/zombie status
 * reports, kill statistics and score summaries.
 *
 * <p>Extracted from {@link GameplayMenuController}, which composes this
 * service and delegates to it, so the view-facing API is unchanged.
 */
final class BoardInfoService {

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

    /**
     * Returns a textual snapshot of the entire game map — which cells
     * have plants, which cells have zombies, sun tokens on the ground.
     */
    public CommandResult<String> showMap() {
        CommandResult<Void> guard = guardGameRunning();
        if (guard != null) return retypeError(guard);

        GameModel model = requireGame();
        GameMap map = model.getMap();
        var level = model.getCurrentLevel();
        var config = level != null ? level.getConfig() : null;
        int tideLimit = config != null ? config.getTideLimitColumn() : -1;

        StringBuilder sb = new StringBuilder();
        appendMapHeader(sb, model, map, tideLimit);
        appendMapGrid(sb, model, map, level, tideLimit);

        sb.append("Legend: P plant  O overlay plant  B both  G ground plant  Z zombie  X zombie on plant\n");
        sb.append("T grave  V vase  ~ water  _ low tide  ^ slide up  v slide down  N necromancy  * ice  . empty\n");
        appendSunTokens(sb, model);
        return CommandResult.successWithData(sb.toString(), sb.toString());
    }

    /**
     * Appends the map title, resource summary (sun/plant food/tick), and
     * the column header row (with tide-limit markers).
     */
    private void appendMapHeader(StringBuilder sb, GameModel model, GameMap map, int tideLimit) {
        sb.append("── Map (").append(map.getRows()).append("x").append(map.getCols()).append(") ──\n");
        sb.append("Sun: ").append(model.getSunAmount())
                .append(" | Plant food: ").append(model.getPlantFoodCount())
                .append(" | Tick: ").append(model.getTick()).append("\n");

        sb.append("       ");
        for (int c = 0; c < map.getCols(); c++) {
            if (tideLimit > 0 && c >= map.getCols() - tideLimit) {
                sb.append("| ");
            } else {
                sb.append(String.format("%-2d", c));
            }
        }
        sb.append('\n');
    }

    /**
     * Appends the row-by-row cell grid, plus the belt/red-line/tide-limit
     * annotations that follow it.
     */
    private void appendMapGrid(StringBuilder sb, GameModel model, GameMap map, Object level, int tideLimit) {
        for (int r = 0; r < map.getRows(); r++) {
            StringBuilder row = new StringBuilder("  Row " + r + ": ");
            for (int c = 0; c < map.getCols(); c++) {
                // I, Zombie red line
                if (level instanceof model.game.level.minigame.izombie.IZombieLevel iZombie
                        && c == iZombie.redLineColumn()) {
                    row.append("| ");
                    continue;
                }
                Cell cell = map.getCell(c, r);
                char ch = cell != null ? cellChar(model, cell, c, r) : '.';
                // Vase Breaker: show V for unbroken vases
                if (level instanceof model.game.level.minigame.vasebreaker.VaseBreakerLevel vbLevel && ch == '.') {
                    if (vbLevel.vaseAt(c, r) != null) {
                        ch = 'V';
                    }
                }
                row.append(ch).append(' ');
            }
            sb.append(row).append('\n');
        }

        // Conveyor belt / bowling available plants
        List<String> beltPlants = model.getSelectedPlants();
        if ((level instanceof model.game.level.special.ConveyorBeltLevel
                || level instanceof model.game.level.minigame.bowling.WallnutBowlingLevel)
                && beltPlants != null && !beltPlants.isEmpty()) {
            sb.append("Belt: ");
            for (int i = 0; i < beltPlants.size(); i++) {
                if (i > 0) sb.append(", ");
                if (i == 0) sb.append("[").append(beltPlants.get(i)).append("]");
                else sb.append(beltPlants.get(i));
            }
            sb.append('\n');
        }

        // I, Zombie red line info
        if (level instanceof model.game.level.minigame.izombie.IZombieLevel iZombie) {
            sb.append("Red line at column ").append(iZombie.redLineColumn())
                    .append(" | zombies must be placed right of |\n");
        }

        // Beach tide limit indicator
        if (tideLimit > 0) {
            sb.append("Tide limit: rightmost ").append(tideLimit)
                    .append(" column(s) may flood (marked with |)\n");
        }
    }

    /** Resolves the single map symbol for one cell: terrain, then grave, then plants, then zombies. */
    private char cellChar(GameModel model, Cell cell, int c, int r) {
        // Terrain first (lowest layer): water, low tide, slides, necromancy, ice.
        char ch = terrainChar(cell.getGroundType());

        // Graves sit on the field and hide the terrain under them.
        for (PlacableLayer layer : PlacableLayer.values()) {
            if (cell.getPlaceable(layer) instanceof Grave) {
                ch = 'T';
                break;
            }
        }

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
                ch = (ch == 'P' || ch == 'O' || ch == 'B') ? 'X' : 'Z';
                break;
            }
        }
        return ch;
    }

    /** Lists any uncollected sun tokens lying on the field. */
    private void appendSunTokens(StringBuilder sb, GameModel model) {
        if (model.getActiveSuns().isEmpty()) return;
        sb.append("Sun tokens on the ground:\n");
        for (Sun s : model.getActiveSuns()) {
            sb.append("  (").append(s.getX()).append(", ").append(s.getY())
                    .append(") value=").append(s.getValue()).append('\n');
        }
    }

    /** Maps a cell's ground type to its map symbol. */
    private char terrainChar(GroundType ground) {
        if (ground == null) {
            return '.';
        }
        switch (ground) {
            case WATER: return '~';
            case LOW_TIDE: return '_';
            case SLIDE_UP: return '^';
            case SLIDE_DOWN: return 'v';
            case NECROMANCY: return 'N';
            case ICE: return '*';
            case CRATER: return 'O';
            default: return '.';
        }
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
