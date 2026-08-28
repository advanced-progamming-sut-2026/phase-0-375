package model.game.level.special;

import model.app.App;
import model.enums.Chapter;
import model.enums.LevelType;
import model.enums.MenuType;
import model.enums.ZombieSize;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.rule.GameRules;
import model.game.rule.TimedWarEndGameCondition;
import model.user.User;
import model.zombie.ZombieFactory;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TimedWarLevelProgressTest {

    private User previousUser;
    private GameModel previousModel;
    private MenuType previousMenu;
    private LevelConfig config;
    private GameRules rules;
    private TimedWarLevel timedWarLevel;
    private TimedWarEndGameCondition condition;

    @BeforeEach
    void setUp() {
        previousUser = App.getInstance().getCurrentUser();
        previousModel = App.getInstance().getCurrentGameModel();
        previousMenu = App.getInstance().getCurrentMenu();

        User user = new User();
        App.getInstance().setCurrentUser(user);
        App.getInstance().setCurrentGameModel(null);

        rules = new GameRules(
                false, false, 150, 1.0, 0, 99,
                Set.of(), Set.of(), Set.of());
        rules.setTimedWarLimit(180f);
        rules.setTimedWarTargetKills(10);

        config = new LevelConfig();
        config.setChapter(Chapter.DARK_AGES);
        config.setLevelId(3);
        config.setLevelType(LevelType.TIMED_WAR);
        config.setRules(rules);
        config.setRows(5);
        config.setColumns(9);

        try {
            ZombieFactory.init("/assets/data/armor/ArmorTypeData.json", "/assets/data/zombies/zombies.json");
        } catch (Exception ignored) {}

        timedWarLevel = new TimedWarLevel(config);
        condition = new TimedWarEndGameCondition(timedWarLevel);
    }

    @AfterEach
    void tearDown() {
        App.getInstance().setCurrentUser(previousUser);
        App.getInstance().setCurrentGameModel(previousModel);
        App.getInstance().setCurrentMenu(previousMenu);
    }

    @Test
    void testInitialProgressIsZero() {
        timedWarLevel.onStart();
        assertEquals(0, timedWarLevel.getEffectiveKills());
        assertEquals(10, timedWarLevel.getTargetKills());
        assertEquals(0f, timedWarLevel.getProgress01(), 0.001f);
    }

    @Test
    void testRecordKillIncrementsProgress() {
        timedWarLevel.onStart();
        timedWarLevel.recordKill();
        timedWarLevel.recordKill();

        assertEquals(2, timedWarLevel.getEffectiveKills());
        assertEquals(0.2f, timedWarLevel.getProgress01(), 0.001f);
    }

    @Test
    void testProgressDecaysWhenNoKillsOccurDuringLapse() {
        timedWarLevel.onStart();
        timedWarLevel.setDecayInterval(4.0f);
        timedWarLevel.recordKill();
        timedWarLevel.recordKill();
        timedWarLevel.recordKill(); // 3 kills

        assertEquals(3, timedWarLevel.getEffectiveKills());

        // Tick 2 seconds - no decay yet
        timedWarLevel.tick(2.0f);
        assertEquals(3, timedWarLevel.getEffectiveKills());

        // Tick another 2.5 seconds (total 4.5s > 4.0s) -> 1 kill decays
        timedWarLevel.tick(2.5f);
        assertEquals(2, timedWarLevel.getEffectiveKills());
        assertEquals(0.2f, timedWarLevel.getProgress01(), 0.001f);

        // Tick another 4.0 seconds -> decays to 1
        timedWarLevel.tick(4.0f);
        assertEquals(1, timedWarLevel.getEffectiveKills());

        // Tick another 4.0 seconds -> decays to 0
        timedWarLevel.tick(4.0f);
        assertEquals(0, timedWarLevel.getEffectiveKills());

        // Should not drop below 0
        timedWarLevel.tick(4.0f);
        assertEquals(0, timedWarLevel.getEffectiveKills());
    }

    @Test
    void testKillResetsDecayTimer() {
        timedWarLevel.onStart();
        timedWarLevel.setDecayInterval(4.0f);
        timedWarLevel.recordKill(); // 1 kill

        // Tick 3 seconds
        timedWarLevel.tick(3.0f);
        assertEquals(1, timedWarLevel.getEffectiveKills());

        // New kill arrives at 3.0s -> resets decay timer
        timedWarLevel.recordKill(); // 2 kills
        assertEquals(2, timedWarLevel.getEffectiveKills());

        // Tick 2 seconds -> total elapsed since tick is 2s, should not decay
        timedWarLevel.tick(2.0f);
        assertEquals(2, timedWarLevel.getEffectiveKills());
    }

    @Test
    void testWinConditionWithEffectiveKills() {
        timedWarLevel.onStart();
        GameModel mockModel = new GameModel(timedWarLevel);

        assertFalse(condition.isWin(mockModel));

        // Reach target kills
        timedWarLevel.setEffectiveKills(10);
        assertTrue(condition.isWin(mockModel));

        // If decayed below target, no longer win
        timedWarLevel.setEffectiveKills(9);
        assertFalse(condition.isWin(mockModel));
    }

    @Test
    void testNukeKillsUpdateProgress() {
        App.getInstance().setCurrentMenu(MenuType.IN_GAME);
        timedWarLevel.onStart();
        GameModel model = new GameModel(timedWarLevel);
        App.getInstance().setCurrentGameModel(model);

        Zombie dummy = new Zombie("ZombieDefault", 100, 1.0f, 10f,
                ZombieSize.NORMAL, Chapter.ANCIENT_EGYPT, 10, 10,
                java.util.List.of(), null, null, java.util.List.of());
        ZombieInstance z1 = new ZombieInstance(dummy);
        ZombieInstance z2 = new ZombieInstance(dummy);
        ZombieInstance z3 = new ZombieInstance(dummy);
        model.getZombies().add(z1);
        model.getZombies().add(z2);
        model.getZombies().add(z3);

        assertEquals(3, model.getZombies().size());
        assertEquals(0, model.getZombiesKilled());

        // Release nuke
        controller.GameplayMenuController.getInstance().releaseNuke();
        assertEquals(3, model.getZombiesKilled());

        // Tick level
        timedWarLevel.tick(0.1f);
        assertEquals(3, timedWarLevel.getEffectiveKills());
        assertEquals(0.3f, timedWarLevel.getProgress01(), 0.001f);
    }
}
