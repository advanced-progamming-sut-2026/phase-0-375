package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Button from complete atlas HUD art (up/down). Uses {@link Button}, not
 * {@link com.badlogic.gdx.scenes.scene2d.ui.ImageButton}, so the region is
 * drawn once — ImageButton would also draw {@code imageUp} on top of {@code up}.
 */
public final class AtlasImageButton extends Button {
    public AtlasImageButton(TextureRegion up, TextureRegion down, float size, Runnable action) {
        this(up, down, size, size, action);
    }

    public AtlasImageButton(TextureRegion up, TextureRegion down, float width, float height,
                            Runnable action) {
        super(styleFor(up, down));
        setSize(width, height);
        addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (action != null) {
                    action.run();
                }
            }
        });
    }

    private static ButtonStyle styleFor(TextureRegion up, TextureRegion down) {
        ButtonStyle style = new ButtonStyle();
        if (up != null) {
            style.up = new TextureRegionDrawable(up);
        }
        if (down != null) {
            style.down = new TextureRegionDrawable(down);
        } else {
            style.down = style.up;
        }
        return style;
    }
}
