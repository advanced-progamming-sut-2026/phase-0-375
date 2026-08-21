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
 * Top-right in-game coin bar: {@code IMAGE_UI_GENERIC_BUTTON_GENERIC_LTECURRENCY}
 * with {@code IMAGE_UI_HUD_INGAME_COIN} on the left.
 */
public final class CoinHud extends WidgetGroup {
    public static final String BAR_ID = "IMAGE_UI_GENERIC_BUTTON_GENERIC_LTECURRENCY";
    public static final String COIN_ID = "IMAGE_UI_HUD_INGAME_COIN";

    /** Native 768 atlas sizes. */
    public static final float COIN_W = 61f;
    public static final float COIN_H = 59f;
    public static final float BAR_W = 158f;
    public static final float BAR_H = 59f;
    /** Bar starts under the coin so the left cap is hidden. */
    public static final float BAR_LEFT = 24f;
    public static final float WIDTH = BAR_LEFT + BAR_W;
    public static final float HEIGHT = COIN_H;

    private final Label amount;

    public CoinHud(Skin skin, TextureBank textures) {
        setSize(WIDTH, HEIGHT);
        setTouchable(Touchable.disabled);

        Image bar = new Image();
        TextureRegion barRegion = textures == null ? null : textures.region(BAR_ID);
        if (barRegion != null) {
            bar.setDrawable(new TextureRegionDrawable(barRegion));
        }
        bar.setBounds(BAR_LEFT, (HEIGHT - BAR_H) * 0.5f, BAR_W, BAR_H);
        bar.setTouchable(Touchable.disabled);
        addActor(bar);

        Image coin = new Image();
        TextureRegion coinRegion = textures == null ? null : textures.region(COIN_ID);
        if (coinRegion != null) {
            coin.setDrawable(new TextureRegionDrawable(coinRegion));
        }
        coin.setBounds(0f, (HEIGHT - COIN_H) * 0.5f, COIN_W, COIN_H);
        coin.setTouchable(Touchable.disabled);
        addActor(coin);

        amount = new Label("0", skin, "medium");
        amount.setAlignment(Align.center);
        float textX = COIN_W;
        float textW = WIDTH - COIN_W;
        float textH = BAR_H * 0.55f;
        amount.setBounds(textX, (HEIGHT - textH) * 0.5f, textW, textH);
        amount.setTouchable(Touchable.disabled);
        addActor(amount);
    }

    public void setAmount(int coins) {
        amount.setText(String.valueOf(Math.max(0, coins)));
    }

    /** Stage-space centre of the coin icon (collect-fly target). */
    public Vector2 logoCenter(Vector2 out) {
        out.set(COIN_W * 0.5f, HEIGHT * 0.5f);
        localToStageCoordinates(out);
        return out;
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
