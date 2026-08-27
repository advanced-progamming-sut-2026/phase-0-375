package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import pvz.skin.BorderedTable;

/**
 * Dimmed overlay + {@link BorderedTable} card for forgot-password and similar flows.
 */
public final class ModalCard {
    private static final float FADE_IN = 0.20f;
    private static final float FADE_OUT = 0.17f;
    private static Texture pixel;

    private ModalCard() {}

    public static Table create(Skin skin, String title, Actor body, Runnable onClose) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(new TextureRegionDrawable(whitePixel()).tint(new Color(0f, 0f, 0f, 0.55f)));

        BorderedTable card = new BorderedTable();
        Label heading = new Label(title, skin, "big");
        heading.setColor(Color.BLACK);
        card.add(heading).padBottom(16f).row();
        card.add(body).growX().padBottom(16f).row();

        TextButton close = new TextButton("Close", skin, "brown");
        close.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dismiss(overlay, onClose);
            }
        });
        card.add(close).width(180f).height(56f);
        overlay.add(card).width(560f).pad(24f);
        fadeIn(overlay);
        return overlay;
    }

    /**
     * Yes/Cancel confirm overlay. {@code onConfirm} runs after the overlay has faded out.
     */
    public static Table confirm(Skin skin, String title, String message, Runnable onConfirm) {
        return confirm(skin, title, message, "Yes", onConfirm);
    }

    public static Table confirm(Skin skin, String title, String message, String yesLabel,
                                Runnable onConfirm) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(new TextureRegionDrawable(whitePixel()).tint(new Color(0f, 0f, 0f, 0.55f)));

        BorderedTable card = new BorderedTable();
        Label heading = new Label(title, skin, "big");
        heading.setColor(Color.BLACK);
        heading.setAlignment(Align.center);
        card.add(heading).padBottom(12f).row();

        Label body = new Label(message, skin, "medium");
        body.setColor(Color.BLACK);
        body.setWrap(true);
        body.setAlignment(Align.center);
        card.add(body).width(440f).padBottom(20f).row();

        Table actions = new Table();
        TextButton cancel = new TextButton("Cancel", skin, "brown");
        cancel.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dismiss(overlay, null);
            }
        });
        TextButton yes = new TextButton(yesLabel != null ? yesLabel : "Yes", skin, "purple");
        yes.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dismiss(overlay, onConfirm);
            }
        });
        actions.add(cancel).width(160f).height(56f).padRight(12f);
        actions.add(yes).width(160f).height(56f);
        card.add(actions);

        overlay.add(card).width(520f).pad(24f);
        fadeIn(overlay);
        return overlay;
    }

    private static void fadeIn(Table overlay) {
        overlay.getColor().a = 0f;
        overlay.addAction(Actions.fadeIn(FADE_IN));
    }

    private static void dismiss(Table overlay, Runnable after) {
        overlay.setTouchable(Touchable.disabled);
        overlay.clearActions();
        overlay.addAction(Actions.sequence(
            Actions.fadeOut(FADE_OUT),
            Actions.run(() -> {
                overlay.remove();
                if (after != null) {
                    after.run();
                }
            })
        ));
    }

    private static Texture whitePixel() {
        if (pixel == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            pixel = new Texture(pixmap);
            pixmap.dispose();
        }
        return pixel;
    }
}
