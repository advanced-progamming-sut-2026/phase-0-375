package view.gui.lawn;

import model.app.App;
import model.enums.Chapter;
import model.enums.LevelType;
import model.enums.MiniGameType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.game.core.GameModel;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.level.minigame.izombie.IZombieLevel;
import model.game.map.FloatPoint;
import model.game.map.Point;
import model.game.rule.GameRules;
import model.game.rule.NeverEndCondition;
import model.user.User;
import model.zombie.definition.Zombie;
import model.zombie.instance.ZombieInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DangerRedShaderTest {

    private User previousUser;

    @BeforeEach
    void setUp() {
        previousUser = App.getInstance().getCurrentUser();
        App.getInstance().setCurrentUser(new User());
    }

    @AfterEach
    void tearDown() {
        App.getInstance().setCurrentUser(previousUser);
    }

    @Test
    void dangerInactiveWhenNoZombiesOrFarAway() {
        assertFalse(DangerRedShader.isDangerActive(null));

        GameModel model = new GameModel(stubLevel());
        assertFalse(DangerRedShader.isDangerActive(model));

        ZombieInstance zombie = createZombie(5.0f, 2);
        model.getZombies().add(zombie);
        assertFalse(DangerRedShader.isDangerActive(model));

        zombie.setContinuousPosition(new FloatPoint(2.5f, 2));
        zombie.setGridX(2);
        assertFalse(DangerRedShader.isDangerActive(model));
    }

    @Test
    void dangerActiveWhenZombieInFirstTwoColumns() {
        GameModel model = new GameModel(stubLevel());
        ZombieInstance zombie = createZombie(1.8f, 2);
        model.getZombies().add(zombie);

        // Column 1 (< 2.0)
        assertTrue(DangerRedShader.isDangerActive(model));

        // Column 0 (< 1.0)
        zombie.setContinuousPosition(new FloatPoint(0.4f, 2));
        zombie.setGridX(0);
        assertTrue(DangerRedShader.isDangerActive(model));
    }

    @Test
    void dangerInactiveWhenZombieIsDyingHypnotizedOrBackward() {
        GameModel model = new GameModel(stubLevel());
        ZombieInstance zombie = createZombie(0.5f, 2);
        model.getZombies().add(zombie);

        assertTrue(DangerRedShader.isDangerActive(model));

        // Dying state
        zombie.setState(ZombieState.DYING);
        assertFalse(DangerRedShader.isDangerActive(model));

        // Hypnotized
        ZombieInstance hypnotizedZombie = createZombie(0.5f, 2);
        hypnotizedZombie.hypnotise();
        model.getZombies().clear();
        model.getZombies().add(hypnotizedZombie);
        assertFalse(DangerRedShader.isDangerActive(model));

        // Moving backward
        ZombieInstance backwardZombie = createZombie(0.5f, 2);
        backwardZombie.setMovingBackward(true);
        model.getZombies().clear();
        model.getZombies().add(backwardZombie);
        assertFalse(DangerRedShader.isDangerActive(model));

        // Dead / 0 HP
        ZombieInstance deadZombie = createZombie(0.5f, 2);
        deadZombie.takeDamage(9999);
        model.getZombies().clear();
        model.getZombies().add(deadZombie);
        assertFalse(DangerRedShader.isDangerActive(model));
    }

    @Test
    void dangerInactiveInIZombieMode() {
        IZombieLevel iZombieLevel = new IZombieLevel(stubConfig(), MiniGameType.I_ZOMBIE, 1);
        GameModel model = new GameModel(iZombieLevel);

        ZombieInstance zombie = createZombie(0.5f, 2);
        model.getZombies().add(zombie);

        assertFalse(DangerRedShader.isDangerActive(model));
    }

    @Test
    void updateIntensitySmoothTransitions() {
        float intensity = 0.0f;
        // Increases when danger is active
        intensity = DangerRedShader.updateIntensity(intensity, true, 0.1f);
        assertTrue(intensity > 0.0f);
        assertEquals(0.35f, intensity, 1e-4f);

        // Maxes out at 1.0
        intensity = DangerRedShader.updateIntensity(intensity, true, 1.0f);
        assertEquals(1.0f, intensity, 1e-4f);

        // Decreases when danger ends
        intensity = DangerRedShader.updateIntensity(intensity, false, 0.1f);
        assertEquals(0.75f, intensity, 1e-4f);

        // Bottoms out at 0.0
        intensity = DangerRedShader.updateIntensity(intensity, false, 1.0f);
        assertEquals(0.0f, intensity, 1e-4f);
    }

    private static ZombieInstance createZombie(float x, int y) {
        Zombie definition = new Zombie(
                "ZombieBasic", 100, 0.25f, 100f, ZombieSize.NORMAL,
                Chapter.ANCIENT_EGYPT, 100, 1, List.of(), null, null, List.of());
        ZombieInstance instance = new ZombieInstance(definition);
        instance.setContinuousPosition(new FloatPoint(x, y));
        instance.setGridPosition(new Point((int) Math.floor(x), y));
        instance.setState(ZombieState.WALKING);
        return instance;
    }

    private static LevelConfig stubConfig() {
        LevelConfig config = new LevelConfig();
        config.setChapter(Chapter.ANCIENT_EGYPT);
        config.setLevelId(1);
        config.setRows(5);
        config.setColumns(9);
        config.setLevelType(LevelType.NORMAL);
        config.setRules(new GameRules(
                true, true, 50, 1.0, 0, 99,
                Set.of(), Set.of(), Set.of()));
        config.setEndGameCondition(new NeverEndCondition());
        config.setWaves(Collections.emptyList());
        return config;
    }

    private static Level stubLevel() {
        return new Level(stubConfig()) {
            @Override public boolean canStart() { return true; }
            @Override public void onStart() {}
            @Override public void tick(float deltaTime) {}
            @Override public void onWaveCleared(int waveNumber) {}
            @Override public void onComplete() {}
            @Override public void onFail() {}
            @Override public boolean checkWinCondition(GameModel model) { return false; }
            @Override public boolean checkLossCondition(GameModel model) { return false; }
        };
    }
}
