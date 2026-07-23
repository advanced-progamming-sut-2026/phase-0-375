package model.game.systems;

import model.enums.Chapter;
import model.enums.GroundType;
import model.enums.PlacableLayer;
import model.enums.PlantTags;
import model.game.core.GameModel;
import model.game.core.Tickable;
import model.game.level.LevelConfig;
import model.game.map.Cell;
import model.game.map.Point;
import model.game.map.terrain.TerrainStrategyFactory;
import model.game.wave.EntryRuntime;
import model.game.wave.Wave;
import model.item.placeable.Placeable;
import model.plant.instance.PlantInstance;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Ambient chapter-specific mechanics that are not tied to a single zombie or
 * plant:
 *
 * <ul>
 *   <li><b>Frostbite Caves — ice wind:</b> when a wave starts, an icy wind may
 *       sweep a few rows; every non-fire plant on those rows gains one frost
 *       level (three levels fully freeze the plant, matching the Hunter
 *       zombie's snowballs).</li>
 *   <li><b>Frostbite Caves — fiery thaw:</b> a fire-tagged plant in one of the
 *       eight cells around a frozen plant melts its ice faster (spec: 600 HP
 *       of ice melting at 60 HP/s).</li>
 *   <li><b>Big Wave Beach — tide:</b> whenever a wave starts the water line
 *       shifts inside the tide band; newly flooded tiles become water
 *       (stranding non-aquatic plants), receding water frees the tiles.</li>
 *   <li><b>Big Wave Beach — low-tide ambush:</b> designated low-tide cells
 *       that are currently under water may release a zombie from beneath the
 *       surface at the start of each wave.</li>
 * </ul>
 *
 * The Ancient Egypt tornado entry is rolled at spawn time inside
 * {@link model.game.wave.Wave#spawnOnLane}.
 */
public class ChapterEffectsSystem implements Tickable {

    // --- Frostbite Caves: ice wind ---

    /** Chance that an icy wind blows when a wave starts. */
    public static final double ICE_WIND_CHANCE = 0.4;

    /** Maximum number of rows a single ice wind can hit. */
    public static final int ICE_WIND_MAX_ROWS = 2;

    /** Frost levels needed to fully freeze a plant (parity with the Hunter). */
    public static final int FROST_LEVELS_TO_FREEZE = 3;

    // --- Frostbite Caves: fiery thaw ---

    /** Total "HP" of the ice coating a fully frozen plant (spec value). */
    public static final int PLANT_ICE_HP = 600;

    /** Melt rate contributed by a neighbouring fiery plant (spec value). */
    public static final int FIERY_THAW_HP_PER_SECOND = 60;

    // --- Big Wave Beach ---

    /** Chance per submerged low-tide cell to release a zombie each wave. */
    public static final double LOW_TIDE_AMBUSH_CHANCE = 0.3;

    private final GameModel gameModel;
    private final Random random = new Random();

    /** Rightmost columns currently flooded by the tide (dynamic band only). */
    private int tideColumns;

    public ChapterEffectsSystem(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    // ------------------------------------------------------------------
    // Per-tick effects
    // ------------------------------------------------------------------

    @Override
    public void tick(float deltaTime) {
        if (gameModel.getChapter() == Chapter.FROSTBITE_CAVES && deltaTime > 0f) {
            tickFieryThaw(deltaTime);
        }
    }

    /**
     * Frozen plants thaw faster while a fiery plant sits in one of the eight
     * neighbouring cells. The engine tracks freezing as a timer rather than
     * ice HP, so the spec's 60 HP/s against 600 HP of ice is converted into
     * an equivalent extra-thaw rate.
     */
    private void tickFieryThaw(float deltaTime) {
        int rows = gameModel.getRowCount();
        int cols = gameModel.getColumnCount();
        float extraThaw = deltaTime
                * ((float) FIERY_THAW_HP_PER_SECOND / PLANT_ICE_HP)
                * PlantInstance.FREEZE_DURATION;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Cell cell = gameModel.getCellAt(row, col);
                if (cell == null) continue;
                Placeable main = cell.getPlaceable(PlacableLayer.MAIN);
                if (!(main instanceof PlantInstance)) continue;
                PlantInstance plant = (PlantInstance) main;
                if (plant.isFrozen() && hasFieryNeighbour(row, col)) {
                    plant.tickFreeze(extraThaw);
                }
            }
        }
    }

    /** @return true if a FIRE-tagged plant occupies one of the 8 neighbours. */
    private boolean hasFieryNeighbour(int row, int col) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                Cell neighbour = gameModel.getCellAt(row + dr, col + dc);
                if (neighbour == null) continue;
                Placeable occupant = neighbour.getPlaceable(PlacableLayer.MAIN);
                if (!(occupant instanceof PlantInstance)) continue;
                PlantInstance plant = (PlantInstance) occupant;
                if (!plant.isFrozen()
                        && plant.getDefinition() != null
                        && plant.getDefinition().hasTag(PlantTags.FIRE)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Wave-start effects
    // ------------------------------------------------------------------

    /** Invoked by the {@link GameModel} whenever a new wave begins. */
    public void onWaveStarted(Wave wave) {
        Chapter chapter = gameModel.getChapter();
        if (chapter == Chapter.FROSTBITE_CAVES) {
            maybeBlowIceWind();
        } else if (chapter == Chapter.BIG_WAVE_BEACH) {
            shiftTide();
            maybeAmbushFromLowTide(wave);
        }
    }

    // --- Ice wind ---

    private void maybeBlowIceWind() {
        if (random.nextDouble() >= ICE_WIND_CHANCE) return;

        int rowCount = gameModel.getRowCount();
        if (rowCount <= 0) return;

        List<Integer> rows = new ArrayList<>();
        for (int r = 0; r < rowCount; r++) rows.add(r);
        Collections.shuffle(rows, random);
        int hitCount = 1 + random.nextInt(Math.min(ICE_WIND_MAX_ROWS, rowCount));

        StringBuilder hitRows = new StringBuilder();
        for (int i = 0; i < hitCount; i++) {
            int row = rows.get(i);
            for (PlantInstance plant : gameModel.getPlantsInLane(row)) {
                if (plant.getDefinition() != null
                        && plant.getDefinition().hasTag(PlantTags.FIRE)) {
                    continue; // fire plants shrug the wind off
                }
                plant.registerFreezeHit(FROST_LEVELS_TO_FREEZE);
            }
            if (hitRows.length() > 0) hitRows.append(", ");
            hitRows.append(row + 1);
        }
        System.out.println("[Ice Wind] An icy wind sweeps row(s) " + hitRows
                + "! Non-fire plants there gain a frost level.");
    }

    // --- Tide ---

    /**
     * Rolls a new water line inside the dynamic tide band (the rightmost
     * {@code tideLimitColumn} columns) and re-types the affected tiles.
     * Static water tiles from the level config always stay water. Plants
     * stranded by rising water are destroyed by the TerrainSystem.
     */
    private void shiftTide() {
        LevelConfig config = levelConfig();
        if (config == null) return;
        int limit = config.getTideLimitColumn();
        if (limit <= 0) return;

        int cols = gameModel.getColumnCount();
        int rows = gameModel.getRowCount();
        limit = Math.min(limit, cols);

        int newTide = random.nextInt(limit + 1); // 0..limit flooded columns
        if (newTide == tideColumns) return;

        for (int col = cols - limit; col < cols; col++) {
            boolean flooded = col >= cols - newTide;
            for (int row = 0; row < rows; row++) {
                Cell cell = gameModel.getCellAt(row, col);
                if (cell == null || isStaticWater(config, row, col)) continue;

                if (flooded) {
                    GroundType ground = isLowTideCell(config, row, col)
                            ? GroundType.LOW_TIDE
                            : GroundType.WATER;
                    cell.setGroundType(ground);
                    cell.setTerrainStrategy(TerrainStrategyFactory.create(ground));
                } else if (cell.getGroundType() == GroundType.WATER
                        || cell.getGroundType() == GroundType.LOW_TIDE) {
                    cell.setGroundType(GroundType.NORMAL);
                    cell.setTerrainStrategy(
                            TerrainStrategyFactory.create(GroundType.NORMAL));
                }
            }
        }

        boolean rising = newTide > tideColumns;
        tideColumns = newTide;
        System.out.println("[Tide] The water " + (rising ? "rises" : "recedes")
                + ": the rightmost " + newTide + " column(s) are now flooded."
                + (rising ? " Stranded plants will drown!" : ""));
    }

    // --- Low-tide ambush ---

    private void maybeAmbushFromLowTide(Wave wave) {
        LevelConfig config = levelConfig();
        if (config == null || wave == null) return;
        List<Point> lowTides = config.getLowTideTiles();
        if (lowTides == null || lowTides.isEmpty()) return;

        for (Point p : lowTides) {
            Cell cell = gameModel.getCellAt(p.getY(), p.getX());
            if (cell == null) continue;
            GroundType ground = cell.getGroundType();
            if (ground != GroundType.LOW_TIDE && ground != GroundType.WATER) {
                continue; // only submerged low-tide cells can ambush
            }
            if (random.nextDouble() >= LOW_TIDE_AMBUSH_CHANCE) continue;

            Zombie zombie = rollWaveZombie(wave);
            if (zombie == null) continue;
            ZombieInstance spawned =
                    gameModel.spawnZombieAt(zombie.getName(), p.getY(), p.getX());
            if (spawned != null) {
                System.out.println("[Ambush] A " + zombie.getName()
                        + " bursts out of the shallows at row " + (p.getY() + 1)
                        + ", column " + (p.getX() + 1) + "!");
            }
        }
    }

    /** Picks a zombie from the starting wave's candidate pools. */
    private Zombie rollWaveZombie(Wave wave) {
        List<EntryRuntime> entries = wave.getRuntimeEntries();
        if (entries == null || entries.isEmpty()) return null;
        EntryRuntime entry = entries.get(random.nextInt(entries.size()));
        return wave.getRng().rollZombiePool(entry.getWaveZombieEntry().getPool());
    }

    // --- Helpers ---

    private LevelConfig levelConfig() {
        return gameModel.getCurrentLevel() != null
                ? gameModel.getCurrentLevel().getConfig()
                : null;
    }

    private boolean isStaticWater(LevelConfig config, int row, int col) {
        return containsPoint(config.getWaterTiles(), row, col);
    }

    private boolean isLowTideCell(LevelConfig config, int row, int col) {
        return containsPoint(config.getLowTideTiles(), row, col);
    }

    private boolean containsPoint(List<Point> points, int row, int col) {
        if (points == null) return false;
        for (Point p : points) {
            if (p.getY() == row && p.getX() == col) return true;
        }
        return false;
    }
}
