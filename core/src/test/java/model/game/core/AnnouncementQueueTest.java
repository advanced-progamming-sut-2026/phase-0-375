package model.game.core;

import model.app.App;
import model.enums.Chapter;
import model.enums.LevelType;
import model.game.level.Level;
import model.game.level.LevelConfig;
import model.game.rule.GameRules;
import model.game.rule.NeverEndCondition;
import model.game.systems.ChapterEffectsSystem;
import model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnnouncementQueueTest {

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
    void drainsAnnouncementsFifo() {
        GameModel model = new GameModel(stubLevel());
        model.enqueueAnnouncement("Wave 1 started.");
        model.enqueueAnnouncement(ChapterEffectsSystem.NECROMANCY_ANNOUNCE);
        model.enqueueAnnouncement(ChapterEffectsSystem.LOW_TIDE_ANNOUNCE);

        assertEquals("Wave 1 started.", model.consumeWaveAnnouncement());
        assertEquals("Necromancy!", model.consumeWaveAnnouncement());
        assertEquals("Low Tide!", model.consumeWaveAnnouncement());
        assertNull(model.consumeWaveAnnouncement());
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
