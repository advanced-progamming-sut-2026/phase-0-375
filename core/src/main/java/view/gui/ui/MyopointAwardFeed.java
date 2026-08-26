package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import model.game.score.MyopointTracker;

import java.util.List;

/**
 * Short-lived "+N Bonus!" toasts stacked under the Myopoint HUD.
 */
public final class MyopointAwardFeed extends Table {
    private static final float SHOW_SEC = 1.15f;
    private static final float FADE_SEC = 0.28f;
    private static final int MAX_VISIBLE = 4;
    private static final Color POINTS = new Color(0.55f, 1f, 0.45f, 1f);

    private final Skin skin;
    private final BitmapFont font;
    private int visible;

    public MyopointAwardFeed(Skin skin) {
        this.skin = skin;
        this.font = SkinFonts.outlined(skin, "medium");
        setTouchable(Touchable.disabled);
        top();
        defaults().padBottom(4f);
    }

    /** Shows floating toasts for stylish bonus awards (base kills are skipped). */
    public void push(List<MyopointTracker.AwardEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (MyopointTracker.AwardEvent event : events) {
            if (event == null || !event.isBonus() || event.points() <= 0) {
                continue;
            }
            pushOne(event);
        }
    }

    private void pushOne(MyopointTracker.AwardEvent event) {
        while (visible >= MAX_VISIBLE && getChildren().size > 0) {
            getChildren().first().remove();
            visible = Math.max(0, visible - 1);
        }

        Label.LabelStyle style = new Label.LabelStyle(font, POINTS);
        Label label = new Label(
            "+" + event.points() + "  " + MyopointTracker.toastLabel(event.key()),
            style);
        label.setAlignment(Align.center);
        label.setTouchable(Touchable.disabled);
        label.getColor().a = 0f;

        Table chip = new Table();
        chip.setTouchable(Touchable.disabled);
        var bg = UiDrawables.tryDrawable(skin, "image_ui_generic_counter_bg_10");
        if (bg != null) {
            chip.setBackground(bg);
        }
        chip.pad(6f, 18f, 6f, 18f);
        chip.add(label);
        chip.getColor().a = 0f;

        add(chip).center().row();
        visible++;

        chip.addAction(Actions.sequence(
            Actions.fadeIn(0.12f),
            Actions.delay(SHOW_SEC),
            Actions.fadeOut(FADE_SEC),
            Actions.run(() -> {
                chip.remove();
                visible = Math.max(0, visible - 1);
            })));
        label.addAction(Actions.fadeIn(0.12f));
    }
}
