package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import model.game.core.GameModel;
import model.game.wave.WaveManager;
import pvz.libpvz.textures.TextureBank;

import java.util.ArrayList;
import java.util.List;

/**
 * Center-top wave meter: skin {@code ingame_progress} ProgressBar.
 * Head walks right→left; green fills behind it via {@code knobAfter}.
 * Bar value is {@code 1 - progress} so at progress 0 the after-fill is empty.
 */
public final class WaveProgressHud extends WidgetGroup {
    public static final String STYLE = "ingame_progress";
    public static final String FLAG_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FLAG_DEFAULT";
    public static final String POLE_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_FLAG_POLE";
    public static final String HEAD_ID = "IMAGE_UI_HUD_INGAME_PROGRESS_METER_ZOMBIEHEAD";

    /** Native 768 atlas sizes. */
    public static final float BAR_W = 273f;
    static final float BAR_H = 33f;
    static final float HEAD_W = 42f;
    static final float HEAD_H = 45f;
    static final float POLE_W = 29f;
    static final float POLE_H = 38f;
    static final float FLAG_W = 27f;
    static final float FLAG_H = 22f;
    /** Rounded end caps so fill/head stay inside the meter. */
    static final float CAP = 16f;
    /** Visible pole half-width inside the {@link #POLE_W} bbox. */
    static final float POLE_VIS_HALF_W = 4.5f;
    /** Transparent padding on the left of the flag art. */
    static final float FLAG_TRIM_L = 6f;

    private final ProgressBar bar;
    private final Image head;
    private final TextureBank textures;
    private final List<Image> poles = new ArrayList<>();
    private final List<Image> flags = new ArrayList<>();

    public WaveProgressHud(Skin skin, TextureBank textures) {
        this.textures = textures;
        setSize(BAR_W, HEAD_H);
        setTouchable(Touchable.disabled);

        // Head starts on the right (value=1). Green is knobAfter (right of the
        // knob): empty at start, grows as value drops with progress. Skin fill
        // often lives on style.knob — promote it to knobAfter, then hide knob.
        ProgressBarStyle style = new ProgressBarStyle(skin.get(STYLE, ProgressBarStyle.class));
        ensureKnobAfterFill(style);
        style.knob = null;
        style.knobBefore = null;

        bar = new ProgressBar(0f, 1f, 0.001f, false, style);
        bar.setAnimateDuration(0f);
        bar.setRound(false);
        bar.setValue(1f);
        bar.setTouchable(Touchable.disabled);
        bar.setSize(BAR_W, BAR_H);
        bar.setPosition(0f, barY());
        addActor(bar);

        head = new Image();
        head.setTouchable(Touchable.disabled);
        addActor(head);
        bindImage(head, HEAD_ID, HEAD_W, HEAD_H);
    }

    private GameModel lastModel;
    private float maxProgress = 0f;

    public void sync(GameModel model) {
        if (model != lastModel) {
            lastModel = model;
            maxProgress = 0f;
        }
        if (model == null || model.getWaveManager() == null) {
            setVisible(false);
            return;
        }
        WaveManager waves = model.getWaveManager();
        if (waves.getTotalWaveCount() <= 0) {
            setVisible(false);
            return;
        }
        setVisible(true);
        float raw = waves.progress01();
        if (raw > maxProgress) {
            maxProgress = raw;
        }
        bar.setValue(1f - maxProgress);
        float[] stops = waves.flagStops01();
        ensureFlags(stops.length);
        layoutDecorations(maxProgress, stops);
    }

    public static boolean showFor(GameModel model) {
        return model != null
            && model.getWaveManager() != null
            && model.getWaveManager().getTotalWaveCount() > 0;
    }

    static float stopCenterX(float stop) {
        return CAP + (1f - clamp01(stop)) * (BAR_W - 2f * CAP);
    }

    static float headCenterX(float progress) {
        float trackLeft = CAP;
        float trackRight = BAR_W - CAP;
        return trackRight - clamp01(progress) * (trackRight - trackLeft);
    }

    /**
     * Pole centered on the section boundary; flag's cloth attaches to the
     * visible top-right of the pole (not the transparent bbox edge).
     */
    static void layoutFlag(float stop, Rectangle pole, Rectangle flag) {
        float stopX = stopCenterX(stop);
        float y = barY();
        pole.set(stopX - POLE_W * 0.5f, y + (BAR_H - POLE_H) * 0.5f, POLE_W, POLE_H);
        flag.set(
            stopX + POLE_VIS_HALF_W - FLAG_TRIM_L,
            pole.y + pole.height - FLAG_H,
            FLAG_W,
            FLAG_H);
    }

    static void layoutHead(float progress, Rectangle head) {
        float y = barY();
        float x = headCenterX(progress);
        head.set(x - HEAD_W * 0.5f, y + (BAR_H - HEAD_H) * 0.5f, HEAD_W, HEAD_H);
    }

    private static float barY() {
        return (HEAD_H - BAR_H) * 0.5f;
    }

    private void layoutDecorations(float progress, float[] stops) {
        Rectangle pole = new Rectangle();
        Rectangle flag = new Rectangle();
        for (int i = 0; i < poles.size(); i++) {
            float stop = i < stops.length ? stops[i] : 1f;
            layoutFlag(stop, pole, flag);
            poles.get(i).setBounds(pole.x, pole.y, pole.width, pole.height);
            flags.get(i).setBounds(flag.x, flag.y, flag.width, flag.height);
        }
        Rectangle headRect = new Rectangle();
        layoutHead(progress, headRect);
        head.setBounds(headRect.x, headRect.y, headRect.width, headRect.height);
        head.toFront();
    }

    private void ensureFlags(int count) {
        while (poles.size() < count) {
            Image pole = new Image();
            pole.setTouchable(Touchable.disabled);
            bindImage(pole, POLE_ID, POLE_W, POLE_H);
            addActor(pole);
            poles.add(pole);

            Image flag = new Image();
            flag.setTouchable(Touchable.disabled);
            bindImage(flag, FLAG_ID, FLAG_W, FLAG_H);
            addActor(flag);
            flags.add(flag);
        }
        for (int i = 0; i < poles.size(); i++) {
            boolean on = i < count;
            poles.get(i).setVisible(on);
            flags.get(i).setVisible(on);
        }
    }

    private void bindImage(Image image, String id, float w, float h) {
        TextureRegion region = textures == null ? null : textures.region(id);
        if (region != null) {
            image.setDrawable(new TextureRegionDrawable(region));
            if (region.getRegionWidth() > 0) {
                image.setSize(region.getRegionWidth(), region.getRegionHeight());
            } else {
                image.setSize(w, h);
            }
        } else {
            image.setSize(w, h);
        }
    }

    /**
     * PvZ meter fills behind the head (right side) → {@link ProgressBarStyle#knobAfter}.
     * If the skin only set {@code knob} / {@code knobBefore}, promote that drawable.
     */
    static void ensureKnobAfterFill(ProgressBarStyle style) {
        if (style == null || style.knobAfter != null) {
            return;
        }
        if (style.knob != null) {
            style.knobAfter = style.knob;
        } else if (style.knobBefore != null) {
            style.knobAfter = style.knobBefore;
        }
    }

    private static float clamp01(float v) {
        if (v < 0f) {
            return 0f;
        }
        if (v > 1f) {
            return 1f;
        }
        return v;
    }

    @Override
    public float getPrefWidth() {
        return BAR_W;
    }

    @Override
    public float getPrefHeight() {
        return HEAD_H;
    }
}
