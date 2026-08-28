package model.game.level.special;

import controller.GameplayMenuController;
import model.app.App;
import model.enums.Chapter;
import model.enums.LevelType;
import model.enums.WaveManagerPhase;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.rule.GameRules;
import model.game.rule.NeverEndCondition;
import model.game.wave.Wave;
import model.game.wave.WaveManager;
import model.plant.PlantFactory;
import model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantWhatYouGetLevelTest {

    private User previousUser;

    @BeforeEach
    void setUp() {
        previousUser = App.getInstance().getCurrentUser();
        User user = new User();
        App.getInstance().setCurrentUser(user);
        try {
            PlantFactory.getAllDefinitions();
        } catch (Exception e) {
            try {
                PlantFactory.init("/assets/data/plants/plants.json");
            } catch (Exception ignored) {}
        }
    }

    @AfterEach
    void tearDown() {
        App.getInstance().setCurrentUser(previousUser);
        App.getInstance().setCurrentGameModel(null);
    }

    @Test
    void setupPhaseInitializationAndStartWaves() {
        LevelConfig config = stubConfig(1000);
        PlantWhatYouGetLevel level = new PlantWhatYouGetLevel(config);

        assertTrue(level.isSetupPhase(), "Should start in setup phase");
        level.startWaves();
        assertFalse(level.isSetupPhase(), "Should leave setup phase after startWaves");
    }

    @Test
    void waveManagerPausesDuringSetupPhase() {
        LevelConfig config = stubConfig(1000);
        Wave wave = new Wave(1, List.of(), 5f, false, false);
        config.setWaves(List.of(wave));
        PlantWhatYouGetLevel level = new PlantWhatYouGetLevel(config);
        GameModel model = new GameModel(level);

        WaveManager waveManager = model.getWaveManager();

        // Tick 10 seconds during setup phase
        waveManager.tick(10f);
        assertEquals(WaveManagerPhase.WAITING_FOR_NEXT_WAVE, waveManager.getPhase(), "Wave should not start while in setup phase");

        // End setup phase
        level.startWaves();

        // Tick past delay to start the wave
        waveManager.tick(6f);
        assertEquals(WaveManagerPhase.ACTIVE_WAVE, waveManager.getPhase(), "Wave should start after setup phase ends");
    }

    @Test
    void pluckingRefundsSunDuringSetupPhase() {
        LevelConfig config = stubConfig(500);
        PlantWhatYouGetLevel level = new PlantWhatYouGetLevel(config);
        GameModel model = new GameModel(level);
        App.getInstance().setCurrentGameModel(model);
        model.setSelectedPlants(List.of("Peashooter"));

        GameplayMenuController controller = GameplayMenuController.getInstance();

        // Initial sun is 500
        assertEquals(500, model.getSunAmount());

        // Plant Peashooter (cost 100)
        var plantResult = controller.plant("Peashooter", 2, 2);
        assertTrue(plantResult.isSuccess(), "Planting should succeed: " + plantResult.getMessage());
        assertEquals(400, model.getSunAmount());

        // Pluck during setup phase -> refunds 100 sun
        var pluckResult = controller.pluck(2, 2);
        assertTrue(pluckResult.isSuccess(), "Pluck should succeed: " + pluckResult.getMessage());
        assertEquals(500, model.getSunAmount(), "Sun should be fully refunded during setup phase");

        // End setup phase
        level.startWaves();

        // Plant again after waves start
        plantResult = controller.plant("Peashooter", 2, 2);
        assertTrue(plantResult.isSuccess(), "Second planting failed: " + plantResult.getMessage());
        assertEquals(400, model.getSunAmount());

        // Pluck after waves started -> no refund
        pluckResult = controller.pluck(2, 2);
        assertTrue(pluckResult.isSuccess(), "Second pluck failed: " + pluckResult.getMessage());
        assertEquals(400, model.getSunAmount(), "Sun should not be refunded after setup phase ends");
    }

    private static LevelConfig stubConfig(int initialSun) {
        LevelConfig config = new LevelConfig();
        config.setChapter(Chapter.ANCIENT_EGYPT);
        config.setLevelId(1);
        config.setRows(5);
        config.setColumns(9);
        config.setLevelType(LevelType.PLANT_WHAT_YOU_GET);
        config.setRules(new GameRules(
                false, false, initialSun, 1.0, 0, 99,
                Set.of(), Set.of(), Set.of()));
        config.setEndGameCondition(new NeverEndCondition());
        return config;
    }
}
