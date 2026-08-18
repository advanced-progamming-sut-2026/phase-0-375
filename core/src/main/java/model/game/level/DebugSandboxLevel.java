package model.game.level;

import model.app.App;
import model.enums.Chapter;
import model.enums.LevelType;
import model.game.core.GameModel;
import model.game.rule.GameRules;
import model.game.rule.NeverEndCondition;
import model.plant.PlantFactory;
import model.plant.definition.Plant;
import model.zombie.ZombieFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FrontLawn debug playground: no waves, no win/lose, every plant available.
 */
public final class DebugSandboxLevel extends Level {

    private static final String PLANTS_JSON = "/assets/data/plants/plants.json";
    private static final String ZOMBIES_JSON = "/assets/data/zombies/zombies.json";
    private static final String ARMOR_JSON = "/assets/data/zombies/armor.json";

    public static DebugSandboxLevel create() {
        LevelConfig config = new LevelConfig();
        config.setChapter(Chapter.ANCIENT_EGYPT);
        config.setLevelId(0);
        config.setRows(5);
        config.setColumns(9);
        config.setLevelType(LevelType.NORMAL);
        config.setWaves(Collections.emptyList());
        config.setEndGameCondition(new NeverEndCondition());

        GameRules rules = new GameRules(
                true,
                true,
                99_999,
                1.2,
                0,
                999,
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet());
        rules.setSunFallsFromSky(true);
        rules.setSunProducingPlantsAllowed(true);
        rules.setPlantFoodDrops(false);
        rules.setLawnMowersEnabled(false);
        rules.setShovelEnabled(true);
        rules.setAllowsChoosingPlants(false);
        rules.setPlantRechargeInSetup(false);
        config.setRules(rules);

        return new DebugSandboxLevel(config);
    }

    private DebugSandboxLevel(LevelConfig config) {
        super(config);
    }

    @Override
    public boolean canStart() {
        LevelConfig config = getConfig();
        return config.getRows() > 0
                && config.getColumns() > 0
                && config.getRules() != null;
    }

    @Override
    public void onStart() {
        ensureFactories();
        GameModel model = App.getInstance().getCurrentGameModel();
        if (model == null) {
            return;
        }

        List<String> allPlants = new ArrayList<>();
        for (Plant plant : PlantFactory.getAllDefinitions()) {
            if (plant != null && plant.getName() != null && !plant.getName().isBlank()) {
                allPlants.add(plant.getName());
            }
        }
        model.setSelectedPlants(allPlants);
        model.disableSeedCooldowns();
    }

    private static void ensureFactories() {
        try {
            PlantFactory.getAllDefinitions();
        } catch (IllegalStateException e) {
            try {
                PlantFactory.init(PLANTS_JSON);
            } catch (IOException ignored) {
                // Planting will fail loudly via controllers if init truly failed.
            }
        }
        try {
            ZombieFactory.getAllDefinitions();
        } catch (IllegalStateException e) {
            try {
                ZombieFactory.init(ZOMBIES_JSON, ARMOR_JSON);
            } catch (IOException ignored) {
                // Spawning will fail loudly via controllers if init truly failed.
            }
        }
    }

    @Override
    public void tick(float deltaTime) {
        // Sandbox has no level-driven scripting.
    }

    @Override
    public void onWaveCleared(int waveNumber) {
        // No waves.
    }

    @Override
    public void onComplete() {
        // Never ends.
    }

    @Override
    public void onFail() {
        // Never ends.
    }

    @Override
    public boolean checkWinCondition(GameModel model) {
        return false;
    }

    @Override
    public boolean checkLossCondition(GameModel model) {
        return false;
    }
}
