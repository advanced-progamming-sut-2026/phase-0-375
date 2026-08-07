package view.gui.ui;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;

/**
 * Temporary top-of-screen notification (phase 2 suggestion for menu errors).
 */
public final class ToastBanner extends Table {
    private static final float SHOW_SECONDS = 3.2f;

    private final Label label;

    public ToastBanner(Skin skin) {
        setFillParent(true);
        top();
        padTop(24f);
        setTouchable(Touchable.disabled);

        label = new Label("", skin, "medium");
        label.setAlignment(Align.center);
        label.setWrap(true);
        label.getColor().a = 0f;

        Table chip = new Table();
        Drawable bg = UiDrawables.tryDrawable(skin, "image_ui_generic_counter_bg_10");
        if (bg != null) {
            chip.setBackground(bg);
        }
        chip.pad(12f, 24f, 12f, 24f);
        chip.add(label).width(720f);
        add(chip).top();
    }

    public void clearMessage() {
        label.clearActions();
        label.setText("");
        label.getColor().a = 0f;
    }

    public void show(String message, boolean error) {
        if (message == null || message.isBlank()) {
            return;
        }
        label.clearActions();
        label.setText(message);
        if (error) {
            label.setColor(1f, 0.35f, 0.35f, 0f);
        } else {
            label.setColor(1f, 1f, 1f, 0f);
        }
        label.addAction(Actions.sequence(
                Actions.fadeIn(0.15f),
                Actions.delay(SHOW_SECONDS),
                Actions.fadeOut(0.35f)
        ));
    }
}
