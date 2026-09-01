package view.gui.screen;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Fixed-size almanac tab slot. Face is top-aligned so ACTIVE vs DOWN never shifts.
 */
final class CollectionTabMarker extends Group {
    static final float SLOT_W = 80f;
    static final float SLOT_H = 106f;

    private final Image face = new Image();
    private final Image icon = new Image();

    CollectionTabMarker(TextureRegion iconRegion, float iconW, float iconH,
                        float iconX, float iconY, Runnable action) {
        setSize(SLOT_W, SLOT_H);
        face.setTouchable(Touchable.disabled);
        icon.setTouchable(Touchable.disabled);
        addActor(face);
        if (iconRegion != null) {
            icon.setDrawable(new TextureRegionDrawable(iconRegion));
            icon.setSize(iconW, iconH);
            icon.setPosition(iconX, iconY);
            addActor(icon);
        }
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (action != null) {
                    action.run();
                }
            }
        });
    }

    void setFace(TextureRegion region) {
        if (region == null) {
            return;
        }
        face.setDrawable(new TextureRegionDrawable(region));
        face.setSize(region.getRegionWidth(), region.getRegionHeight());
        face.setPosition(0f, SLOT_H - face.getHeight());
    }
}
