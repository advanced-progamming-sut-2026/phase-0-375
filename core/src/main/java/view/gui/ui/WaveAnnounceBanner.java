package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

/**
 * Wave sting using the same red outline font as {@link ReadySetPlantBanner},
 * but starting large and shrinking away.
 */
public final class WaveAnnounceBanner extends Table {
    static final float START_SCALE = 1.45f;
    static final float END_SCALE = 0.78f;

    private static final float SHRINK_SEC = 0.85f;
    private static final float HOLD_SEC = 0.18f;
    private static final float FADE_SEC = 0.32f;

    private final Label label;
    private final Container<Label> wrap;
    private boolean playing;

    public WaveAnnounceBanner(Skin skin) {
        setFillParent(true);
        setTouchable(Touchable.disabled);
        center();
        BitmapFont font = SkinFonts.linear(skin, ReadySetPlantBanner.FONT);
        Label.LabelStyle style = new Label.LabelStyle(font, ReadySetPlantBanner.RED);
        label = new Label("", style);
        label.setAlignment(Align.center);
        wrap = new Container<>(label);
        wrap.setTransform(true);
        wrap.getColor().a = 0f;
        add(wrap);
    }

    public void show(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        playing = true;
        label.setText(text);
        wrap.pack();
        wrap.setOrigin(wrap.getWidth() * 0.5f, wrap.getHeight() * 0.5f);
        wrap.setScale(START_SCALE);
        wrap.getColor().a = 1f;
        wrap.clearActions();
        wrap.addAction(Actions.sequence(
            Actions.scaleTo(END_SCALE, END_SCALE, SHRINK_SEC, Interpolation.fade),
            Actions.delay(HOLD_SEC),
            Actions.fadeOut(FADE_SEC),
            Actions.run(() -> playing = false)));
    }

    public boolean isPlaying() {
        return playing;
    }
}
