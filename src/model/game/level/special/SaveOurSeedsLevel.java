package model.game.level.special;

import model.app.App;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.level.ProtectedPlantTile;
import model.game.level.RegularLevel;
import model.game.map.Point;
import model.game.rule.SaveOurSeedsEndGameCondition;
import model.plant.PlantFactory;
import model.plant.instance.PlantInstance;

import java.io.IOException;
import java.util.List;

/**
 * Save Our Seeds: endangered plants are pre-placed on protected tiles and
 * the level is lost the moment any of them dies or disappears.
 *
 * <p>The loss rule lives in {@link SaveOurSeedsEndGameCondition}, which
 * checks every protected tile for a living plant on each tick. This class
 * pre-places the protected plants in {@link #onStart()} so that check is
 * meaningful from the first tick.
 *
 * <p>Which plant sits on which tile is configurable in the level data via
 * {@code protectedPlants} ({@code {x, y, plant}} entries). Tiles without a
 * plant name fall back to the level-wide {@code protectedPlantName}, and
 * finally to {@code Wall-nut}.
 */
public class SaveOurSeedsLevel extends RegularLevel {

    private static final String DEFAULT_PLANTS_JSON = "/assets/data/plants/plants.json";

    /** Last-resort plant when neither the tile nor the level names one. */
    private static final String DEFAULT_PROTECTED_PLANT = "Wall-nut";

    public SaveOurSeedsLevel(LevelConfig config) {
        super(config);
        config.setEndGameCondition(new SaveOurSeedsEndGameCondition(this));
    }

    @Override
    public boolean canStart() {
        List<ProtectedPlantTile> protectedTiles = getConfig().getProtectedPlants();
        if (!super.canStart() || protectedTiles == null || protectedTiles.isEmpty()) {
            return false;
        }
        if (!ensurePlantFactory()) {
            return false;
        }
        for (ProtectedPlantTile tile : protectedTiles) {
            Point position = tile.getPosition();
            boolean inGrid = position.getY() >= 0 && position.getY() < getConfig().getRows()
                    && position.getX() >= 0 && position.getX() < getConfig().getColumns();
            if (!inGrid || !PlantFactory.hasDefinition(plantNameFor(tile))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart(); // initial graves

        GameModel model = App.getInstance().getCurrentGameModel();
        List<ProtectedPlantTile> protectedTiles = getConfig().getProtectedPlants();
        if (model == null || protectedTiles == null || !ensurePlantFactory()) {
            return;
        }

        for (ProtectedPlantTile tile : protectedTiles) {
            PlantInstance plant = PlantFactory.createInstance(plantNameFor(tile));
            Point position = tile.getPosition();
            model.placePlant(plant, position.getY(), position.getX());
        }
    }

    /** Plant for a tile: per-tile name, then level-wide name, then default. */
    private String plantNameFor(ProtectedPlantTile tile) {
        String name = tile.getPlantName();
        if (name == null || name.isBlank()) {
            name = getConfig().getProtectedPlantName();
        }
        return name == null || name.isBlank() ? DEFAULT_PROTECTED_PLANT : name;
    }

    /**
     * True when the plant factory is usable, initialising it from the default
     * data file if nothing has done so yet.
     */
    private static boolean ensurePlantFactory() {
        try {
            PlantFactory.getAllDefinitions();
            return true;
        } catch (IllegalStateException notInitialised) {
            try {
                PlantFactory.init(DEFAULT_PLANTS_JSON);
                return true;
            } catch (IOException | RuntimeException loadError) {
                return false;
            }
        }
    }
}
