package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import model.enums.LevelType;
import model.game.core.GameModel;
import model.game.level.Level;
import model.game.level.special.TimedWarLevel;
import pvz.libpvz.textures.TextureBank;

/**
 * Progress meter for timed levels showing zombie kills towards the goal.
 * Uses the {@code timed_progress} ProgressBar style from PvZ skin (with fallback to {@code ingame_progress})
 * and places {@code IMAGE_UI_HUD_INGAME_CHALLENGE_ZOMBIE_HEAD} over the leftmost part of the bar.
 */
public final class TimedProgressHud extends WidgetGroup {
    public static final String STYLE = "timed_progress";
    public static final String HEAD_ID = "IMAGE_UI_HUD_INGAME_CHALLENGE_ZOMBIE_HEAD";

    /** Native atlas sizes: bar 25% wider (106f), head (26x27). */
    public static final float BAR_W = 106f;
    public static final float BAR_H = 33f;
    public static final float HEAD_W = 26f;
    public static final float HEAD_H = 27f;

    /** Default X-axis offset for the zombie head on the leftmost part of the bar (tweakable in code). */
    public static final float DEFAULT_HEAD_OFFSET_X = 8f;

    private final ProgressBar bar;
    private final Image head;
    private final TextureBank textures;
    private float headOffsetX = DEFAULT_HEAD_OFFSET_X;

    public TimedProgressHud(Skin skin, TextureBank textures) {
        this.textures = textures;
        setSize(BAR_W, Math.max(BAR_H, HEAD_H));
        setTouchable(Touchable.disabled);

        ProgressBarStyle style = resolveStyle(skin);
        bar = new ProgressBar(0f, 1f, 0.001f, false, style);
        bar.setAnimateDuration(0f);
        bar.setRound(false);
        bar.setValue(0f);
        bar.setTouchable(Touchable.disabled);
        bar.setSize(BAR_W, BAR_H);
        bar.setPosition(0f, barY());
        addActor(bar);

        head = new Image();
        head.setTouchable(Touchable.disabled);
        bindHeadImage(skin, head);
        addActor(head);

        layoutHead();
    }

    public static final String TIMED_PROGRESS_BG = "image_ui_hud_ingame_zomboss_progress_meter_10";
    public static final String TIMED_PROGRESS_FILL = "image_ui_hud_ingame_progress_meter_fill_zomboss_bonus_10";

    private ProgressBarStyle resolveStyle(Skin skin) {
        if (skin != null) {
            if (skin.has(STYLE, ProgressBarStyle.class)) {
                ProgressBarStyle style = new ProgressBarStyle(skin.get(STYLE, ProgressBarStyle.class));
                ensureKnobBeforeFill(style);
                style.knob = null;
                return style;
            }
            if (skin.has(TIMED_PROGRESS_BG, Drawable.class) || skin.has(TIMED_PROGRESS_FILL, Drawable.class)) {
                ProgressBarStyle style = new ProgressBarStyle();
                if (skin.has(TIMED_PROGRESS_BG, Drawable.class)) {
                    style.background = skin.getDrawable(TIMED_PROGRESS_BG);
                }
                if (skin.has(TIMED_PROGRESS_FILL, Drawable.class)) {
                    style.knobBefore = skin.getDrawable(TIMED_PROGRESS_FILL);
                }
                style.knob = null;
                return style;
            }
        }
        ProgressBarStyle style = new ProgressBarStyle();
        return style;
    }

    static void ensureKnobBeforeFill(ProgressBarStyle style) {
        if (style == null) return;
        if (style.knobBefore == null) {
            if (style.knobAfter != null) {
                style.knobBefore = style.knobAfter;
                style.knobAfter = null;
            } else if (style.knob != null) {
                style.knobBefore = style.knob;
            }
        }
    }

    private void bindHeadImage(Skin skin, Image image) {
        TextureRegion region = textures == null ? null : textures.region(HEAD_ID);
        if (region != null) {
            image.setDrawable(new TextureRegionDrawable(region));
            float w = region.getRegionWidth() > 0 ? region.getRegionWidth() : HEAD_W;
            float h = region.getRegionHeight() > 0 ? region.getRegionHeight() : HEAD_H;
            image.setSize(w, h);
            return;
        }

        Drawable skinDrawable = UiDrawables.tryNamed(skin, "image_ui_hud_ingame_challenge_zombie_head");
        if (skinDrawable == null) {
            skinDrawable = UiDrawables.tryNamed(skin, "image_ui_hud_ingame_progress_meter_zombiehead");
        }
        if (skinDrawable != null) {
            image.setDrawable(skinDrawable);
        }
        image.setSize(HEAD_W, HEAD_H);
    }

    public void setProgress(float progress01) {
        bar.setValue(Math.min(1f, Math.max(0f, progress01)));
    }

    /** Match timer for networked I, Zombie (elapsed fraction). */
    public void syncMatchTimer(float timeRemaining, float matchDuration) {
        setVisible(true);
        float duration = matchDuration > 0f ? matchDuration : 1f;
        float elapsed = Math.max(0f, duration - Math.max(0f, timeRemaining));
        setProgress(Math.min(1f, elapsed / duration));
    }

    public void setBarWidth(float width) {
        if (width <= 0f) {
            return;
        }
        setSize(width, Math.max(BAR_H, HEAD_H));
        bar.setSize(width, BAR_H);
        bar.setPosition(0f, barY());
        layoutHead();
    }

    public float getProgress() {
        return bar.getValue();
    }

    public void sync(GameModel model) {
        if (!showFor(model)) {
            setVisible(false);
            return;
        }
        setVisible(true);
        float progress = 0f;
        Level level = model.getCurrentLevel();
        if (level instanceof TimedWarLevel timedWar) {
            progress = timedWar.getProgress01();
        } else if (level != null && level.getConfig() != null && level.getConfig().getRules() != null) {
            int target = level.getConfig().getRules().getTimedWarTargetKills();
            if (target > 0) {
                progress = Math.min(1f, Math.max(0f, (float) model.getZombiesKilled() / target));
            }
        }
        setProgress(progress);
    }

    public static boolean showFor(GameModel model) {
        if (model == null || model.getCurrentLevel() == null) {
            return false;
        }
        Level level = model.getCurrentLevel();
        if (level instanceof TimedWarLevel) {
            return true;
        }
        return level.getConfig() != null
                && level.getConfig().getLevelType() == LevelType.TIMED_WAR
                && level.getConfig().getRules() != null
                && level.getConfig().getRules().getTimedWarTargetKills() > 0;
    }

    public float getHeadOffsetX() {
        return headOffsetX;
    }

    public void setHeadOffsetX(float headOffsetX) {
        this.headOffsetX = headOffsetX;
        layoutHead();
    }

    private void layoutHead() {
        float headW = head.getWidth() > 0 ? head.getWidth() : HEAD_W;
        float headH = head.getHeight() > 0 ? head.getHeight() : HEAD_H;
        float headY = barY() + (BAR_H - headH) * 0.5f;
        head.setBounds(headOffsetX, headY, headW, headH);
        head.toFront();
    }

    private float barY() {
        return (getHeight() - BAR_H) * 0.5f;
    }

    public ProgressBar getBar() {
        return bar;
    }

    public Image getHead() {
        return head;
    }

    @Override
    public float getPrefWidth() {
        return getWidth() > 0f ? getWidth() : BAR_W;
    }

    @Override
    public float getPrefHeight() {
        return Math.max(BAR_H, HEAD_H);
    }
}
