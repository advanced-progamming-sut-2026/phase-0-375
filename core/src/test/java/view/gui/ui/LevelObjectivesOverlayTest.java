package view.gui.ui;

import model.enums.LevelType;
import model.game.level.LevelConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelObjectivesOverlayTest {

    @Test
    void deadLineObjectiveMatchesPauseCopy() {
        LevelConfig config = new LevelConfig();
        config.setLevelType(LevelType.DEAD_LINE);
        List<String> lines = LevelObjectivesOverlay.objectivesFor(config);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).toLowerCase().contains("dead line"));
    }

    @Test
    void sunflowerTiltsClockwiseToTheRight() {
        assertEquals(-15f, PauseMenuOverlay.SUNFLOWER_TILT_DEG, 0.001f);
    }

    @Test
    void timedWarObjectiveDisplaysKillsAndSeconds() {
        LevelConfig config = new LevelConfig();
        config.setLevelType(LevelType.TIMED_WAR);
        model.game.rule.GameRules rules = new model.game.rule.GameRules(
                false, false, 150, 1.0, 0, 99,
                java.util.Set.of(), java.util.Set.of(), java.util.Set.of());
        rules.setTimedWarTargetKills(10);
        rules.setTimedWarDecayInterval(5.0f);
        config.setRules(rules);

        List<String> lines = LevelObjectivesOverlay.objectivesFor(config);
        assertEquals(1, lines.size());
        assertEquals("Defeat 10 zombies in 5 seconds", lines.get(0));
    }
}
