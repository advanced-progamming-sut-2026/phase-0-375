package model.game.level.minigame.izombie;

import model.app.App;
import model.enums.MiniGameType;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.level.minigame.MiniGameLevel;
import model.plant.PlantFactory;
import model.plant.instance.PlantInstance;
import model.zombie.ZombieFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * I, Zombie: the player fights on the zombies' side.
 */
public class IZombieLevel extends MiniGameLevel {

    private IZombieSettings settings = new IZombieSettings();

    public IZombieLevel(LevelConfig config, MiniGameType miniGameType, int difficultyTier) {
        super(config, miniGameType, difficultyTier);
    }

    public IZombieSettings getSettings() {
        return settings;
    }

    public void setSettings(IZombieSettings settings) {
        if (settings != null) {
            this.settings = settings;
        }
    }

    /** Column of the red line: zombies may only be placed at or right of it. */
    public int redLineColumn() {
        return settings.getRedLineColumn();
    }

    @Override
    public boolean canStart() {
        LevelConfig config = getConfig();
        if (config == null || config.getRows() <= 0 || config.getColumns() <= 0
                || config.getRules() == null
                || settings.getZombieCosts().isEmpty()
                || settings.getPlantLayout().isEmpty()) {
            return false;
        }
        int redLine = redLineColumn();
        if (redLine <= 0 || redLine >= config.getColumns()) {
            return false;
        }
        if (!ensurePlantFactory() || !ensureZombieFactory()) {
            return false;
        }
        // Every configured plant must exist, sit left of the red line, be in
        // bounds and occupy a unique cell.
        Set<String> occupiedCells = new HashSet<>();
        for (IZombieSettings.PlantPlacement placement : settings.getPlantLayout()) {
            if (!PlantFactory.hasDefinition(placement.getPlant())) {
                return false;
            }
            if (placement.getRow() < 0 || placement.getRow() >= config.getRows()
                    || placement.getCol() < 0 || placement.getCol() >= redLine) {
                return false;
            }
            if (!occupiedCells.add(placement.getRow() + ":" + placement.getCol())) {
                return false;
            }
        }
        for (String zombieName : settings.getZombieCosts().keySet()) {
            if (!ZombieFactory.hasDefinition(zombieName)) {
                return false;
            }
        }
        return ZombieFactory.hasDefinition(settings.getSunZombie());
    }

    @Override
    public void onStart() {
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) {
            return;
        }
        prePlantPlants(model);
        spawnSunZombies(model);
    }

    @Override
    public void tick(float deltaTime) {
        // All I, Zombie logic runs through the regular systems: placed
        // zombies walk and eat on their own, the sun zombies produce sun via
        // their behavior, and eaten brains are tracked per breached lane by
        // GameModel.
    }

    @Override
    public void onWaveCleared(int waveNumber) {
        // No waves: every attacking zombie is placed by the player.
    }

    @Override
    public void onFail() {
        // No special teardown.
    }

    /** Win: every lane's brain has been eaten (all rows breached). */
    @Override
    public boolean checkWinCondition(GameModel model) {
        return model != null
                && model.getBreachedRows().size() >= getConfig().getRows();
    }

    /**
     * Loss: no zombies left on the lawn and not enough sun to place even the
     * cheapest roster zombie - unless the last zombie just ate the final
     * brain, which is a win.
     */
    @Override
    public boolean checkLossCondition(GameModel model) {
        if (model == null || checkWinCondition(model)) {
            return false;
        }
        return model.getZombieCount() == 0
                && model.getSunAmount() < (int) (settings.minZombieCost() * model.difficultyPenalty());
    }

    /**
     * Places a roster zombie for the player, spending sun.
     *
     * @param row grid row (the x of the CLI command)
     * @param col grid column (the y of the CLI command)
     * @return null on success, otherwise a user-facing error message
     */
    public String placeZombie(GameModel model, String zombieName, int row, int col) {
        if (model == null) {
            return "No active game.";
        }
        String canonical = canonicalRosterName(zombieName);
        if (canonical == null) {
            return "'" + zombieName + "' is not one of this stage's zombies: "
                    + String.join(", ", settings.getZombieCosts().keySet()) + ".";
        }
        if (row < 0 || row >= getConfig().getRows()
                || col < 0 || col >= getConfig().getColumns()) {
            return "Position (" + col + ", " + row + ") is out of bounds.";
        }
        int redLine = redLineColumn();
        if (col < redLine) {
            return "Zombies can only be placed right of the red line (column >= "
                    + redLine + ").";
        }
        int cost = (int) (settings.getZombieCosts().get(canonical) * model.difficultyPenalty());
        if (!model.spendSun(cost)) {
            return "Not enough sun. Need " + cost + ", have " + model.getSunAmount() + ".";
        }
        if (model.spawnZombieAt(canonical, row, col) == null) {
            model.addSun(cost); // refund: the zombie could not be created
            return "Could not create zombie '" + canonical + "'.";
        }
        return null;
    }

    /** Resolves a user-typed name against the roster, case-insensitively. */
    private String canonicalRosterName(String zombieName) {
        if (zombieName == null || zombieName.isBlank()) {
            return null;
        }
        for (String name : settings.getZombieCosts().keySet()) {
            if (name.equalsIgnoreCase(zombieName)) {
                return name;
            }
        }
        return null;
    }

    // --- Board setup ---

    /** Pre-plants the stage's defense exactly as configured in minigames.json. */
    private void prePlantPlants(GameModel model) {
        for (IZombieSettings.PlantPlacement placement : settings.getPlantLayout()) {
            PlantInstance plant = PlantFactory.createInstance(placement.getPlant());
            if (plant != null) {
                model.placePlant(plant, placement.getRow(), placement.getCol());
            }
        }
    }

    /** Spawns one stationary sun-producing zombie per lane in the rightmost column. */
    private void spawnSunZombies(GameModel model) {
        int lastColumn = getConfig().getColumns() - 1;
        for (int row = 0; row < getConfig().getRows(); row++) {
            model.spawnZombieAt(settings.getSunZombie(), row, lastColumn);
        }
    }

    // --- Factory bootstrap (mirrors the other mini-games) ---

    private static boolean ensurePlantFactory() {
        try {
            PlantFactory.getAllDefinitions();
            return true;
        } catch (IllegalStateException notInitialised) {
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
                return true;
            } catch (IOException | RuntimeException loadError) {
                return false;
            }
        }
    }

    private static boolean ensureZombieFactory() {
        try {
            ZombieFactory.hasDefinition("ZombieDefault");
            return true;
        } catch (RuntimeException notInitialised) {
            try {
                ZombieFactory.init("/assets/data/zombies/zombies.json",
                        "/assets/data/armor/ArmorTypeData.json");
                return true;
            } catch (IOException | RuntimeException loadError) {
                return false;
            }
        }
    }
}
