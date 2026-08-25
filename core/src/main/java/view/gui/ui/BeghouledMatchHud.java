package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import model.game.core.GameModel;
import model.game.level.minigame.beghouled.BeghouledLevel;

/**
 * Top-center match progress for Beghouled.
 */
public final class BeghouledMatchHud extends Table {
    private final Label label;

    public BeghouledMatchHud(Skin skin) {
        setTouchable(Touchable.disabled);
        BitmapFont font = SkinFonts.outlined(skin, "medium");
        Label.LabelStyle style = new Label.LabelStyle(font, Color.WHITE);
        label = new Label("Matches 0/0", style);
        label.setAlignment(Align.center);
        label.setTouchable(Touchable.disabled);
        add(label).pad(6f, 16f, 6f, 16f);
    }

    public void sync(GameModel model) {
        if (!(model != null && model.getCurrentLevel() instanceof BeghouledLevel beghouled)) {
            setVisible(false);
            return;
        }
        setVisible(true);
        int made = beghouled.getMatchesMade();
        int target = beghouled.getSettings().getMatchTarget();
        label.setText("Matches " + made + "/" + target);
    }

    public static boolean showFor(GameModel model) {
        return model != null && model.getCurrentLevel() instanceof BeghouledLevel;
    }
}
