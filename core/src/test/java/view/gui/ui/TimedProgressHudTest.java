package view.gui.ui;

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import model.app.App;
import model.enums.Chapter;
import model.enums.LevelType;
import model.game.core.GameModel;
import model.game.level.LevelConfig;
import model.game.level.RegularLevel;
import model.game.level.special.TimedWarLevel;
import model.game.rule.GameRules;
import model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TimedProgressHudTest {

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

    @Test
    void ensureKnobBeforeFillPromotesKnob() {
        ProgressBarStyle style = new ProgressBarStyle();
        style.knob = new TextureRegionDrawable();
        style.knobBefore = null;
        TimedProgressHud.ensureKnobBeforeFill(style);
        assertNotNull(style.knobBefore);
        assertSame(style.knob, style.knobBefore);
    }

    @Test
    void ensureKnobBeforeFillPromotesKnobAfterIfKnobBeforeNull() {
        ProgressBarStyle style = new ProgressBarStyle();
        TextureRegionDrawable knobAfter = new TextureRegionDrawable();
        style.knobAfter = knobAfter;
        style.knobBefore = null;
        TimedProgressHud.ensureKnobBeforeFill(style);
        assertNotNull(style.knobBefore);
        assertSame(knobAfter, style.knobBefore);
    }

    @Test
    void testShowForTimedWarLevel() {
        GameRules rules = new GameRules(
                false, false, 150, 1.0, 0, 99,
                Set.of(), Set.of(), Set.of());
        rules.setTimedWarLimit(180f);
        rules.setTimedWarTargetKills(20);

        LevelConfig config = new LevelConfig();
        config.setChapter(Chapter.DARK_AGES);
        config.setLevelId(3);
        config.setLevelType(LevelType.TIMED_WAR);
        config.setRules(rules);
        config.setRows(5);
        config.setColumns(9);

        TimedWarLevel level = new TimedWarLevel(config);
        GameModel model = new GameModel(level);

        assertTrue(TimedProgressHud.showFor(model));
    }

    @Test
    void testShowForNonTimedLevelReturnsFalse() {
        LevelConfig config = new LevelConfig();
        config.setChapter(Chapter.DARK_AGES);
        config.setLevelId(1);
        config.setLevelType(LevelType.NORMAL);
        config.setRules(new GameRules(
                false, false, 150, 1.0, 0, 99,
                Set.of(), Set.of(), Set.of()));
        config.setRows(5);
        config.setColumns(9);

        RegularLevel level = new RegularLevel(config);
        GameModel model = new GameModel(level);
        assertFalse(TimedProgressHud.showFor(model));
    }

    @Test
    void testHeadOffsetCanBeTweaked() {
        TimedProgressHud hud = new TimedProgressHud(null, null);
        assertEquals(TimedProgressHud.DEFAULT_HEAD_OFFSET_X, hud.getHeadOffsetX(), 0.001f);

        hud.setHeadOffsetX(-20f);
        assertEquals(-20f, hud.getHeadOffsetX(), 0.001f);
        assertEquals(-20f, hud.getHead().getX(), 0.001f);
    }
}
