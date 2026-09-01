package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

/**
 * Square hub icon: PvzSkin brown chrome + atlas icon on top.
 * {@code iconScale} &gt; 1 makes the icon larger than the brown background.
 */
public final class SkinIconButton extends Stack {
    public static final float DEFAULT_ICON_SCALE = 1f;

    private final Label badge;
    private final float buttonSize;
    private final Table iconLayer;

    public SkinIconButton(Skin skin, TextureRegion icon, float size, Runnable action) {
        this(skin, icon, size, DEFAULT_ICON_SCALE, action);
    }

    /**
     * @param iconScale 1 = icon matches button size; e.g. 1.35 = 35% larger than brown chrome
     */
    public SkinIconButton(Skin skin, TextureRegion icon, float size, float iconScale, Runnable action) {
        buttonSize = size;
        TextButton.TextButtonStyle brown = skin.get("brown", TextButton.TextButtonStyle.class);

        Button.ButtonStyle chrome = new Button.ButtonStyle();
        chrome.up = brown.up;
        chrome.down = brown.down;
        chrome.over = brown.over;
        chrome.checked = brown.checked;

        Button background = new Button(chrome);
        background.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (action != null) {
                    action.run();
                }
            }
        });
        add(background);

        iconLayer = new Table();
        iconLayer.setTouchable(Touchable.disabled);
        setIcon(icon, iconScale);
        add(iconLayer);

        Table badgeLayer = new Table();
        badgeLayer.top().right();
        badgeLayer.setTouchable(Touchable.disabled);
        badge = new Label("", skin, "secondary");
        badge.setColor(Color.WHITE);
        badge.setVisible(false);
        badgeLayer.add(badge).pad(6f, 0f, 0f, 6f);
        add(badgeLayer);

        setSize(size, size);
    }

    public void setIcon(TextureRegion icon, float iconScale) {
        iconLayer.clear();
        if (icon != null) {
            float iconSize = buttonSize * Math.max(0.1f, iconScale);
            Image iconImage = new Image(new TextureRegionDrawable(icon));
            iconImage.setScaling(Scaling.fit);
            iconImage.setTouchable(Touchable.disabled);
            iconLayer.add(iconImage).size(iconSize, iconSize);
        }
    }

    public void setBadge(int unreadCount) {
        if (unreadCount <= 0) {
            badge.setVisible(false);
            badge.setText("");
            return;
        }
        badge.setVisible(true);
        badge.setText(unreadCount > 9 ? "9+" : "!" + unreadCount);
        badge.setColor(new Color(1f, 0.85f, 0.2f, 1f));
    }
}
