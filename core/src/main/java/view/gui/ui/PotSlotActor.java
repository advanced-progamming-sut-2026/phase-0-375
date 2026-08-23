package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;
import model.enums.PotState;
import model.greenhouse.Pot;
import pvz.libpvz.textures.TextureBank;
import view.gui.anim.PamClipCache;
import view.gui.assets.PvzAssets;
import view.gui.assets.ZenGardenArt;
import view.gui.assets.ZenGardenLayout;

/**
 * Terracotta pot glued to a painted Zen Garden platform, with PAM plant idle.
 */
public final class PotSlotActor extends Table {
    public static final float POT_W = 140f;
    public static final float POT_H = 120f;
    public static final float ACTION_W = 108f;
    public static final float ACTION_H = 32f;

    public interface Listener {
        void onPrimary(int x, int y, PotState state);
    }

    private final int potX;
    private final int potY;
    private final TextureBank textures;
    private final Listener listener;

    private final Image potImage;
    private final Image overlayImage;
    private final PotPlantView plantView;
    private final Label statusLabel;
    private final Label timerLabel;
    private final TextButton actionButton;
    private PotState lastState;

    public PotSlotActor(int potX, int potY, Skin skin, PvzAssets assets, PamClipCache clips,
                        Listener listener) {
        this.potX = potX;
        this.potY = potY;
        this.textures = assets.textures;
        this.listener = listener;

        setTransform(false);
        pad(0f);

        TextureRegion potReg = textures.region(ZenGardenArt.SLOT);
        potImage = potReg != null
                ? new Image(new TextureRegionDrawable(potReg))
                : new Image();
        potImage.setTouchable(Touchable.disabled);

        overlayImage = new Image();
        overlayImage.setVisible(false);
        overlayImage.setTouchable(Touchable.disabled);

        plantView = new PotPlantView(assets, clips);

        Stack potStack = new Stack();
        potStack.add(potImage);
        Table overlayWrap = new Table();
        overlayWrap.add(overlayImage).expand().center().padBottom(18f);
        potStack.add(overlayWrap);
        potStack.add(plantView);

        statusLabel = new Label("", skin, "secondary");
        statusLabel.setColor(Color.WHITE);
        statusLabel.setFontScale(0.72f);
        statusLabel.setAlignment(Align.center);

        timerLabel = new Label("", skin, "medium");
        timerLabel.setColor(Color.WHITE);
        timerLabel.setFontScale(0.68f);
        timerLabel.setAlignment(Align.center);

        actionButton = new TextButton("…", skin, "brown");
        actionButton.getLabel().setFontScale(0.68f);
        actionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                firePrimary();
            }
        });

        add(potStack).size(POT_W, POT_H).row();
        add(statusLabel).width(POT_W + 20f).padTop(2f).row();
        add(timerLabel).width(POT_W + 20f).row();
        add(actionButton).width(ACTION_W).height(ACTION_H).padTop(2f);

        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.getTarget().isDescendantOf(actionButton)) {
                    return;
                }
                firePrimary();
            }
        });
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        // Swap sprout → PAM once the idle clip finishes loading.
        if (lastState == PotState.GROWING && plantView.isClipReady() && overlayImage.isVisible()) {
            clearOverlay();
        }
    }

    private void firePrimary() {
        if (listener != null && lastState != null) {
            listener.onPrimary(potX, potY, lastState);
        }
    }

    public void placeOnPlatform(ZenGardenLayout.CoverTransform t, int colIndex, int rowIndex) {
        float cx = ZenGardenLayout.SLOT_X[colIndex];
        float cy = ZenGardenLayout.SLOT_Y[rowIndex];
        float screenCx = t.screenX(cx);
        float screenCy = t.screenY(cy);

        pack();
        float w = Math.max(getPrefWidth(), POT_W);
        float h = getPrefHeight();
        setSize(w, h);
        float potBottomFromActorBottom = h - POT_H;
        setPosition(screenCx - w * 0.5f, screenCy - potBottomFromActorBottom - POT_H * 0.18f);
    }

    public void refresh(Pot pot) {
        if (pot == null) {
            return;
        }
        pot.isReady();
        PotState state = pot.getState();
        lastState = state;

        switch (state) {
            case LOCKED -> {
                potImage.setColor(0.45f, 0.45f, 0.5f, 0.85f);
                plantView.clearPlant();
                setOverlay(ZenGardenArt.LOCKED_ICON, 36f, 48f);
                statusLabel.setText("Locked");
                timerLabel.setText("");
                actionButton.setText("Shop");
                actionButton.setVisible(true);
            }
            case EMPTY -> {
                potImage.setColor(Color.WHITE);
                plantView.clearPlant();
                clearOverlay();
                statusLabel.setText("");
                timerLabel.setText("");
                actionButton.setText("Plant");
                actionButton.setVisible(true);
            }
            case GROWING -> {
                potImage.setColor(Color.WHITE);
                String plant = pot.getPlantType();
                plantView.setPlant(plant, false);
                if (plantView.isClipReady()) {
                    clearOverlay();
                } else {
                    setOverlay(ZenGardenArt.SPROUT, 40f, 26f);
                }
                statusLabel.setText(plant == null ? "Plant" : plant);
                timerLabel.setText(formatRemaining(pot.getRemainingGrowthHours()));
                actionButton.setText("Grow " + pot.accelerationCost() + "g");
                actionButton.setVisible(true);
            }
            case READY -> {
                potImage.setColor(Color.WHITE);
                String plant = pot.getPlantType();
                plantView.setPlant(plant, true);
                setOverlay(ZenGardenArt.HIGHLIGHT, 86f, 86f);
                statusLabel.setText(plant == null ? "Ready" : plant);
                timerLabel.setText("Ready!");
                actionButton.setText("Collect");
                actionButton.setVisible(true);
            }
        }
    }

    private void setOverlay(String regionId, float w, float h) {
        TextureRegion reg = textures.region(regionId);
        if (reg == null) {
            overlayImage.setVisible(false);
            return;
        }
        overlayImage.setDrawable(new TextureRegionDrawable(reg));
        overlayImage.setVisible(true);
        overlayImage.setSize(w, h);
    }

    private void clearOverlay() {
        overlayImage.setVisible(false);
        overlayImage.setDrawable(null);
    }

    private static String formatRemaining(float hours) {
        if (hours <= 0f) {
            return "Soon";
        }
        int totalMin = Math.max(1, Math.round(hours * 60f));
        int h = totalMin / 60;
        int m = totalMin % 60;
        if (h > 0) {
            return h + "h " + m + "m";
        }
        return m + "m";
    }
}
