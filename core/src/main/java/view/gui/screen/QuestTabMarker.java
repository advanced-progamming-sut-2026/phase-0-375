package view.gui.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import view.gui.ui.SkinFonts;

final class QuestTabMarker extends Group {
    static final float SLOT_W = 139f;
    static final float SLOT_H = 86f;
    private static final Color LABEL_COLOR = new Color(1f, 0.96f, 0.88f, 1f);
    private static final float LABEL_W = 130f;
    private static final float LABEL_H = 28f;
    private static final float LABEL_X = 4.5f;
    private static final float LABEL_Y = 37f;
    private static final float LABEL_SCALE = 1.15f;

    private final Image face = new Image();
    private final Label caption;

    QuestTabMarker(Skin skin, String title, Runnable action) {
        setSize(SLOT_W, SLOT_H);
        face.setTouchable(Touchable.disabled);
        addActor(face);

        caption = new Label(title, skin, "medium");
        caption.setColor(LABEL_COLOR);
        caption.setAlignment(Align.center);
        caption.setTouchable(Touchable.disabled);
        SkinFonts.scaleLabel(caption, skin, "medium", LABEL_SCALE);
        caption.setSize(LABEL_W, LABEL_H);
        caption.setPosition(LABEL_X, LABEL_Y);
        addActor(caption);

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
