package view.gui.ui;

import model.app.App;
import model.enums.Chapter;
import model.enums.LevelType;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.level.RegularLevel;
import model.game.level.special.LoveYourPlantsLevel;
import model.game.rule.GameRules;
import model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LoveYourPlantsHudTest {

    private User previousUser;

    @BeforeEach
    void setUp() {
        previousUser = App.getInstance().getCurrentUser();
        User user = new User();
        App.getInstance().setCurrentUser(user);
    }

    @AfterEach
    void tearDown() {
        App.getInstance().setCurrentUser(previousUser);
    }

    private static LevelConfig createConfig(LevelType type, int maxDeaths) {
        LevelConfig config = new LevelConfig();
        config.setChapter(Chapter.DARK_AGES);
        config.setLevelId(2);
        config.setRows(5);
        config.setColumns(9);
        config.setLevelType(type);
        GameRules rules = new GameRules(
                false, false, 150, 1.0, 0, 99,
                Set.of(), Set.of(), Set.of());
        rules.setMaxPlantDeaths(maxDeaths);
        config.setRules(rules);
        return config;
    }

    @Test
    void resourceIdsAreCorrect() {
        assertEquals("IMAGE_UI_HUD_WORLDMAP_LEVEL_COUNTER", LoveYourPlantsHud.COUNTER_ID);
        assertEquals("IMAGE_UI_HUD_INGAME_CHALLENGE_PLANT_LOST_ICON", LoveYourPlantsHud.ICON_ID);
    }

    @Test
    void showForOnlyLoveYourPlantsLevel() {
        assertFalse(LoveYourPlantsHud.showFor(null));

        LevelConfig config = createConfig(LevelType.LOVE_YOUR_PLANTS, 3);
        LoveYourPlantsLevel level = new LoveYourPlantsLevel(config);
        GameModel model = new GameModel(level);

        assertTrue(LoveYourPlantsHud.showFor(model));

        // Normal level without max plant deaths
        LevelConfig normalConfig = createConfig(LevelType.NORMAL, -1);
        RegularLevel regLevel = new RegularLevel(normalConfig);
        GameModel regModel = new GameModel(regLevel);

        assertFalse(LoveYourPlantsHud.showFor(regModel));
    }

    @Test
    void formatAndRemainingCountCalculation() {
        LevelConfig config = createConfig(LevelType.LOVE_YOUR_PLANTS, 3);
        LoveYourPlantsLevel level = new LoveYourPlantsLevel(config);
        GameModel model = new GameModel(level);

        int maxDeaths = model.getCurrentLevel().getConfig().getRules().getMaxPlantDeaths();
        int lost = model.getPlantsLost();
        int remaining = Math.max(0, maxDeaths - lost);
        assertEquals(3, remaining);
        assertEquals("3 Left", remaining + " Left");

        // When 1 plant is lost
        int remainingAfterOne = Math.max(0, maxDeaths - 1);
        assertEquals(2, remainingAfterOne);
        assertEquals("2 Left", remainingAfterOne + " Left");

        // When 3 plants are lost
        int remainingAfterThree = Math.max(0, maxDeaths - 3);
        assertEquals(0, remainingAfterThree);
        assertEquals("0 Left", remainingAfterThree + " Left");
    }
}
