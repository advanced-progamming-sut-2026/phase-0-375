package view.gui.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
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
import model.app.App;
import model.enums.PotState;
import model.greenhouse.Pot;
import model.user.User;
import pvz.libpvz.textures.TextureBank;
import view.gui.anim.PamClipCache;
import view.gui.assets.PvzAssets;
import view.gui.assets.UiRegions;
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

    /** 768 atlas crop of {@link ZenGardenArt#EMPTY_SLOT}. */
    private static final float EMPTY_SLOT_W = 78f;
    private static final float EMPTY_SLOT_H = 103f;
    /** 768 native {@link ZenGardenArt#TIMER_BG}. */
    private static final float TIMER_W = 100f;
    private static final float TIMER_H = 36f;
    /** Instant-grow gem chrome (768 native 165×74). */
    private static final float UNLOCK_W = 125f;
    private static final float UNLOCK_H = 60f;
    /** Purple buy-pot button on the next locked slot. */
    private static final float BUY_POT_W = 128f;
    private static final float BUY_POT_H = 50f;
    private static final float BUY_COIN_ICON = 30f;
    private static final float TIMER_FONT_SCALE = 0.9f;
    private static final float GROW_FONT_SCALE = 1.4f;
    private static final float UNLOCK_PRICE_FONT_SCALE = 1.2f;
    /** Same as {@link model.shop.Shop} pot item — 2000 coins, not gems. */
    private static final int POT_PRICE = 2000;
    /** Lift the timer + grow row. Negative = higher. */
    private static final float META_PAD_TOP = -18f;
    private static final float META_GAP = -9f;

    public interface Listener {
        void onPrimary(int x, int y, PotState state);
    }

    private final int potX;
    private final int potY;
    private final TextureBank textures;
    private final Listener listener;

    private Image potImage;
    private Image overlayImage;
    private Image emptySlotImage;
    private Table emptyWrap;
    private TextButton unlockButton;
    private Table unlockWrap;
    private Label unlockPriceLabel;
    private PotPlantView plantView;
    private Table metaRow;
    private Stack timerStack;
    private Image timerBg;
    private Label timerLabel;
    private Stack growStack;
    private Image growChrome;
    private Label growCostLabel;
    private TextButton collectButton;
    private PotState lastState;

    public PotSlotActor(int potX, int potY, Skin skin, PvzAssets assets, PamClipCache clips,
                        Listener listener) {
        this.potX = potX;
        this.potY = potY;
        this.textures = assets.textures;
        this.listener = listener;
        setTransform(false);
        pad(0f);
        setTouchable(Touchable.childrenOnly);
        initPotImages();
        initUnlock(skin);
        plantView = new PotPlantView(assets, clips);
        initTimer(skin);
        initGrow(skin);
        initCollect(skin);
        layoutSlot(assemblePotStack());
        addListener(new SlotClick());
    }

    private void initPotImages() {
        TextureRegion potReg = textures.region(ZenGardenArt.SLOT);
        potImage = potReg != null
            ? new Image(new TextureRegionDrawable(potReg))
            : new Image();
        potImage.setTouchable(Touchable.disabled);
        overlayImage = new Image();
        overlayImage.setVisible(false);
        overlayImage.setTouchable(Touchable.disabled);
        emptySlotImage = regionImage(ZenGardenArt.EMPTY_SLOT);
        emptySlotImage.setVisible(false);
        emptySlotImage.setTouchable(Touchable.enabled);
        emptySlotImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
                firePlant();
            }
        });
    }

    private void initUnlock(Skin skin) {
        unlockButton = new TextButton("", skin, "purple");
        unlockButton.setVisible(false);
        unlockButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                fireUnlock();
            }
        });
        unlockPriceLabel = new Label(String.valueOf(POT_PRICE),
                new Label.LabelStyle(
                        SkinFonts.outlined(SkinFonts.getScaled(skin, "medium", UNLOCK_PRICE_FONT_SCALE)),
                        Color.WHITE));
        unlockPriceLabel.setAlignment(Align.left);
        unlockPriceLabel.setTouchable(Touchable.disabled);
        unlockPriceLabel.setVisible(false);
    }

    private Stack assemblePotStack() {
        Stack potStack = new Stack();
        potStack.setTouchable(Touchable.childrenOnly);
        potStack.add(potImage);
        emptyWrap = new Table();
        emptyWrap.setTouchable(Touchable.childrenOnly);
        emptyWrap.add(emptySlotImage).size(EMPTY_SLOT_W, EMPTY_SLOT_H).expand().center().padBottom(10f);
        potStack.add(emptyWrap);
        Table overlayWrap = new Table();
        overlayWrap.setTouchable(Touchable.childrenOnly);
        overlayWrap.add(overlayImage).expand().center().padBottom(18f);
        potStack.add(overlayWrap);
        unlockWrap = buildUnlockWrap();
        potStack.add(unlockWrap);
        potStack.add(plantView);
        return potStack;
    }

    private Table buildUnlockWrap() {
        Table wrap = new Table();
        wrap.setTouchable(Touchable.childrenOnly);
        wrap.setVisible(false);
        Stack unlockStack = new Stack();
        unlockStack.setTouchable(Touchable.childrenOnly);
        unlockStack.add(unlockButton);
        Table unlockPriceWrap = new Table();
        unlockPriceWrap.setTouchable(Touchable.disabled);
        Image coinIcon = regionImage(UiRegions.COIN_ICON);
        unlockPriceWrap.add(coinIcon).size(BUY_COIN_ICON, BUY_COIN_ICON).padRight(4f);
        unlockPriceWrap.add(unlockPriceLabel);
        unlockStack.add(unlockPriceWrap);
        wrap.add(unlockStack).size(BUY_POT_W, BUY_POT_H).expand().center();
        return wrap;
    }

    private void initTimer(Skin skin) {
        timerBg = regionImage(ZenGardenArt.TIMER_BG);
        timerLabel = new Label("", new Label.LabelStyle(
                SkinFonts.outlined(SkinFonts.getScaled(skin, "medium", TIMER_FONT_SCALE)), Color.WHITE));
        timerLabel.setAlignment(Align.center);
        timerStack = new Stack();
        timerStack.add(timerBg);
        Table timerText = new Table();
        timerText.setTouchable(Touchable.disabled);
        timerText.add(timerLabel).expand().center();
        timerStack.add(timerText);
        timerStack.setVisible(false);
    }

    private void initGrow(Skin skin) {
        growChrome = regionImage(ZenGardenArt.UNLOCK_ACTIVE);
        growCostLabel = new Label("", new Label.LabelStyle(
                SkinFonts.outlined(SkinFonts.getScaled(skin, "medium", GROW_FONT_SCALE)), Color.WHITE));
        growCostLabel.setAlignment(Align.center);
        growCostLabel.setTouchable(Touchable.disabled);
        growStack = new Stack();
        growStack.add(growChrome);
        Table growText = new Table();
        growText.setTouchable(Touchable.disabled);
        growText.add(growCostLabel).expand().center().padLeft(18f).padBottom(5f);
        growStack.add(growText);
        growStack.setVisible(false);
        growStack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
                fireGrow();
            }
        });
    }

    private void initCollect(Skin skin) {
        collectButton = new TextButton("Collect", skin, "brown");
        SkinFonts.scaleButton(collectButton, skin, "brown", 0.68f);
        collectButton.setVisible(false);
        collectButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                fireCollect();
            }
        });
        metaRow = new Table();
        metaRow.setTouchable(Touchable.childrenOnly);
        metaRow.defaults().center();
        metaRow.add(timerStack).size(TIMER_W, TIMER_H).padRight(META_GAP);
        metaRow.add(growStack).size(UNLOCK_W, UNLOCK_H);
    }

    private void layoutSlot(Stack potStack) {
        add(potStack).size(POT_W, POT_H).row();
        add(metaRow).padTop(META_PAD_TOP).row();
        add(collectButton).width(ACTION_W).height(ACTION_H).padTop(2f);
    }

    private final class SlotClick extends ClickListener {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            if (event.getTarget().isDescendantOf(growStack)
                || event.getTarget().isDescendantOf(collectButton)
                || event.getTarget().isDescendantOf(unlockButton)
                || event.getTarget().isDescendantOf(emptySlotImage)) {
                return;
            }
            if (lastState == PotState.EMPTY) {
                firePlant();
            } else if (lastState == PotState.READY) {
                fireCollect();
            } else if (lastState == PotState.LOCKED) {
                fireUnlock();
            }
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        // Swap sprout → PAM once the idle clip finishes loading.
        if (lastState == PotState.GROWING && plantView.isClipReady() && overlayImage.isVisible()) {
            clearOverlay();
        }
    }

    private void firePlant() {
        if (listener != null && lastState == PotState.EMPTY) {
            listener.onPrimary(potX, potY, PotState.EMPTY);
        }
    }

    private void fireGrow() {
        if (listener != null && lastState == PotState.GROWING) {
            listener.onPrimary(potX, potY, PotState.GROWING);
        }
    }

    private void fireCollect() {
        if (listener != null && lastState == PotState.READY) {
            listener.onPrimary(potX, potY, PotState.READY);
        }
    }

    private void fireUnlock() {
        if (listener != null && lastState == PotState.LOCKED) {
            listener.onPrimary(potX, potY, PotState.LOCKED);
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
        refresh(pot, false);
    }

    /**
     * @param purchasable next locked pot that can be bought — shows the
     *                    unlock button instead of the padlock.
     */
    public void refresh(Pot pot, boolean purchasable) {
        if (pot == null) {
            return;
        }
        pot.isReady();
        PotState state = pot.getState();
        lastState = state;
        switch (state) {
            case LOCKED -> refreshLocked(purchasable);
            case EMPTY -> refreshEmpty();
            case GROWING -> refreshGrowing(pot);
            case READY -> refreshReady(pot);
        }
        refreshMetaRow();
    }

    private void refreshLocked(boolean purchasable) {
        potImage.setColor(0.45f, 0.45f, 0.5f, 0.85f);
        plantView.clearPlant();
        emptySlotImage.setVisible(false);
        emptySlotImage.setTouchable(Touchable.disabled);
        if (purchasable) {
            clearOverlay();
            setUnlockVisible(true);
        } else {
            setUnlockVisible(false);
            setOverlay(ZenGardenArt.LOCKED_ICON, 36f, 48f);
        }
        hideTimer();
        hideGrow();
        collectButton.setVisible(false);
    }

    private void refreshEmpty() {
        potImage.setColor(Color.WHITE);
        plantView.clearPlant();
        clearOverlay();
        setUnlockVisible(false);
        emptySlotImage.setVisible(true);
        emptySlotImage.setTouchable(Touchable.enabled);
        hideTimer();
        hideGrow();
        collectButton.setVisible(false);
    }

    private void refreshGrowing(Pot pot) {
        potImage.setColor(Color.WHITE);
        emptySlotImage.setVisible(false);
        emptySlotImage.setTouchable(Touchable.disabled);
        setUnlockVisible(false);
        String plant = pot.getPlantType();
        plantView.setPlant(plant, false);
        if (plantView.isClipReady()) {
            clearOverlay();
        } else {
            setOverlay(ZenGardenArt.SPROUT, 40f, 26f);
        }
        showTimer(formatRemaining(pot.getRemainingGrowthHours()));
        showGrow(pot.accelerationCost());
        collectButton.setVisible(false);
    }

    private void refreshReady(Pot pot) {
        potImage.setColor(Color.WHITE);
        emptySlotImage.setVisible(false);
        emptySlotImage.setTouchable(Touchable.disabled);
        setUnlockVisible(false);
        plantView.setPlant(pot.getPlantType(), true);
        setOverlay(ZenGardenArt.HIGHLIGHT, 86f, 86f);
        showTimer("Ready!");
        hideGrow();
        collectButton.setVisible(true);
    }

    private void showTimer(String text) {
        timerLabel.setText(text);
        timerStack.setVisible(true);
        if (timerBg.getDrawable() == null) {
            TextureRegion reg = textures.region(ZenGardenArt.TIMER_BG);
            if (reg != null) {
                timerBg.setDrawable(new TextureRegionDrawable(reg));
            }
        }
    }

    private void hideTimer() {
        timerStack.setVisible(false);
        timerLabel.setText("");
    }

    private void showGrow(int cost) {
        int gems = currentGems();
        boolean afford = gems >= cost && cost > 0;
        TextureRegion chrome = textures.region(afford ? ZenGardenArt.UNLOCK_ACTIVE : ZenGardenArt.UNLOCK_INACTIVE);
        if (chrome != null) {
            growChrome.setDrawable(new TextureRegionDrawable(chrome));
        }
        growCostLabel.setText(String.valueOf(Math.max(0, cost)));
        growStack.setVisible(true);
        growStack.setTouchable(Touchable.enabled);
    }

    private void hideGrow() {
        growStack.setVisible(false);
        growStack.setTouchable(Touchable.disabled);
    }

    private void setUnlockVisible(boolean visible) {
        unlockWrap.setVisible(visible);
        unlockWrap.setTouchable(visible ? Touchable.childrenOnly : Touchable.disabled);
        unlockButton.setVisible(visible);
        unlockButton.setChecked(false);
        unlockButton.setTouchable(visible ? Touchable.enabled : Touchable.disabled);
        unlockPriceLabel.setVisible(visible);
    }

    private void refreshMetaRow() {
        boolean showTimer = timerStack.isVisible();
        boolean showGrow = growStack.isVisible();
        timerStack.setVisible(showTimer);
        growStack.setVisible(showGrow);
        metaRow.setVisible(showTimer || showGrow);

        Cell<?> timerCell = metaRow.getCell(timerStack);
        if (timerCell != null) {
            if (showTimer) {
                timerCell.size(TIMER_W, TIMER_H).padRight(showGrow ? META_GAP : 0f);
            } else {
                timerCell.size(0f, 0f).pad(0f);
            }
        }
        Cell<?> growCell = metaRow.getCell(growStack);
        if (growCell != null) {
            if (showGrow) {
                growCell.size(UNLOCK_W, UNLOCK_H);
            } else {
                growCell.size(0f, 0f);
            }
        }
    }

    private static int currentGems() {
        User user = App.getInstance().getCurrentUser();
        return user == null ? 0 : user.getGems();
    }

    private Image regionImage(String regionId) {
        TextureRegion reg = textures.region(regionId);
        Image image = reg != null ? new Image(new TextureRegionDrawable(reg)) : new Image();
        image.setTouchable(Touchable.disabled);
        return image;
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
