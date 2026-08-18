package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
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
 * Pregame "Ready... / Set... / PLANT" sting. One word at a time, center of the
 * lawn, each growing in then yielding to the next; {@code PLANT} is larger
 * and fades out as play begins.
 */
public final class ReadySetPlantBanner extends Table {
    public static final String FONT = "FBUSV8C5EI_1_outline";
    static final String[] WORDS = {"Ready...", "Set...", "PLANT"};
    static final float[] PEAK_SCALE = {1f, 1f, 1.28f};

    static final Color RED = new Color(0.90f, 0.10f, 0.10f, 1f);
    private static final float START_SCALE = 0.48f;
    private static final float GROW_SEC = 0.50f;
    private static final float HOLD_SEC = 0.08f;
    private static final float SWAP_SEC = 0.16f;
    private static final float PLANT_GROW_SEC = 0.58f;
    private static final float PLANT_HOLD_SEC = 0.32f;
    private static final float PLANT_FADE_SEC = 0.36f;

    private final Label label;
    private final Container<Label> wrap;
    private int wordIndex = -1;
    private boolean playing;

    public ReadySetPlantBanner(Skin skin) {
        setFillParent(true);
        setTouchable(Touchable.disabled);
        center();
        BitmapFont font = SkinFonts.linear(skin, FONT);
        Label.LabelStyle style = new Label.LabelStyle(font, RED);
        label = new Label("", style);
        label.setAlignment(Align.center);
        wrap = new Container<>(label);
        wrap.setTransform(true);
        add(wrap);
        getColor().a = 1f;
    }

    public void play() {
        playing = true;
        wordIndex = 0;
        showWord();
    }

    public boolean isPlaying() {
        return playing;
    }

    private void showWord() {
        boolean last = wordIndex == WORDS.length - 1;
        label.setText(WORDS[wordIndex]);
        wrap.pack();
        wrap.setOrigin(wrap.getWidth() * 0.5f, wrap.getHeight() * 0.5f);
        wrap.setScale(START_SCALE);
        wrap.getColor().a = 1f;
        wrap.clearActions();
        float peak = PEAK_SCALE[wordIndex];
        if (last) {
            wrap.addAction(Actions.sequence(
                Actions.scaleTo(peak, peak, PLANT_GROW_SEC, Interpolation.fade),
                Actions.delay(PLANT_HOLD_SEC),
                Actions.fadeOut(PLANT_FADE_SEC),
                Actions.run(this::finish)));
            return;
        }
        wrap.addAction(Actions.sequence(
            Actions.scaleTo(peak, peak, GROW_SEC, Interpolation.fade),
            Actions.delay(HOLD_SEC),
            Actions.fadeOut(SWAP_SEC),
            Actions.run(this::nextWord)));
    }

    private void nextWord() {
        wordIndex++;
        showWord();
    }

    private void finish() {
        playing = false;
        wrap.getColor().a = 0f;
    }
}
