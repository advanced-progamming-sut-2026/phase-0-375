package view.gui.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import pvz.skin.BorderedTable;

/**
 * Reusable menu row/card. Call {@link #setArt(Drawable)} later for chapter/level/plant atlas art
 * without changing screen layout code.
 */
public final class SelectableMenuCard extends BorderedTable {
    private final Label title;
    private final Label subtitle;
    private final TextButton action;
    private Drawable art;

    public SelectableMenuCard(Skin skin, String titleText, String subtitleText, String actionText) {
        pad(16f);
        title = new Label(titleText, skin, "medium");
        subtitle = new Label(subtitleText == null ? "" : subtitleText, skin, "secondary");
        subtitle.setWrap(true);
        action = new TextButton(actionText, skin, "purple");
        rebuild();
    }

    private void rebuild() {
        clearChildren();
        if (art != null) {
            Table frame = new Table();
            frame.setBackground(art);
            add(frame).size(72f, 72f).padRight(12f);
        }
        Table text = new Table();
        text.add(title).left().growX().row();
        text.add(subtitle).left().growX().padTop(4f);
        add(text).growX().padRight(12f);
        add(action).width(140f).height(48f);
    }

    public void setArt(Drawable drawable) {
        this.art = drawable;
        rebuild();
    }

    public void setSubtitle(String value) {
        subtitle.setText(value == null ? "" : value);
    }

    public void setActionEnabled(boolean enabled) {
        action.setDisabled(!enabled);
    }

    public void setActionText(String value) {
        action.setText(value);
    }

    public void onAction(Runnable runnable) {
        action.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!action.isDisabled() && runnable != null) {
                    runnable.run();
                }
            }
        });
    }
}
