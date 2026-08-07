package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import pvz.skin.BorderedTable;

/**
 * Dimmed overlay + {@link BorderedTable} card for forgot-password and similar flows.
 */
public final class ModalCard {
    private static Texture pixel;

    private ModalCard() {}

    public static Table create(Skin skin, String title, Actor body, Runnable onClose) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(new TextureRegionDrawable(whitePixel()).tint(new Color(0f, 0f, 0f, 0.55f)));

        BorderedTable card = new BorderedTable();
        Label heading = new Label(title, skin, "big");
        card.add(heading).padBottom(16f).row();
        card.add(body).growX().padBottom(16f).row();

        TextButton close = new TextButton("Close", skin, "brown");
        close.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (onClose != null) {
                    onClose.run();
                }
                overlay.remove();
            }
        });
        card.add(close).width(180f).height(56f);
        overlay.add(card).width(560f).pad(24f);
        return overlay;
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
