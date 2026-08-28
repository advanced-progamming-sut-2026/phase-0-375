package model.game.core;

import controller.GameplayMenuController;
import model.app.App;
import model.enums.Chapter;
import model.enums.GameState;
import model.enums.LevelType;
import model.enums.LootPickupKind;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.rule.GameRules;
import model.game.rule.NeverEndCondition;
import model.item.LootPickup;
import model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoinPersistenceTest {

    private User testUser;
    private User previousUser;

    @BeforeEach
    void setUp() {
        previousUser = App.getInstance().getCurrentUser();
        testUser = new User();
        testUser.setUsername("coinTester");
        testUser.setCoins(500);
        testUser.setGems(10);
        testUser.setUnlockedPots(2);
        App.getInstance().setCurrentUser(testUser);
    }

    @AfterEach
    void tearDown() {
        App.getInstance().setCurrentUser(previousUser);
        App.getInstance().setCurrentGameModel(null);
        App.getInstance().setCurrentGameLoop(null);
    }

    @Test
    void addCoinsUpdatesUserTotalAndModelCount() {
        GameModel model = new GameModel(stubLevel());
        model.addCoins(50);

        assertEquals(50, model.getCoinCount());
        assertEquals(550, testUser.getCoins());
    }

    @Test
    void applyLootPickupUpdatesCoinsDiamondsAndPots() {
        GameModel model = new GameModel(stubLevel());

        model.applyLootPickup(new LootPickup(LootPickupKind.COIN_GOLD, 50, 0, 0));
        assertEquals(550, testUser.getCoins());

        model.applyLootPickup(new LootPickup(LootPickupKind.COIN_SILVER, 50, 0, 0));
        assertEquals(600, testUser.getCoins());

        model.applyLootPickup(new LootPickup(LootPickupKind.DIAMOND, 1, 0, 0));
        assertEquals(11, testUser.getGems());

        model.applyLootPickup(new LootPickup(LootPickupKind.FLOWER_POT, 1, 0, 0));
        assertEquals(3, testUser.getUnlockedPots());
    }

    @Test
    void finishDrainsActiveLootOnWinOrLoss() {
        GameModel model = new GameModel(stubLevel());
        model.spawnLootPickup(new LootPickup(LootPickupKind.COIN_GOLD, 50, 1, 1));
        model.spawnLootPickup(new LootPickup(LootPickupKind.COIN_SILVER, 50, 2, 2));

        PvZGameLoop loop = new PvZGameLoop(model);
        loop.setGameState(GameState.LOST);
        for (LootPickup loot : new java.util.ArrayList<>(model.getActiveLootPickups())) {
            model.applyLootPickup(loot);
            model.removeLootPickup(loot);
        }

        assertEquals(600, testUser.getCoins());
        assertEquals(0, model.getActiveLootPickups().size());
    }

    @Test
    void menuExitDrainsActiveLootOnQuitMidGame() {
        GameModel model = new GameModel(stubLevel());
        model.spawnLootPickup(new LootPickup(LootPickupKind.COIN_GOLD, 100, 1, 1));
        App.getInstance().setCurrentGameModel(model);

        GameplayMenuController.getInstance().menuExit();

        assertEquals(600, testUser.getCoins());
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
