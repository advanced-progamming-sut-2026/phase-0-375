package view.gui.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import pvz.libpvz.textures.TextureBank;

/**
 * Top-right in-game coin bar ({@code IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL}).
 */
public final class CoinHud extends WidgetGroup {
    public static final String BAR_ID = "IMAGE_UI_GENERIC_BUTTONS_COIN_BUY_NORMAL";

    /** Native 768 atlas size. */
    public static final float BAR_W = 181f;
    public static final float BAR_H = 57f;
    /** Coin-icon centre (fly target). */
    private static final float LOGO_X_FRAC = 0.19f;
    private static final float LOGO_Y_FRAC = 0.50f;
    /** Amount text sits on the black bar. */
    private static final float AMOUNT_W_FRAC = 0.42f;
    private static final float AMOUNT_X_FRAC = 0.38f;
    private static final float AMOUNT_Y_FRAC = 0.46f;

    private final Label amount;

    public CoinHud(Skin skin, TextureBank textures) {
        setSize(BAR_W, BAR_H);
        setTouchable(Touchable.disabled);

        Image bar = new Image();
        TextureRegion region = textures == null ? null : textures.region(BAR_ID);
        if (region != null) {
            bar.setDrawable(new TextureRegionDrawable(region));
        }
        bar.setBounds(0f, 0f, BAR_W, BAR_H);
        bar.setTouchable(Touchable.disabled);
        addActor(bar);

        amount = new Label("0", skin, "medium");
        amount.setAlignment(Align.center);
        float aw = BAR_W * AMOUNT_W_FRAC;
        float ah = BAR_H * 0.55f;
        amount.setBounds(
            BAR_W * AMOUNT_X_FRAC,
            BAR_H * AMOUNT_Y_FRAC - ah * 0.5f,
            aw, ah);
        amount.setTouchable(Touchable.disabled);
        addActor(amount);
    }

    public void setAmount(int coins) {
        amount.setText(String.valueOf(Math.max(0, coins)));
    }

    /** Stage-space centre of the coin icon (collect-fly target). */
    public Vector2 logoCenter(Vector2 out) {
        out.set(BAR_W * LOGO_X_FRAC, BAR_H * LOGO_Y_FRAC);
        localToStageCoordinates(out);
        return out;
    }

    @Override
    public float getPrefWidth() {
        return BAR_W;
    }

    @Override
    public float getPrefHeight() {
        return BAR_H;
    }
}
