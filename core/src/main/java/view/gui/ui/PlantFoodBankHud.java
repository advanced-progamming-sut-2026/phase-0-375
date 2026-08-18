package view.gui.ui;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import pvz.libpvz.textures.TextureBank;

/**
 * In-game plant-food bank HUD.
 */
public final class PlantFoodBankHud extends WidgetGroup {
    public static final String BANK_ID = "IMAGE_UI_HUD_INGAME_PLANTFOOD_BANK";
    public static final String FILLED_SLOT_ID = "IMAGE_UI_HUD_INGAME_PLANTFOOD_BANK_FILLED_SLOT";
    public static final String CURSOR_ID = "IMAGE_UI_HUD_INGAME_PLANTFOOD_BUTTON";
    public static final String ATLAS_GROUP = "UI_AlwaysLoaded_768";
    public static final String ATLAS_PAGE_0 = "ATLASIMAGE_ATLAS_UI_ALWAYSLOADED_768_00";
    public static final String ATLAS_PAGE_1 = "ATLASIMAGE_ATLAS_UI_ALWAYSLOADED_768_01";

    public static final int SLOT_COUNT = 5;

    public static final float BANK_H = 88f;

    /**
     * Native 768 atlas pixels for {@code IMAGE_UI_HUD_INGAME_PLANTFOOD_BANK}
     * (206×88). Slot centres measured from the five dark wells; Y is Scene2D
     * (bottom-origin). Scale with {@code BANK_H / NATIVE_H}.
     */
    private static final float NATIVE_W = 206f;
    private static final float NATIVE_H = 88f;
    private static final float LOGO_CX = 43f;
    private static final float LOGO_CY = 44f;
    private static final float LOGO_W = 53f;
    private static final float LOGO_H = 56f;
    private static final float SLOT_CX0 = 87f;
    private static final float SLOT_CY = 43f;
    private static final float SLOT_STEP = 24f;
    private static final float SLOT_SIZE = 25f;

    private final TextureBank textures;
    private final Image bankImage;
    private final ImageButton plantfoodButton;
    private final Image[] filledSlots = new Image[SLOT_COUNT];

    private float bankW = BANK_H * 2f;
    private boolean layoutDirty = true;

    public PlantFoodBankHud(Skin skin, TextureBank textures) {
        this.textures = textures;
        setSize(bankW, BANK_H);
        setTouchable(Touchable.childrenOnly);

        bankImage = new Image();
        bankImage.setTouchable(Touchable.disabled);
        addActor(bankImage);

        plantfoodButton = buildPlantfoodButton(skin);
        addActor(plantfoodButton);

        for (int i = 0; i < SLOT_COUNT; i++) {
            Image slot = new Image();
            slot.setTouchable(Touchable.disabled);
            slot.setVisible(false);
            addActor(slot);
            filledSlots[i] = slot;
        }
    }

    /** Show the first {@code count} slot overlays (clamped to {@link #SLOT_COUNT}). */
    public void setCount(int count) {
        int n = Math.max(0, Math.min(SLOT_COUNT, count));
        for (int i = 0; i < SLOT_COUNT; i++) {
            filledSlots[i].setVisible(i < n);
        }
    }

    /** Wire the plant-food button click. Fires on every press (toggle is the caller's job). */
    public void onPlantFoodButton(Runnable handler) {
        if (handler == null) {
            return;
        }
        plantfoodButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handler.run();
            }
        });
    }

    /** Visually mark the button as the active source of the armed cursor. */
    public void setButtonChecked(boolean checked) {
        plantfoodButton.setChecked(checked);
    }

    /** Stage-space centre of the plant-food logo (collect-fly target). */
    public Vector2 logoCenter(Vector2 out) {
        float s = BANK_H / NATIVE_H;
        out.set(LOGO_CX * s, LOGO_CY * s);
        localToStageCoordinates(out);
        return out;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        bindBankImage();
        bindFilledSlots();
        if (layoutDirty) {
            relayout();
        }
    }

    @Override
    public float getPrefWidth() {
        return bankW;
    }

    @Override
    public float getPrefHeight() {
        return BANK_H;
    }

    private void bindBankImage() {
        if (bankImage.getDrawable() != null) {
            return;
        }
        TextureRegion region = textures == null ? null : textures.region(BANK_ID);
        if (region == null) {
            return;
        }
        bankImage.setDrawable(new TextureRegionDrawable(region));
        float nativeW = region.getRegionWidth();
        float nativeH = region.getRegionHeight();
        if (nativeH > 0f) {
            bankW = BANK_H * (nativeW / nativeH);
            setSize(bankW, BANK_H);
            invalidateHierarchy();
        } else {
            bankW = BANK_H * (NATIVE_W / NATIVE_H);
            setSize(bankW, BANK_H);
        }
        layoutDirty = true;
    }

    private void bindFilledSlots() {
        TextureRegion region = textures == null ? null : textures.region(FILLED_SLOT_ID);
        if (region == null) {
            return;
        }
        boolean bound = false;
        for (Image slot : filledSlots) {
            if (slot.getDrawable() == null) {
                slot.setDrawable(new TextureRegionDrawable(region));
                bound = true;
            }
        }
        if (bound) {
            layoutDirty = true;
        }
    }

    /** Recompute the bank image, button and slot overlay rectangles. */
    private void relayout() {
        bankImage.setBounds(0f, 0f, bankW, BANK_H);

        float s = BANK_H / NATIVE_H;
        plantfoodButton.setSize(LOGO_W * s, LOGO_H * s);
        plantfoodButton.setPosition(
            LOGO_CX * s - LOGO_W * s * 0.5f,
            LOGO_CY * s - LOGO_H * s * 0.5f);

        float slotSize = SLOT_SIZE * s;
        float half = slotSize * 0.5f;
        float cy = SLOT_CY * s;
        for (int i = 0; i < SLOT_COUNT; i++) {
            float cx = (SLOT_CX0 + i * SLOT_STEP) * s;
            Image slot = filledSlots[i];
            slot.setSize(slotSize, slotSize);
            slot.setPosition(cx - half, cy - half);
            slot.toFront();
        }
        plantfoodButton.toFront();
        layoutDirty = false;
    }

    private static ImageButton buildPlantfoodButton(Skin skin) {
        ImageButton button;
        if (skin != null && skin.has("plantfood", ImageButton.ImageButtonStyle.class)) {
            button = new ImageButton(skin, "plantfood");
        } else {
            ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
            if (skin != null) {
                Drawable up = UiDrawables.tryNamed(skin, "image_ui_hud_ingame_plantfood_button");
                if (up == null) {
                    up = UiDrawables.tryNamed(skin, "plantfood");
                }
                Drawable down = UiDrawables.tryNamed(skin, "image_ui_hud_ingame_plantfood_button_down");
                style.imageUp = up;
                style.imageDown = down;
            }
            button = new ImageButton(style);
        }
        // Tile-click calls setChecked; that must not re-fire ChangeListener.
        button.setProgrammaticChangeEvents(false);
        return button;
    }
}
