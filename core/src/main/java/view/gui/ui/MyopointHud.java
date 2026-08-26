package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import model.game.core.GameModel;
import model.game.level.special.ScoreLevel;
import model.game.score.MyopointTracker;

/**
 * Top-center Myopoint total + combo for the daily Score Game.
 */
public final class MyopointHud extends Table {
    private static final Color COMBO_COLOR = new Color(1f, 0.92f, 0.35f, 1f);

    private final Label scoreLabel;
    private final Label comboLabel;

    public MyopointHud(Skin skin) {
        setTouchable(Touchable.disabled);
        BitmapFont font = SkinFonts.outlined(skin, "medium");
        Label.LabelStyle scoreStyle = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle comboStyle = new Label.LabelStyle(font, COMBO_COLOR);

        scoreLabel = new Label("Myopoints 0", scoreStyle);
        scoreLabel.setAlignment(Align.center);
        scoreLabel.setTouchable(Touchable.disabled);

        comboLabel = new Label("", comboStyle);
        comboLabel.setAlignment(Align.center);
        comboLabel.setTouchable(Touchable.disabled);
        comboLabel.setVisible(false);

        add(scoreLabel).pad(4f, 16f, 2f, 16f).row();
        add(comboLabel).pad(0f, 16f, 4f, 16f);
    }

    public void sync(GameModel model) {
        MyopointTracker tracker = trackerOf(model);
        if (tracker == null) {
            setVisible(false);
            return;
        }
        setVisible(true);
        scoreLabel.setText("Myopoints " + tracker.getTotalPoints());
        int combo = tracker.getComboStreak();
        if (combo >= 2) {
            comboLabel.setText("Combo x" + combo);
            comboLabel.setVisible(true);
        } else {
            comboLabel.setText("");
            comboLabel.setVisible(false);
        }
    }

    public static boolean showFor(GameModel model) {
        return trackerOf(model) != null;
    }

    private static MyopointTracker trackerOf(GameModel model) {
        if (model == null) {
            return null;
        }
        if (model.getCurrentLevel() instanceof ScoreLevel scoreLevel) {
            return scoreLevel.getTracker();
        }
        return model.getMyopointTracker();
    }
}
