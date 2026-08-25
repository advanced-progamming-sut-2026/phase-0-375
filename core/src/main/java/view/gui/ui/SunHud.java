package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.ray3k.tenpatch.TenPatchDrawable;
import model.game.core.GameModel;
import model.game.level.Level;
import model.game.level.minigame.bowling.WallnutBowlingLevel;
import model.game.level.minigame.vasebreaker.VaseBreakerLevel;
import model.game.level.special.ConveyorBeltLevel;

/**
 * In-game sun bank: PvzSkin {@code image_ui_hud_ingame_sun_down} over a 3-slice bar.
 */
public final class SunHud extends WidgetGroup {
    /** PvzSkin atlas names (not TextureBank {@code IMAGE_*} ids). */
    public static final String SUN = "image_ui_hud_ingame_sun_down";
    public static final String BAR = "image_ui_hud_ingame_background_3slice";

    /** Native 768 sizes from the skin atlas. */
    public static final float SUN_W = 70f;
    public static final float SUN_H = 71f;
    public static final float BAR_H = 42f;
    /** Bar starts under the sun so the left cap is hidden. */
    public static final float BAR_LEFT = 24f;
    public static final float WIDTH = 148f;
    public static final float HEIGHT = SUN_H;

    private final Label amount;

    public SunHud(Skin skin) {
        setSize(WIDTH, HEIGHT);
        setTouchable(Touchable.disabled);

        Rectangle sun = new Rectangle();
        Rectangle bar = new Rectangle();
        Rectangle text = new Rectangle();
        layoutRects(WIDTH, HEIGHT, sun, bar, text);

        Drawable barDraw = barDrawable(skin);
        Image barImage = barDraw == null ? new Image() : new Image(barDraw);
        barImage.setBounds(bar.x, bar.y, bar.width, bar.height);
        barImage.setTouchable(Touchable.disabled);
        addActor(barImage);

        Drawable sunDraw = UiDrawables.tryNamed(skin, SUN);
        Image sunImage = sunDraw == null ? new Image() : new Image(sunDraw);
        sunImage.setBounds(sun.x, sun.y, sun.width, sun.height);
        sunImage.setTouchable(Touchable.disabled);
        addActor(sunImage);

        BitmapFont font = SkinFonts.outlined(skin, "medium");
        Label.LabelStyle style = new Label.LabelStyle(font, Color.WHITE);
        amount = new Label("0", style);
        amount.setAlignment(Align.center);
        amount.setBounds(text.x, text.y, text.width, text.height);
        amount.setTouchable(Touchable.disabled);
        addActor(amount);
    }

    public void setAmount(int suns) {
        amount.setText(String.valueOf(Math.max(0, suns)));
    }

    /** Stage-space centre of the sun logo (collect-fly target). */
    public Vector2 logoCenter(Vector2 out) {
        out.set(SUN_W * 0.5f, HEIGHT * 0.5f);
        localToStageCoordinates(out);
        return out;
    }

    /** Conveyor / bowling / vase breaker do not spend sun; hide the bank. */
    public static boolean showFor(GameModel model) {
        if (model == null) {
            return false;
        }
        Level level = model.getCurrentLevel();
        return !(level instanceof ConveyorBeltLevel)
                && !(level instanceof WallnutBowlingLevel)
                && !(level instanceof VaseBreakerLevel);
    }

    /**
     * Sun on the left (taller than the bar). Count is centred in the bar
     * to the right of the logo.
     */
    static void layoutRects(float width, float height, Rectangle sun, Rectangle bar, Rectangle text) {
        sun.set(0f, (height - SUN_H) * 0.5f, SUN_W, SUN_H);
        bar.set(BAR_LEFT, (height - BAR_H) * 0.5f, width - BAR_LEFT, BAR_H);
        text.set(SUN_W, bar.y, width - SUN_W, BAR_H);
    }

    private static Drawable barDrawable(Skin skin) {
        Drawable ten = UiDrawables.tenPatch(skin, BAR);
        if (ten != null) {
            return ten;
        }
        TextureRegion region = region(skin, BAR);
        if (region != null) {
            return stretch3(region);
        }
        return UiDrawables.tryNamed(skin, BAR);
    }

    private static TextureRegion region(Skin skin, String name) {
        Drawable drawable = UiDrawables.tryDrawable(skin, name);
        if (drawable instanceof TextureRegionDrawable trd) {
            return trd.getRegion();
        }
        if (skin == null) {
            return null;
        }
        try {
            return skin.getRegion(name);
        } catch (Exception e) {
            return null;
        }
    }

    /** Horizontal 3-slice; do not scale the raw 84×42 region. */
    static Drawable stretch3(TextureRegion region) {
        int w = region.getRegionWidth();
        int h = region.getRegionHeight();
        int cap = Math.max(8, w / 4);
        TenPatchDrawable drawable = new TenPatchDrawable(
                new int[]{cap, Math.max(cap, w - cap - 1)},
                new int[]{0, Math.max(0, h - 1)},
                false,
                region);
        drawable.setMinWidth(0f);
        drawable.setMinHeight(0f);
        return drawable;
    }

    @Override
    public float getPrefWidth() {
        return WIDTH;
    }

    @Override
    public float getPrefHeight() {
        return HEIGHT;
    }
}
