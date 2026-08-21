package model.game.core;

import model.app.App;
import model.enums.Chapter;
import model.enums.LevelType;
import model.enums.ZombieSize;
import model.enums.ZombieState;
import model.game.level.Level;
import model.game.level.LevelConfig;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HouseBreachSpotlightTest {

    private User previousUser;

    @BeforeEach
    void setUser() {
        previousUser = App.getInstance().getCurrentUser();
        App.getInstance().setCurrentUser(new User());
    }

    @AfterEach
    void restoreUser() {
        App.getInstance().setCurrentUser(previousUser);
    }

    @Test
    void applyHouseBreachKeepsZombieEating() {
        GameModel model = new GameModel(stubLevel());
        ZombieInstance zombie = new ZombieInstance(new Zombie(
            "ZombieBasic", 100, 0.25f, 100f, ZombieSize.NORMAL,
            Chapter.ANCIENT_EGYPT, 100, 1, List.of(), null, null, List.of()));
        zombie.setContinuousPosition(new model.game.map.FloatPoint(GameModel.HOUSE_CHEW_X, 2));
        zombie.setState(ZombieState.WALKING);

        model.applyHouseBreach(zombie, 2);

        assertTrue(model.isHouseBreached());
        assertTrue(model.getBreachedRows().contains(2));
        assertSame(zombie, model.getBreachingZombie());
        assertTrue(zombie.isEating());
        assertEquals(GameModel.HOUSE_CHEW_X, zombie.getContinuousX(), 1e-5f);
    }

    @Test
    void recordLastZombieDeathStoresCoords() {
        GameModel model = new GameModel(stubLevel());
        model.recordLastZombieDeath(4.5f, 1f);
        assertEquals(4.5f, model.getLastZombieDeathX(), 1e-5f);
        assertEquals(1f, model.getLastZombieDeathY(), 1e-5f);
    }

    private static Level stubLevel() {
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
        return new Level(config) {
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
