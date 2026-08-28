package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import model.enums.LevelType;
import model.game.core.GameModel;
import model.game.level.Level;
import model.game.level.special.LoveYourPlantsLevel;
import pvz.libpvz.textures.TextureBank;

/**
 * In-game counter for Love Your Plants levels:
 * {@code IMAGE_UI_HUD_WORLDMAP_LEVEL_COUNTER} badge with
 * {@code IMAGE_UI_HUD_INGAME_CHALLENGE_PLANT_LOST_ICON} on the left
 * and remaining plant deaths count formatted as "# Left".
 */
public final class LoveYourPlantsHud extends WidgetGroup {
    public static final String COUNTER_ID = "IMAGE_UI_HUD_WORLDMAP_LEVEL_COUNTER";
    public static final String ICON_ID = "IMAGE_UI_HUD_INGAME_CHALLENGE_PLANT_LOST_ICON";

    public static final float COUNTER_W = 98f;
    public static final float COUNTER_H = 43f;
    public static final float ICON_W = 29f;
    public static final float ICON_H = 36f;
    public static final float WIDTH = COUNTER_W;
    public static final float HEIGHT = COUNTER_H;

    private final Label amount;

    public LoveYourPlantsHud(Skin skin, TextureBank textures) {
        setSize(WIDTH, HEIGHT);
        setTouchable(Touchable.disabled);

        Image bg = new Image();
        TextureRegion bgRegion = textures == null ? null : textures.region(COUNTER_ID);
        if (bgRegion != null) {
            bg.setDrawable(new TextureRegionDrawable(bgRegion));
        }
        bg.setBounds(0f, 0f, COUNTER_W, COUNTER_H);
        bg.setTouchable(Touchable.disabled);
        addActor(bg);

        Image icon = new Image();
        TextureRegion iconRegion = textures == null ? null : textures.region(ICON_ID);
        if (iconRegion != null) {
            icon.setDrawable(new TextureRegionDrawable(iconRegion));
        }
        float iconX = 6f;
        float iconY = (COUNTER_H - ICON_H) * 0.5f;
        icon.setBounds(iconX, iconY, ICON_W, ICON_H);
        icon.setTouchable(Touchable.disabled);
        addActor(icon);

        BitmapFont font = SkinFonts.get("medium_outline", 18);
        if (font == null) {
            font = SkinFonts.outlined(skin, "medium");
        }
        Label.LabelStyle style = font != null
                ? new Label.LabelStyle(font, Color.WHITE)
                : (skin != null && skin.has("medium", Label.LabelStyle.class)
                    ? skin.get("medium", Label.LabelStyle.class)
                    : new Label.LabelStyle(new BitmapFont(), Color.WHITE));
        amount = new Label("0 Left", style);
        amount.setAlignment(Align.center);
        float textX = iconX + ICON_W;
        float textW = COUNTER_W - textX - 4f;
        amount.setBounds(textX, 0f, textW, COUNTER_H);
        amount.setTouchable(Touchable.disabled);
        addActor(amount);
    }

    public void setCount(int remaining) {
        amount.setText(Math.max(0, remaining) + " Left");
    }

    public void sync(GameModel model) {
        if (!showFor(model)) {
            setVisible(false);
            return;
        }
        setVisible(true);
        int maxDeaths = model.getCurrentLevel().getConfig().getRules().getMaxPlantDeaths();
        int lost = model.getPlantsLost();
        int remaining = Math.max(0, maxDeaths - lost);
        setCount(remaining);
    }

    public static boolean showFor(GameModel model) {
        if (model == null || model.getCurrentLevel() == null) {
            return false;
        }
        Level level = model.getCurrentLevel();
        if (level instanceof LoveYourPlantsLevel) {
            return level.getConfig() != null
                    && level.getConfig().getRules() != null
                    && level.getConfig().getRules().getMaxPlantDeaths() >= 0;
        }
        return level.getConfig() != null
                && level.getConfig().getLevelType() == LevelType.LOVE_YOUR_PLANTS
                && level.getConfig().getRules() != null
                && level.getConfig().getRules().getMaxPlantDeaths() >= 0;
    }

    public Label getAmountLabel() {
        return amount;
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
